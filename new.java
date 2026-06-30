private HardwareData parseFrame(byte[] frame) {

    HardwareData data = new HardwareData();

    // Payload starts after:
    // 7E 16 04 13 00 18
    int p = 6;

    p++;        // Heartbeat
    p++;        // Status
    p++;        // Fault

    p += 2;     // Voltage

    p += 2;     // Current

    // Temperature (1 byte)
    data.temp = frame[p++] & 0xFF;

    // Forward Power (2 bytes)
    data.fwd = (((frame[p++] & 0xFF) << 8)
              |  (frame[p++] & 0xFF));

    // Reverse Voltage (2 bytes)
    data.rfl = (((frame[p++] & 0xFF) << 8)
              |  (frame[p++] & 0xFF));

    data.paOn = data.fwd > 0;

    return data;
}

-----
private HardwareData parseFrame(byte[] frame) {

    HardwareData data = new HardwareData();

    // Payload starts after:
    // 7E LEN CMD SUBCMD ...
    // Change this if your protocol header length changes.
    int payloadStart = 6;

    // Temperature (offset 7)
    data.temp = frame[payloadStart + 7] & 0xFF;

    // Forward Power (offset 8-9)
    data.fwd =
            ((frame[payloadStart + 8] & 0xFF) << 8)
          |  (frame[payloadStart + 9] & 0xFF);

    // Reverse Power (offset 10-11)
    data.rfl =
            ((frame[payloadStart + 10] & 0xFF) << 8)
          |  (frame[payloadStart + 11] & 0xFF);

    // PA ON
    data.paOn = data.fwd > 0;

    return data;
}

---

private HardwareData parseFrame(byte[] frame) {

    HardwareData data = new HardwareData();

    int payloadStart = 6;

    // Temperature (1 byte)
    data.temp = frame[payloadStart + 7] & 0xFF;

    // Forward Power (2 bytes) - Watts
    data.fwd = ((frame[payloadStart + 8] & 0xFF) << 8)
             |  (frame[payloadStart + 9] & 0xFF);

    // Reverse Power (2 bytes)
    data.rfl = ((frame[payloadStart + 10] & 0xFF) << 8)
             |  (frame[payloadStart + 11] & 0xFF);

    data.paOn = data.fwd > 0;

    return data;
}