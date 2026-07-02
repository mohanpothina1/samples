@Override
public void run() {

    if (!pingStarted) {
        startPingChecker();
        pingStarted = true;
    }

    while (running) {

        try {

            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 2000);

            out = socket.getOutputStream();

            connected.set(true);

            System.out.println("TCP Connection Started!");
            System.out.println("Connected to " + ip + ":" + port);

            if (statusCallback != null) {
                statusCallback.accept(true);
            }

            startReceiver();

            if (senderThread == null || !senderThread.isAlive()) {
                startSender();
            }

            if (scheduler == null || scheduler.isShutdown()) {
                startAutoPolling();
            }

            while (running && connected.get() && !socket.isClosed()) {
                Thread.sleep(200);
            }

        } catch (Exception e) {

            connected.set(false);

            if (statusCallback != null) {
                statusCallback.accept(false);
            }

            closeSocket();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}

private void closeSocket() {

    connected.set(false);

    try {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    } catch (Exception ignored) {
    }

    try {
        if (receiver != null) {
            receiver.interrupt();
            receiver = null;
        }
    } catch (Exception ignored) {
    }

    try {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    } catch (Exception ignored) {
    }

    socket = null;
    out = null;
}

private void startSender() {

    senderThread = new Thread(() -> {

        while (running) {

            try {

                byte[] cmd = sendQueue.take();

                if (!connected.get()) {
                    continue;
                }

                if (out != null) {
                    out.write(cmd);
                    out.flush();

                    System.out.println("Sent: " + bytesToHex(cmd));
                }

            } catch (InterruptedException e) {
                break;

            } catch (Exception e) {

                connected.set(false);
                closeSocket();
            }

        }

    });

    senderThread.setDaemon(true);
    senderThread.start();
}

public void enqueueCommand(byte[] command) {

    if (command == null) {
        return;
    }

    sendQueue.offer(command);
}

------------

private void handleFrame(byte[] frame) {

    System.out.println("====================================");
    System.out.println("RX Frame : " + bytesToHex(frame));
    System.out.println("Frame Length : " + frame.length);

    boolean valid = verifyChecksum(frame);
    System.out.println("Checksum Valid : " + valid);

    if (!valid) {
        System.out.println("Checksum Failed");
        return;
    }

    try {

        HardwareData data = parseFrame(frame);

        lastData = data;

        System.out.println("Parsed Successfully");
        System.out.println("TEMP : " + data.temp);
        System.out.println("FWD  : " + data.fwd);
        System.out.println("RFL  : " + data.rfl);
        System.out.println("VSWR : " + data.vswr);
        System.out.println("PA ON: " + data.paOn);

        if (dataCallback != null) {
            System.out.println("Updating UI...");
            dataCallback.accept(data);
        } else {
            System.out.println("dataCallback is NULL");
        }

    } catch (Exception e) {

        System.out.println("Parser Exception");
        e.printStackTrace();
    }

    System.out.println("====================================");
}

private HardwareData parseFrame(byte[] frame) {

    HardwareData data = new HardwareData();

    if (frame.length < 14) {
        System.out.println("Packet too short for parsing.");
        return data;
    }

    int payLoad = 6;

    payLoad++;
    payLoad++;
    payLoad++;

    payLoad += 2;
    payLoad += 2;

    data.temp = frame[payLoad++] & 0xFF;

    int fwdRaw =
            ((frame[payLoad++] & 0xFF) << 8)
          |  (frame[payLoad++] & 0xFF);

    data.fwd = fwdRaw;

    int rflRaw =
            ((frame[payLoad++] & 0xFF) << 8)
          |  (frame[payLoad++] & 0xFF);

    data.rfl = rflRaw;

    if (fwdRaw > rflRaw && fwdRaw > 0) {

        double gamma = Math.sqrt((double) rflRaw / fwdRaw);

        if (gamma < 1.0)
            data.vswr = (1 + gamma) / (1 - gamma);
        else
            data.vswr = 99.9;

    } else {

        data.vswr = 1.0;

    }

    data.paOn = (fwdRaw > 0);

    return data;
}


=============


Overall Flow
UI
 │
 │ creates
 ▼
ControlCardSocketHandler
 │
 │
 ├── Connect to TCP
 ├── Start Receiver Thread
 ├── Start Sender Thread
 ├── Start Auto Polling
 ├── Parse Incoming Data
 ├── Update UI
 └── Reconnect if disconnected

Think of it as a manager for one control card.

Member Variables
private final String ip;
private final int port;

Which control card to connect.

Example

192.168.127.253
4003
private final byte[] statusCommand;

The command sent every second.

Example

7E 16 03 13 00 00 D4 7F

This asks

"Give me your latest status."

private Thread senderThread;

Separate thread only for sending commands.

private Consumer<Boolean> statusCallback;

Whenever connection changes

Connected
Disconnected

UI is informed.

Example

statusCallback.accept(true);

UI turns GREEN.

private Consumer<HardwareData> dataCallback;

Whenever hardware data arrives

VSWR
TEMP
FWD
RFL

UI is updated.

private ArrayBlockingQueue<byte[]> sendQueue;

This is extremely important.

Instead of sending immediately,

commands go here.

Example

Power ON
Status Query
Power OFF

Queue

+----------------------+
| Status               |
| Power ON             |
| Status               |
| Status               |
+----------------------+

Sender thread sends them one by one.

private volatile boolean running = true;

Controls whole handler.

When false

Everything stops.

private AtomicBoolean connected

Current TCP status.

true

means socket alive.

Socket socket;
OutputStream out;

Actual TCP socket.

ScheduledExecutorService scheduler;

Automatically sends status command every second.

TCPReceive receiver;

Separate thread reading packets.

HardwareData lastData;

Stores latest values

Temp

VSWR

FWD

RFL

UI reads this later.

Constructor
public ControlCardSocketHandler(...)

Only stores everything.

Nothing starts here.

start()
public void start() {
    new Thread(this).start();
}

Since

implements Runnable

this executes

run()

inside another thread.

run()

This is the heart.

run()

Loop

while(running)

means

Connect
↓

Disconnected?

↓

Reconnect

↓

Disconnected?

↓

Reconnect

↓

Forever
Connect
socket = new Socket();

socket.connect(...)

Creates TCP connection.

PC ------------ Hardware
Get Output Stream
out = socket.getOutputStream();

Now Java can send bytes.

out.write(...)
Connected
connected.set(true);

Marks

Connection alive
Notify UI
statusCallback.accept(true);

UI changes

RED

↓

GREEN
Receiver
startReceiver();

Creates

TCPReceive

Thread.

Its only job

Read packets.

Nothing else.

Sender
startSender();

Creates sender thread.

Its only job

Take command from queue

↓

Send to hardware
Polling
startAutoPolling();

Creates scheduler.

Every second

enqueueCommand(statusCommand)

So queue becomes

Status

Status

Status

Status

Then

Sender

takes one

↓

out.write()

Hardware replies.

Receiver

Receiver receives

RX

7E

16

03

...

7F

Then

handleFrame(frame)

is called.

handleFrame()
verifyChecksum()

First checks packet validity.

If wrong

Discard.

If valid

parseFrame()

Extracts

Temp

FWD

RFL

VSWR

Creates

HardwareData

Then

lastData=data;

Stores latest values.

Then

dataCallback.accept(data);

UI updates

TEMP textbox

↓

VSWR textbox

↓

FWD textbox

↓

RFL textbox
parseFrame()

This is only decoding bytes.

Example

Packet

7E

16

03

13

00

1B

54

02

80

01

20

00

30

XX

7F

It skips header

7E

16

03

13

00

1B

Reads

Temp

↓

FWD

↓

RFL

Calculates

VSWR

Returns

HardwareData
enqueueCommand()

UI presses

Power ON

Instead of

write()

it does

enqueueCommand()

Queue

Power ON

Sender thread later sends it.

This avoids multiple threads writing to socket simultaneously.

startSender()

Loop

while(running)
↓

wait

↓

Queue gets command

↓

Take command

↓

Send

↓

Repeat
startAutoPolling()

Every second

enqueueCommand(statusCommand)

Queue

Status

↓

Sender

↓

Hardware

↓

Receiver

↓

UI
startPingChecker()

Every second

IPAddress.tester(ip)

Example

PING

true

Checkbox becomes GREEN.

If

false
closeSocket()

Reconnect starts.

closeSocket()

Stops

Receiver

Scheduler

Socket

Marks

connected=false

Then

run()

creates a new connection.

Complete Architecture
                UI

                 │

                 │ enqueueCommand()

                 ▼

          ArrayBlockingQueue

                 │

                 ▼

          Sender Thread

                 │

                 ▼

              TCP Socket

                 │

     ----------------------------

                 │

              Hardware

                 │

     ----------------------------

                 │

                 ▼

          Receiver Thread

                 │

                 ▼

          handleFrame()

                 │

                 ▼

          parseFrame()

                 │

                 ▼

          HardwareData

                 │

                 ▼

         dataCallback()

                 │

                 ▼

                UI
One design issue

This class is doing too many responsibilities:

Managing TCP connections.
Sending commands.
Receiving frames.
Polling hardware.
Parsing packets.
Monitoring network reachability.
Updating the UI through callbacks.