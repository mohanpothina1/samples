Add this helper method inside PAControlSystem

private void updateAllBandUI(boolean on) {

    SwingUtilities.invokeLater(() -> {

        for (List<JToggleButton> toggles : toggleMap.values()) {

            for (JToggleButton toggle : toggles) {

                toggle.setSelected(on);
                toggle.setForeground(on ? Color.GREEN : Color.RED);

                // keep disabled
                toggle.setEnabled(false);
            }
        }
    });
}

Create helper method to power ON all control card modules

private void powerOnAllBands(Config config) {

    // PSU linked cards
    for (PowerSupply psu : config.power_supplies) {

        for (ControlCard card : psu.control_cards) {

            ControlCardSocketHandler handler =
                    cardHandlers.get(card.id);

            if (handler != null && handler.isConnected()) {

                for (Module m : card.modules) {

                    handler.enqueueCommand(
                            hexStringToBytes(m.power_on));

                    try {
                        Thread.sleep(50);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    // Standalone cards
    for (ControlCard card : config.standalone_control_cards) {

        ControlCardSocketHandler handler =
                cardHandlers.get(card.id);

        if (handler != null && handler.isConnected()) {

            for (Module m : card.modules) {

                handler.enqueueCommand(
                        hexStringToBytes(m.power_on));

                try {
                    Thread.sleep(50);
                } catch (Exception ignored) {
                }
            }
        }
    }

    updateAllBandUI(true);
}


Modify your existing powerOffAllPAs().

At the end add:
updateAllBandUI(false);
So it becomes:

private void powerOffAllPAs(Config config) {

    ...
    existing code
    ...

    updateAllBandUI(false);
}

Modify POWER ON ALL button
Replace:
powerOnAll.addActionListener(e -> {
    ....
});


with:

powerOnAll.addActionListener(e -> {

    // Existing PSU ON logic
    for (PowerSupply psu : config.power_supplies) {

        PowerSupplySocketHandler psuHandler =
                psuHandlers.get(psu.id);

        if (psuHandler != null && psuHandler.isConnected()) {

            if (psu.id.equals("psu_02")) {

                psuHandler.enqueueCommand(
                        "CO.PS.S,0,1".getBytes());

            } else {

                for (ControlCard card : psu.control_cards) {

                    if (card.on_command != null &&
                            !card.on_command.isEmpty()) {

                        splitCmd(card.on_command, psuHandler);

                        psuHandler.enqueueCommand(
                                hexStringToBytes(card.on_command));
                    }
                }
            }
        }
    }

    // NEW
    powerOnAllBands(config);
});

Prevent control card reconnect from enabling buttons

Currently you have:
btn.setEnabled(isConnected);

Change to:

btn.setEnabled(false);

because you don't want manual control anymore.

So:

if (btns != null) {

    for (JToggleButton btn : btns) {

        btn.setEnabled(false);

        for (Module m : card.modules) {

            if (m.name.equalsIgnoreCase(btn.getText())) {

                cardHandlers.get(card.id)
                        .enqueueCommand(
                                hexStringToBytes(m.power_off));

                break;
            }
        }
    }
}


Replace this entire block:

for (Module m : card.modules) {
    JToggleButton toggle = new JToggleButton(m.name);
    toggle.setFont(new Font("Arial", Font.BOLD, 28));
    toggle.setForeground(Color.RED);
    toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));

    if (!m.name.equalsIgnoreCase("")) {
        toggle.setEnabled(false);
    }

    toggle.addItemListener(e -> {
        if (!toggle.isEnabled())
            return;

        ControlCardSocketHandler handler = cardHandlers.get(card.id);

        if (handler == null || !handler.isConnected()) {

            byte[] cmd = hexStringToBytes(
                    toggle.isSelected()
                            ? m.power_on
                            : m.power_off);

            toggle.setForeground(
                    toggle.isSelected()
                            ? Color.GREEN
                            : Color.RED);

            handler.enqueueCommand(cmd);
        }
    });

    bandsPanel.add(toggle);
    toggles.add(toggle);
}

with this:

for (Module m : card.modules) {

    JToggleButton toggle = new JToggleButton(m.name);

    toggle.setFont(new Font("Arial", Font.BOLD, 28));
    toggle.setForeground(Color.RED);

    // Status indicator only
    toggle.setEnabled(false);
    toggle.setFocusPainted(false);

    bandsPanel.add(toggle);
    toggles.add(toggle);
}


Another improvement

Inside your connection callback you currently have:

for (JToggleButton btn : btns) {
    btn.setEnabled(false);

    for (Module m : card.modules) {
        if (m.name.equalsIgnoreCase(btn.getText())) {
            cardHandlers.get(card.id)
                    .enqueueCommand(hexStringToBytes(m.power_off));
            break;
        }
    }
}

I would remove the automatic power-off command on every reconnect.

Change to:

for (JToggleButton btn : btns) {
    btn.setEnabled(false);
}


Inside your powerOnAllBands() method, change:

System.out.println("POWER ON -> " + m.name);
System.out.println("Command -> " + m.power_on);

handler.enqueueCommand(
        hexStringToBytes(m.power_on));



Better: Print Hex Bytes Actually Sent

If you want to verify the exact bytes being sent:

byte[] cmd = hexStringToBytes(m.power_on);

System.out.print("POWER ON -> " + m.name + " : ");

for (byte b : cmd) {
    System.out.printf("%02X ", b & 0xFF);
}
System.out.println();

handler.enqueueCommand(cmd);

Example output:

POWER ON -> 5100 - 5300 MHz :
24 03 02 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 24

POWER ON -> 1560 - 1670 MHz(GNSS-L1) :
24 03 02 02 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 24



Inside startSender():

byte[] cmd = sendQueue.take();

System.out.print("Sending to " + ip + ":" + port + " -> ");

for (byte b : cmd) {
    System.out.printf("%02X ", b & 0xFF);
}
System.out.println();

out.write(cmd);
out.flush();



powerOnAll.addActionListener(e -> {

    System.out.println("========== POWER ON ALL CLICKED ==========");

    for (String cardId : cardHandlers.keySet()) {

        ControlCardSocketHandler h =
                cardHandlers.get(cardId);

        System.out.println(
                cardId +
                " connected = " +
                (h != null && h.isConnected()));
    }

    powerOnAllBands(config);
});

