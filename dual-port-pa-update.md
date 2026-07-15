# Dual Port PA Update

Use this update only for the PA bands that need separate status/control connections.
Normal PA cards continue to use the existing `ControlCardSocketHandler`.

## 1. Add these fields in `ControlSystemUI`

```java
private static final int DUAL_PA_STATUS_PORT = 20108;
private static final int DUAL_PA_CONTROL_PORT = 7;
private static final byte[] DUAL_PA_STATUS_ENABLE_COMMAND = "CO.HL.G\r\n".getBytes(StandardCharsets.US_ASCII);

private boolean isDualPortPa(Module module) {
	return module.name.equalsIgnoreCase("5470-5725MHz")
			|| module.name.equalsIgnoreCase("5700-5900MHz");
}

private boolean isDualPortCard(ControlCard card) {
	for (Module module : card.modules) {
		if (isDualPortPa(module)) {
			return true;
		}
	}
	return false;
}
```

Also add this import:

```java
import java.nio.charset.StandardCharsets;
```

If your actual dual-port bands are different, only change `isDualPortPa(...)`.

## 2. Replace the handler creation block in `ControlSystemUI`

Replace this existing block:

```java
ControlCardSocketHandler handler = new ControlCardSocketHandler(card.ip, card.port,
		hexStringToBytes(card.status_command), isConnected -> {
```

with:

```java
ControlCardSocketHandler handler;

if (isDualPortCard(card)) {
	handler = new DualPortControlCardSocketHandler(card.ip, DUAL_PA_STATUS_PORT, DUAL_PA_CONTROL_PORT,
			DUAL_PA_STATUS_ENABLE_COMMAND, isConnected -> {
```

Then keep the existing `isConnected -> { ... }` body and the existing `data -> { ... }` body unchanged.

At the end of that constructor call, close the `if/else` like this:

```java
			});
} else {
	handler = new ControlCardSocketHandler(card.ip, card.port,
			hexStringToBytes(card.status_command), isConnected -> {
				updateCardStatus(cardId, isConnected);
				List<JToggleButton> btns = toggleMap.get(card.id);
				if (btns != null) {
					for (JToggleButton btn : btns) {
						btn.setEnabled(isConnected);
						if (isConnected) {
							for (Module m : card.modules) {
								if (m.name.equalsIgnoreCase(btn.getText())) {
									cardHandlers.get(card.id).enqueueCommand(hexStringToBytes(m.power_off));
									break;
								}
							}
						}
					}
				}
			}, data -> {
				SwingUtilities.invokeLater(() -> {
					for (int i = 0; i < card.modules.size(); i++) {
						JTextField vswrField = vswrList.get(startIndex + i);
						JTextField tempField = tempList.get(startIndex + i);

						double vswr = data.vswr;
						vswrField.setText(String.valueOf(vswr));

						if (vswr >= 3.5) {
							vswrField.setBackground(Color.RED);
							vswrField.setForeground(Color.WHITE);
							vswrField.setToolTipText("Alert VSWR High..!");
						} else if (vswr >= 2.5) {
							vswrField.setBackground(Color.YELLOW);
							vswrField.setForeground(Color.BLACK);
							vswrField.setToolTipText("Warning VSWR");
						} else {
							vswrField.setBackground(Color.GREEN);
							vswrField.setForeground(Color.BLACK);
							vswrField.setToolTipText(null);
						}

						int temperature = data.temp;
						tempField.setText(String.valueOf(temperature));

						if (temperature >= 60) {
							tempField.setBackground(Color.RED);
							tempField.setForeground(Color.WHITE);
							tempField.setToolTipText("Alert High Temperature..!");
						} else if (temperature >= 45) {
							tempField.setBackground(Color.YELLOW);
							tempField.setForeground(Color.BLACK);
							tempField.setToolTipText("Warning Temperature is above high");
						} else {
							tempField.setBackground(Color.GREEN);
							tempField.setForeground(Color.BLACK);
							tempField.setToolTipText(null);
						}

						fwdList.get(startIndex + i).setText(String.valueOf(data.fwd));
						rflList.get(startIndex + i).setText(String.valueOf(data.rfl));
					}
				});
			});
}
```

This keeps the UI update logic the same. The only difference is that selected cards use the new dual-port handler.

## 3. Add this new class

```java
public class DualPortControlCardSocketHandler extends ControlCardSocketHandler {

	private final String ip;
	private final int statusPort;
	private final int controlPort;
	private final byte[] statusEnableCommand;
	private final Consumer<Boolean> statusCallback;
	private final Consumer<HardwareData> dataCallback;
	private final ArrayBlockingQueue<byte[]> sendQueue = new ArrayBlockingQueue<>(100);
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private volatile boolean running = true;
	private volatile HardwareData lastData;
	private Socket statusSocket;
	private Socket controlSocket;
	private OutputStream controlOut;
	private Thread statusThread;
	private Thread senderThread;
	private ScheduledExecutorService statusEnableScheduler;

	public DualPortControlCardSocketHandler(String ip, int statusPort, int controlPort, byte[] statusEnableCommand,
			Consumer<Boolean> statusCallback, Consumer<HardwareData> dataCallback) {
		super(ip, controlPort, null, statusCallback, dataCallback);
		this.ip = ip;
		this.statusPort = statusPort;
		this.controlPort = controlPort;
		this.statusEnableCommand = statusEnableCommand;
		this.statusCallback = statusCallback;
		this.dataCallback = dataCallback;
	}

	@Override
	public HardwareData getLastData() {
		return lastData;
	}

	@Override
	public void enqueueCommand(byte[] command) {
		if (command != null && connected.get()) {
			sendQueue.offer(command);
		}
	}

	@Override
	public boolean isConnected() {
		return connected.get();
	}

	@Override
	public void shutdown() {
		running = false;
		closeDualSockets();
	}

	@Override
	public void run() {
		while (running) {
			try {
				statusSocket = new Socket();
				statusSocket.connect(new InetSocketAddress(ip, statusPort), 2000);

				controlSocket = new Socket();
				controlSocket.connect(new InetSocketAddress(ip, controlPort), 2000);
				controlOut = controlSocket.getOutputStream();

				connected.set(true);
				if (statusCallback != null) {
					statusCallback.accept(true);
				}

				startStatusReader();
				startSender();
				startStatusEnablePolling();

				while (running && !statusSocket.isClosed() && !controlSocket.isClosed()) {
					Thread.sleep(200);
				}
			} catch (Exception e) {
				System.out.println("Dual PA connection failed for: " + ip + " status:" + statusPort + " control:" + controlPort);
			}

			connected.set(false);
			if (statusCallback != null) {
				statusCallback.accept(false);
			}
			closeDualSockets();

			try {
				Thread.sleep(2000);
			} catch (InterruptedException ignored) {
			}
		}
	}

	private void startStatusEnablePolling() {
		if (statusEnableCommand == null || statusEnableCommand.length == 0) {
			return;
		}

		statusEnableScheduler = Executors.newSingleThreadScheduledExecutor();
		statusEnableScheduler.scheduleAtFixedRate(() -> {
			try {
				if (connected.get() && statusSocket != null && !statusSocket.isClosed()) {
					OutputStream statusOut = statusSocket.getOutputStream();
					statusOut.write(statusEnableCommand);
					statusOut.flush();
				}
			} catch (Exception e) {
				closeDualSockets();
			}
		}, 0, 300, TimeUnit.MILLISECONDS);
	}

	private void startStatusReader() {
		statusThread = new Thread(() -> {
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(statusSocket.getInputStream(), StandardCharsets.US_ASCII));

				String line;
				while (running && (line = reader.readLine()) != null) {
					HardwareData data = parseHealthStatus(line.trim());
					if (data != null) {
						lastData = data;
						if (dataCallback != null) {
							dataCallback.accept(data);
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Dual PA status reader stopped.");
			} finally {
				closeDualSockets();
			}
		});
		statusThread.start();
	}

	private HardwareData parseHealthStatus(String line) {
		if (line == null || !line.startsWith("CO.HL.G")) {
			return null;
		}

		String[] parts = line.split(",");
		if (parts.length < 6) {
			return null;
		}

		try {
			HardwareData data = new HardwareData();
			data.paOn = "1".equals(parts[1].trim());
			data.vswr = Double.parseDouble(parts[2].trim());
			data.temp = (int) Math.round(Double.parseDouble(parts[3].trim()));
			data.fwd = (int) Math.round(Double.parseDouble(parts[4].trim()));
			data.rfl = (int) Math.round(Double.parseDouble(parts[5].trim()));
			return data;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void startSender() {
		senderThread = new Thread(() -> {
			try {
				while (running && controlSocket != null && !controlSocket.isClosed()) {
					byte[] cmd = sendQueue.take();

					if (controlOut != null) {
						controlOut.write(cmd);
						controlOut.flush();
					}
				}
			} catch (Exception e) {
				System.out.println("Dual PA sender stopped.");
			} finally {
				closeDualSockets();
			}
		});
		senderThread.start();
	}

	private void closeDualSockets() {
		connected.set(false);

		try {
			if (statusEnableScheduler != null && !statusEnableScheduler.isShutdown()) {
				statusEnableScheduler.shutdownNow();
			}
		} catch (Exception ignored) {
		}

		try {
			if (statusSocket != null) {
				statusSocket.close();
			}
		} catch (Exception ignored) {
		}

		try {
			if (controlSocket != null) {
				controlSocket.close();
			}
		} catch (Exception ignored) {
		}
	}
}
```

Required imports for this class:

```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
```

## 4. Important check

If the real status enable command is not `CO.HL.G\r\n`, change only this line:

```java
private static final byte[] DUAL_PA_STATUS_ENABLE_COMMAND = "CO.HL.G\r\n".getBytes(StandardCharsets.US_ASCII);
```

For example, if the hardware manual says enable streaming is `CO.HL.S,1`, use:

```java
private static final byte[] DUAL_PA_STATUS_ENABLE_COMMAND = "CO.HL.S,1\r\n".getBytes(StandardCharsets.US_ASCII);
```
