private void powerOnAllBands(Config config) {

    boolean commandSent = false;

    // PSU linked cards
    for (PowerSupply psu : config.power_supplies) {

        for (ControlCard card : psu.control_cards) {

            ControlCardSocketHandler handler =
                    cardHandlers.get(card.id);

            if (handler != null && handler.isConnected()) {

                commandSent = true;

                for (Module m : card.modules) {

                    handler.enqueueCommand(
                            hexStringToBytes(m.power_on));
                }
            }
        }
    }

    // Standalone cards
    for (ControlCard card : config.standalone_control_cards) {

        ControlCardSocketHandler handler =
                cardHandlers.get(card.id);

        if (handler != null && handler.isConnected()) {

            commandSent = true;

            for (Module m : card.modules) {

                handler.enqueueCommand(
                        hexStringToBytes(m.power_on));
            }
        }
    }

    if (commandSent) {
        updateAllBandUI(true);
    } else {
        System.out.println("No control cards connected.");
    }
}



Replace:

bandsPanel.setLayout(new GridLayout(4, 1, 10, 10));

with:

bandsPanel.setLayout(new GridLayout(0, 1, 10, 10));

0 rows means:

Create as many rows as needed
1 column only



User selects checkboxes.
Clicking POWER ON ALL sends ON commands only for checked bands.
Clicking POWER OFF ALL sends OFF commands only for checked bands.
Toggle/indicator turns Green for ON and Red for OFF.

private final Map<String, JCheckBox> bandCheckBoxes = new HashMap<>();

Create checkbox beside each band

Replace:

for (Module m : card.modules) {

    JToggleButton toggle = new JToggleButton(m.name);

    bandsPanel.add(toggle);
    toggles.add(toggle);
}

with:

for (Module m : card.modules) {

    JPanel row = new JPanel(new BorderLayout());

    JCheckBox checkBox = new JCheckBox();
    bandCheckBoxes.put(m.name, checkBox);

    JToggleButton toggle = new JToggleButton(m.name);
    toggle.setEnabled(false);
    toggle.setForeground(Color.RED);

    row.add(checkBox, BorderLayout.WEST);
    row.add(toggle, BorderLayout.CENTER);

    bandsPanel.add(row);
    toggles.add(toggle);
}


Step 1: Store checkboxes

Instead of:

private final Map<String, JCheckBox> bandCheckBoxes = new HashMap<>();

I recommend:

private final Map<String, List<JCheckBox>> checkBoxMap = new HashMap<>();



Step 2: Create checkbox list

Inside:

for (ControlCard card : allCards)

create:

List<JCheckBox> checkBoxes = new ArrayList<>();

Then:


for (Module m : card.modules) {

    JPanel row = new JPanel(new BorderLayout());

    JCheckBox checkBox = new JCheckBox();
    checkBox.setEnabled(false);

    checkBoxes.add(checkBox);

    JToggleButton toggle = new JToggleButton(m.name);
    toggle.setEnabled(false);
    toggle.setForeground(Color.RED);

    row.add(checkBox, BorderLayout.WEST);
    row.add(toggle, BorderLayout.CENTER);

    bandsPanel.add(row);
    toggles.add(toggle);
}

After loop:

checkBoxMap.put(card.id, checkBoxes);
toggleMap.put(card.id, toggles);

Enable checkboxes when LAN connects

Inside your callback:

isConnected -> {

    updateCardStatus(card.id, isConnected);

    List<JCheckBox> boxes =
            checkBoxMap.get(card.id);

    if (boxes != null) {

        SwingUtilities.invokeLater(() -> {

            for (JCheckBox box : boxes) {

                box.setEnabled(isConnected);

                if (!isConnected) {
                    box.setSelected(false);
                }
            }
        });
    }
}

private void powerOnAllBands(Config config) {

    System.out.println("========== POWER ON SELECTED BANDS ==========");

    boolean commandSent = false;

    // PSU linked cards
    for (PowerSupply psu : config.power_supplies) {

        for (ControlCard card : psu.control_cards) {

            ControlCardSocketHandler handler =
                    cardHandlers.get(card.id);

            List<JCheckBox> boxes =
                    checkBoxMap.get(card.id);

            System.out.println(
                    "Card=" + card.id +
                    " Connected=" +
                    (handler != null && handler.isConnected())
            );

            if (handler != null &&
                    handler.isConnected() &&
                    boxes != null) {

                List<JToggleButton> toggles =
                        toggleMap.get(card.id);

                for (int i = 0; i < card.modules.size(); i++) {

                    Module m = card.modules.get(i);

                    if (i < boxes.size() &&
                            boxes.get(i).isSelected()) {

                        commandSent = true;

                        System.out.println(
                                "POWER ON -> " + m.name);
                        System.out.println(
                                "COMMAND -> " + m.power_on);

                        handler.enqueueCommand(
                                hexStringToBytes(m.power_on));

                        if (toggles != null &&
                                i < toggles.size()) {

                            toggles.get(i)
                                    .setForeground(Color.GREEN);

                            toggles.get(i)
                                    .setSelected(true);
                        }

                        try {
                            Thread.sleep(50);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    // Standalone cards
    for (ControlCard card : config.standalone_control_cards) {

        ControlCardSocketHandler handler =
                cardHandlers.get(card.id);

        List<JCheckBox> boxes =
                checkBoxMap.get(card.id);

        if (handler != null &&
                handler.isConnected() &&
                boxes != null) {

            List<JToggleButton> toggles =
                    toggleMap.get(card.id);

            for (int i = 0; i < card.modules.size(); i++) {

                Module m = card.modules.get(i);

                if (i < boxes.size() &&
                        boxes.get(i).isSelected()) {

                    commandSent = true;

                    System.out.println(
                            "POWER ON -> " + m.name);
                    System.out.println(
                            "COMMAND -> " + m.power_on);

                    handler.enqueueCommand(
                            hexStringToBytes(m.power_on));

                    if (toggles != null &&
                            i < toggles.size()) {

                        toggles.get(i)
                                .setForeground(Color.GREEN);

                        toggles.get(i)
                                .setSelected(true);
                    }

                    try {
                        Thread.sleep(50);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    if (!commandSent) {
        System.out.println(
                "No selected bands or no connected cards.");
    }
}



Updated powerOffAllPAs()

Replace the band OFF portion with:

for (ControlCard card : config.standalone_control_cards) {

    ControlCardSocketHandler handler =
            cardHandlers.get(card.id);

    List<JCheckBox> boxes =
            checkBoxMap.get(card.id);

    List<JToggleButton> toggles =
            toggleMap.get(card.id);

    if (handler != null &&
            handler.isConnected() &&
            boxes != null) {

        for (int i = 0; i < card.modules.size(); i++) {

            Module m = card.modules.get(i);

            if (i < boxes.size() &&
                    boxes.get(i).isSelected()) {

                System.out.println(
                        "POWER OFF -> " + m.name);

                handler.enqueueCommand(
                        hexStringToBytes(m.power_off));

                if (toggles != null &&
                        i < toggles.size()) {

                    toggles.get(i)
                            .setForeground(Color.RED);

                    toggles.get(i)
                            .setSelected(false);
                }

                try {
                    Thread.sleep(50);
                } catch (Exception ignored) {
                }
            }
        }
    }
}