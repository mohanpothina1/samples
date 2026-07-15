# Dual-port integration guide

Applies to every control card currently using **port 7**:
`ctrl_01_01`, `ctrl_01_02`, `ctrl_01_03`, `ctrl_01_04`, `ctrl_02_01`,
`ctrl_02_03`, `ctrl_s_06`.

`ctrl_02_02` is unaffected — it already uses 20108 as a single port.
The other standalone cards (`ctrl_s_01/03/04/05/07/08/9/10`) are unaffected —
they already work over a single port with the binary `0x5A..0xA5` framed
`status_command` protocol.

## 1. config.json — add two fields to each port-7 card

```json
{
  "id": "ctrl_01_01",
  "ip": "172.232.50.24",
  "port": 7,
  "status_port": 20108,
  "enable_command": "CO.HL.S,1\r\n",
  "on_command": "06,02,0d,0a,03,02,0d,0a",
  "off_command": "06,00,0d,0a,03,00,0d,0a",
  "modules": [ ... unchanged ... ]
}
```

- `status_port`: **TODO confirm** — assumed same IP as the card, port 20108,
  matching how `ctrl_02_02` is already wired.
- `enable_command`: **TODO confirm exact bytes/string** with your protocol
  doc. Placeholder above follows the `CO.PS.S,0,1` ASCII-command pattern
  already used elsewhere in `ControlSystemUI` (e.g. `psu_02`'s on/off).
  If the real enable command is hex-framed instead of ASCII, encode it the
  same way `on_command`/`off_command` are (comma-separated hex bytes) and
  convert with the existing `hexStringToBytes(...)` helper instead of
  `.getBytes()`.

Cards *without* `status_port` set (null) should keep using the existing
single-port `ControlCardSocketHandler` unchanged.

## 2. ControlCard.java — two new optional fields

```java
public Integer status_port;   // null => existing single-port behavior
public String enable_command; // ASCII (or hex-CSV, see note above) sent once after status_port connects
```

## 3. ControlSystemUI.java — branch the handler creation

In the card-handler loop (where `ControlCardSocketHandler handler = new
ControlCardSocketHandler(...)` is currently created for every card), branch
on whether `status_port` is set:

```java
if (card.status_port != null) {

    byte[] enableBytes = card.enable_command.getBytes(); // or hexStringToBytes(...) if hex-framed

    DualPortControlCardSocketHandler dualHandler = new DualPortControlCardSocketHandler(
            card.ip, card.status_port, card.port, enableBytes,
            isConnected -> {
                updateCardStatus(cardId, isConnected);
                List<JToggleButton> btns = toggleMap.get(card.id);
                if (btns != null) {
                    for (JToggleButton btn : btns) {
                        btn.setEnabled(isConnected);
                    }
                }
            },
            data -> {
                SwingUtilities.invokeLater(() -> {
                    for (int i = 0; i < card.modules.size(); i++) {
                        JTextField vswrField = vswrList.get(startIndex + i);
                        JTextField tempField = tempList.get(startIndex + i);

                        // same red/yellow/green logic you already have for
                        // vswrField / tempField / fwdList / rflList using `data`
                    }
                });
            });

    dualHandler.start();
    dualHandler.setLans(temp);
    dualCardHandlers.put(card.id, dualHandler); // new Map<String, DualPortControlCardSocketHandler>

} else {
    // existing ControlCardSocketHandler(...) code, unchanged
}
```

You'll need a second map (e.g. `dualCardHandlers`) alongside `cardHandlers`,
and every place that currently does
`cardHandlers.get(card.id).enqueueCommand(...)` / `.isConnected()` /
`.shutdown()` needs a matching branch that checks the second map too
(power-on-all, power-off-all, toggle listeners, `dispose()`).

## Why two independent connection loops instead of one "connect A then B"

The requirement was to connect 20108 first and 7 second **without losing
either connection once both are up**. Making them two independent
reconnect loops (rather than one sequential state machine) means a blip on
the control port doesn't tear down the status telemetry stream, and vice
versa — each reconnects on its own 2s backoff.

## What's still an assumption, not a fact

1. Whether `status_port` really is 20108-on-the-same-IP for every port-7
   card, or a different shared port/IP.
2. The literal bytes of `enable_command`.
3. Whether `CO.HL.G` lines are pushed automatically every 300ms once
   enabled (assumed here — handler doesn't poll it, just reads whatever
   arrives), or whether you must also poll/query them yourself.

Please check these three against your device protocol doc before wiring
this into the live UI — I flagged them rather than baking in a guess that
looks confident but might not match the hardware.
