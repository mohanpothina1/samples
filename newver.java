
private HardwareData parseFrame(byte[] frame) {

    HardwareData data = new HardwareData();

    // Payload starts after:
    // SOF + Function + Address + Command + Ack + Length
    int payload = 6;

    // ---------------- Temperature ----------------
    data.temp = frame[payload + 7] & 0xFF;

    // ---------------- Forward Power ----------------
    int fwdRaw =
            ((frame[payload + 8] & 0xFF) << 8) |
             (frame[payload + 9] & 0xFF);

    data.fwd = fwdRaw;

    // ---------------- Reverse Power ----------------
    int revRaw =
            ((frame[payload + 10] & 0xFF) << 8) |
             (frame[payload + 11] & 0xFF);

    data.rfl = revRaw;

    // ---------------- VSWR ----------------
    if (fwdRaw > revRaw && fwdRaw > 0) {

        double gamma = Math.sqrt((double) revRaw / fwdRaw);

        if (gamma < 1.0) {
            data.vswr = (1 + gamma) / (1 - gamma);
        } else {
            data.vswr = 99.9;
        }

    } else {
        data.vswr = 1.0;
    }

    data.paOn = (fwdRaw > 0);

    return data;
}

double dBm = fwdRaw / 10.0;
double watts = Math.pow(10, (dBm - 30) / 10.0);

public class TCPReceive extends Thread {

    private final Socket socket;
    private final Consumer<byte[]> frameCallback;

    public TCPReceive(Socket socket, Consumer<byte[]> frameCallback) {
        this.socket = socket;
        this.frameCallback = frameCallback;
    }

    @Override
    public void run() {

        try {

            socket.setSoTimeout(3000);

            InputStream in = socket.getInputStream();

            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {

                try {

                    byte[] frame = readFrame(in);

                    if (frame != null && frameCallback != null) {
                        System.out.println("RX : " + bytesToHex(frame));
                        frameCallback.accept(frame);
                    }

                } catch (SocketTimeoutException e) {
                    // No data received within timeout.
                    // Continue looping so the socket can be closed/reconnected.
                }

            }

        } catch (Exception e) {
            System.out.println("Receiver stopped : " + e.getMessage());
        }
    }

    private byte[] readFrame(InputStream in) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int b;

        // Wait for start byte (0x7E)
        while ((b = in.read()) != -1) {

            if (b == 0x7E) {
                buffer.write(b);
                break;
            }

        }

        if (b == -1) {
            return null;
        }

        // Read until end byte (0x7F)
        while ((b = in.read()) != -1) {

            buffer.write(b);

            if (b == 0x7F) {
                break;
            }

            if (buffer.size() > 512) {
                System.out.println("Frame too large. Discarding.");
                return null;
            }

        }

        return buffer.toByteArray();
    }

    private String bytesToHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }

        return sb.toString();
    }
}


private void closeSocket() {

    connected.set(false);

    try {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    } catch (Exception ignored) {
    }

    try {
        if (receiver != null) {
            receiver.interrupt();
        }
    } catch (Exception ignored) {
    }

    try {
        if (socket != null) {
            socket.close();
        }
    } catch (Exception ignored) {
    }
}


private boolean verifyChecksum(byte[] frame) {

    // Minimum frame:
    // SOF FUN ADDR CMD ACK LEN CHK EOF
    if (frame == null || frame.length < 8)
        return false;

    // Last byte must be EOF
    if ((frame[frame.length - 1] & 0xFF) != 0x7F)
        return false;

    int sum = 0;

    // Sum everything except SOF and EOF
    // FUN ... CHECKSUM
    for (int i = 1; i < frame.length - 1; i++) {
        sum += frame[i] & 0xFF;
    }

    return (sum & 0xFF) == 0;
}

Then in handleFrame() change

if (!verifyChecksum(frame)) {
    System.out.println("Checksum Failed " + bytesToHex(frame));
    return;
}

private double calculateVSWR(int forward, int reverse) {

    if (forward <= 0)
        return 1.0;

    double rho = Math.sqrt((double) reverse / forward);

    if (rho >= 1.0)
        return 99.9;

    return (1 + rho) / (1 - rho);
}




Then

data.vswr = calculateVSWR(data.fwd, data.rfl);

-------------


private boolean verifyChecksum(byte[] frame) {

    if (frame == null || frame.length < 7)
        return false;

    // First byte = SOF
    if ((frame[0] & 0xFF) != 0x7E)
        return false;

    // Last byte = EOF
    if ((frame[frame.length - 1] & 0xFF) != 0x7F)
        return false;

    int sum = 0;

    // Skip SOF (index 0)
    // Skip checksum (length-2)
    // Skip EOF (length-1)
    for (int i = 1; i < frame.length - 2; i++) {
        sum += frame[i] & 0xFF;
    }

    sum += frame[frame.length - 2] & 0xFF;

    return (sum & 0xFF) == 0;
}

--------------

private boolean verifyChecksum(byte[] frame) {

    if (frame == null || frame.length < 7)
        return false;

    if ((frame[0] & 0xFF) != 0x7E)
        return false;

    if ((frame[frame.length - 1] & 0xFF) != 0x7F)
        return false;

    int sum = 0;

    // Function code to last data byte
    for (int i = 1; i < frame.length - 2; i++) {
        sum += frame[i] & 0xFF;
    }

    // Include checksum
    sum += frame[frame.length - 2] & 0xFF;

    return (sum & 0xFF) == 0;
}