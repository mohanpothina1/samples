/**
 * Parsed representation of a "CO.HL.G" health-status line received on the
 * status port (e.g. 20108).
 *
 * Example wire frame (ASCII, CRLF terminated):
 *   CO.HL.G,1,0,0,50.00,30.00\r\n
 *
 * Field order per the protocol table:
 *   PAOnOff, vswrAlarm, temperatureAlarm, fwdPower, refPower
 *
 * This is intentionally a *separate* class from HardwareData (which is used
 * by the existing binary 0x5A..0xA5 framed cards) because the two protocols
 * don't carry the same information: this one gives you alarm flags rather
 * than a raw VSWR ratio / raw temperature.
 */
public class HealthStatusData {

    public boolean paOn;          // PAOnOff
    public boolean vswrAlarm;     // vswr alarm flag
    public boolean temperatureAlarm; // temperature alarm flag
    public double fwdPower;       // Fwd Power
    public double refPower;       // Ref Power (reflected)

    @Override
    public String toString() {
        return "HealthStatusData{" +
                "paOn=" + paOn +
                ", vswrAlarm=" + vswrAlarm +
                ", temperatureAlarm=" + temperatureAlarm +
                ", fwdPower=" + fwdPower +
                ", refPower=" + refPower +
                '}';
    }
}
