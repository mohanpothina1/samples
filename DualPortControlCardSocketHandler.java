import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Handler for the "few bands" that need two independent TCP connections to
 * the same card:
 *
 *   1) STATUS port (e.g. 20108) - connected FIRST. Enables the health/status
 *      telemetry stream and receives "CO.HL.G,PAOnOff,vswrAlarm,tempAlarm,
 *      Fwd,Ref" ASCII lines, expected roughly every 300ms once enabled.
 *
 *   2) CONTROL port (e.g. 7) - connected SECOND, used the same way the
 *      existing single-port ControlCardSocketHandler is used: to send the
 *      hex on/off command bytes for the module(s) on this card.
 *
 * The two connections are managed independently (separate threads, separate
 * reconnect loops) so that a drop/reconnect on one does NOT tear down the
 * other ("without loss" - i.e. losing the control connection while
 * reconnecting status, or vice versa, should not happen).
 *
 * isConnected() reflects the CONTROL port, since that's what gates whether
 * the UI should enable the on/off toggle and whether commands may be sent.
 * Health telemetry keeps flowing/reconnecting independently of that.
 *
 * To keep this a drop-in replacement for ControlCardSocketHandler in
 * ControlSystemUI, the health data is adapted into the existing
 * HardwareData shape via the dataCallback. Alarm flags don't carry a raw
 * VSWR ratio / raw temperature, so sentinel values are used purely to drive
 * the existing red/yellow/green thresholds in the UI:
 *   vswrAlarm  -> vswr = 4.0 (red) or 1.0 (green)
 *   tempAlarm  -> temp = 70   (red) or 30   (green)
 * fwd/rfl are the real reported power values (rounded to int to match the
 * existing HardwareData field types). If you'd rather show the raw alarm
 * flags/power as text instead of reusing the threshold colors, swap the
 * dataCallback type to Consumer<HealthStatusData> and adjust the UI code
 * that wires this card (see integration notes).
 */
public class DualPortControlCardSocketHandler implements Runnable {

    // ---- config ----
    private final String ip;
    private final int statusPort;   // e.g. 20108
    private final int controlPort;  // e.g. 7
    private final byte[] enableCommand; // sent once after status connect - TODO confirm exact bytes with protocol doc

    private final Consumer<Boolean> statusCallback;      // reflects CONTROL port connectivity (drives UI card indicator/toggle enable)
    private final Consumer<HardwareData> dataCallback;    // adapted health data, see class javadoc

    // ---- status (20108) connection state ----
    private volatile boolean running = true;
    private Socket statusSocket;
    private OutputStream statusOut;
    private final AtomicBoolean statusConnected = new AtomicBoolean(false);
    private Thread statusConnectionThread;
    private Thread statusReceiverThread;

    // ---- control (7) connection state ----
    private Socket controlSocket;
    private OutputStream controlOut;
    private final AtomicBoolean controlConnected = new AtomicBoolean(false);
    private Thread controlConnectionThread;
    private Thread controlSenderThread;
    private final ArrayBlockingQueue<byte[]> sendQueue = new ArrayBlockingQueue<>(100);

    private ArrayList<JCheckBox> lans; // reused for the ping/LAN indicator, same as ControlCardSocketHandler

    private volatile HealthStatusData lastHealth;

    public DualPortControlCardSocketHandler(String ip, int statusPort, int controlPort, byte[] enableCommand,
            Consumer<Boolean> statusCallback, Consumer<HardwareData> dataCallback) {
        this.ip = ip;
        this.statusPort = statusPort;
        this.controlPort = controlPort;
        this.enableCommand = enableCommand;
        this.statusCallback = statusCallback;
        this.dataCallback = dataCallback;
    }

    public void setLans(ArrayList<JCheckBox> lans) {
        this.lans = lans;
    }

    public HealthStatusData getLastHealth() {
        return lastHealth;
    }

    public boolean isConnected() {
        return controlConnected.get();
    }

    public boolean isStatusConnected() {
        return statusConnected.get();
    }

    public void enqueueCommand(byte[] command) {
        if (command != null && controlConnected.get()) {
            sendQueue.offer(command);
        }
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        // Step 1: bring up the STATUS port first, as required.
        statusConnectionThread = new Thread(this::runStatusConnectionLoop, "status-conn-" + ip + ":" + statusPort);
        statusConnectionThread.start();

        // Step 2: bring up the CONTROL port independently. It does not wait
        // forever on step 1 - both retry on their own schedule - but the
        // thread ordering above means status connects first in the normal
        // (both-reachable) case.
        controlConnectionThread = new Thread(this::runControlConnectionLoop, "control-conn-" + ip + ":" + controlPort);
        controlConnectionThread.start();
    }

    public void shutdown() {
        running = false;
        closeStatusSocket();
        closeControlSocket();
    }

    // ------------------------------------------------------------------
    // STATUS PORT (20108): connect, send enable command, read ASCII lines
    // ------------------------------------------------------------------

    private void runStatusConnectionLoop() {
        while (running) {
            try {
                statusSocket = new Socket();
                statusSocket.connect(new InetSocketAddress(ip, statusPort), 2000);
                statusOut = statusSocket.getOutputStream();
                statusConnected.set(true);
                System.out.println("Status port connected: " + ip + ":" + statusPort);

                if (enableCommand != null && enableCommand.length > 0) {
                    statusOut.write(enableCommand);
                    statusOut.flush();
                    System.out.println("Sent status-enable command on " + ip + ":" + statusPort);
                }

                startStatusReceiver();

                while (running && statusSocket != null && !statusSocket.isClosed()) {
                    Thread.sleep(200);
                }

            } catch (Exception e) {
                System.out.println("Status port connection failed for " + ip + ":" + statusPort);
                statusConnected.set(false);
                closeStatusSocket();
                sleepQuiet(2000);
            }
        }
    }

    private void startStatusReceiver() {
        statusReceiverThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(statusSocket.getInputStream()));
                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleStatusLine(line.trim());
                }
            } catch (Exception e) {
                System.out.println("Status receiver stopped for " + ip + ":" + statusPort);
            } finally {
                statusConnected.set(false);
            }
        }, "status-recv-" + ip + ":" + statusPort);
        statusReceiverThread.start();
    }

    private void handleStatusLine(String line) {
        if (line.isEmpty() || !line.startsWith("CO.HL.G")) {
            return;
        }

        // CO.HL.G,PAOnOff,vswrAlarm,temperatureAlarm,FwdPower,RefPower
        String[] parts = line.split(",");
        if (parts.length < 6) {
            System.out.println("Malformed health status line: " + line);
            return;
        }

        try {
            HealthStatusData h = new HealthStatusData();
            h.paOn = "1".equals(parts[1].trim());
            h.vswrAlarm = "1".equals(parts[2].trim());
            h.temperatureAlarm = "1".equals(parts[3].trim());
            h.fwdPower = Double.parseDouble(parts[4].trim());
            h.refPower = Double.parseDouble(parts[5].trim());
            this.lastHealth = h;

            if (dataCallback != null) {
                dataCallback.accept(adaptToHardwareData(h));
            }
        } catch (NumberFormatException nfe) {
            System.out.println("Failed to parse health status line: " + line);
        }
    }

    /**
     * Adapts alarm-flag based health data into the existing HardwareData
     * shape so this handler is a drop-in replacement for the UI's existing
     * dataCallback wiring. See class javadoc for the sentinel-value
     * rationale; replace with a dedicated HealthStatusData callback in the
     * UI if you'd rather not reuse the vswr/temp threshold coloring.
     */
    private HardwareData adaptToHardwareData(HealthStatusData h) {
        HardwareData data = new HardwareData();
        data.paOn = h.paOn;
        data.vswr = h.vswrAlarm ? 4.0 : 1.0;   // sentinel: trips existing >=3.5 red threshold when alarm is set
        data.temp = h.temperatureAlarm ? 70 : 30; // sentinel: trips existing >=60 red threshold when alarm is set
        data.fwd = (int) Math.round(h.fwdPower);
        data.rfl = (int) Math.round(h.refPower);
        return data;
    }

    private void closeStatusSocket() {
        statusConnected.set(false);
        try {
            if (statusSocket != null) statusSocket.close();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------
    // CONTROL PORT (7): connect, send queued on/off commands
    // ------------------------------------------------------------------

    private void runControlConnectionLoop() {
        while (running) {
            try {
                controlSocket = new Socket();
                controlSocket.connect(new InetSocketAddress(ip, controlPort), 2000);
                controlOut = controlSocket.getOutputStream();
                controlConnected.set(true);
                System.out.println("Control port connected: " + ip + ":" + controlPort);

                if (statusCallback != null) {
                    statusCallback.accept(true);
                }

                startControlSender();

                while (running && controlSocket != null && !controlSocket.isClosed()) {
                    Thread.sleep(200);
                }

            } catch (Exception e) {
                System.out.println("Control port connection failed for " + ip + ":" + controlPort);
                controlConnected.set(false);
                if (statusCallback != null) {
                    statusCallback.accept(false);
                }
                closeControlSocket();
                sleepQuiet(2000);
            }
        }
    }

    private void startControlSender() {
        controlSenderThread = new Thread(() -> {
            try {
                while (running && controlSocket != null && !controlSocket.isClosed()) {
                    byte[] cmd = sendQueue.take();
                    if (controlOut != null) {
                        controlOut.write(cmd);
                        controlOut.flush();
                        System.out.println("Sent (control " + ip + ":" + controlPort + "): " + bytesToHex(cmd));
                    }
                }
            } catch (Exception e) {
                System.out.println("Control sender stopped for " + ip + ":" + controlPort);
            }
        }, "control-send-" + ip + ":" + controlPort);
        controlSenderThread.start();
    }

    private void closeControlSocket() {
        controlConnected.set(false);
        try {
            if (controlSocket != null) controlSocket.close();
        } catch (Exception ignored) {
        }
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString();
    }
}
