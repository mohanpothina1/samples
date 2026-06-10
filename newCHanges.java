Currently:

JPanel bandsPanel = new JPanel();
bandsPanel.setBorder(new LineBorder(SystemColor.activeCaption));
centerPanel.add(bandsPanel, BorderLayout.CENTER);
bandsPanel.setLayout(new GridLayout(4, 1, 10, 10));

Change to:

JPanel bandsPanel = new JPanel();
bandsPanel.setBorder(new LineBorder(SystemColor.activeCaption));
centerPanel.add(bandsPanel, BorderLayout.CENTER);

bandsPanel.setLayout(new GridLayout(1, 2, 15, 15));




Inside:

for (ControlCard card : allCards)

create:

JPanel paPanel = new JPanel();
paPanel.setBorder(
    new TitledBorder(
        card.id.equals("ctrl_s_1") ? "PA1" : "PA2"
    )
);

paPanel.setLayout(new GridLayout(0, 1, 5, 5));

bandsPanel.add(paPanel);


Instead of:

bandsPanel.add(row);

use:

paPanel.add(row);

where:

JPanel row = new JPanel(new BorderLayout());

JCheckBox checkBox = new JCheckBox();

JToggleButton toggle = new JToggleButton(m.name);
toggle.setEnabled(false);

row.add(checkBox, BorderLayout.WEST);
row.add(toggle, BorderLayout.CENTER);

paPanel.add(row);




Before loop:

int paNumber = 1;

Inside loop:

JPanel paPanel = new JPanel();

paPanel.setBorder(
    new TitledBorder("PA" + paNumber++)
);

paPanel.setLayout(new GridLayout(0, 1, 5, 5));

bandsPanel.add(paPanel);


Step 1: Store PA Select-All checkboxes

Add:

private final Map<String, JCheckBox> selectAllMap = new HashMap<>();
Step 2: Create Select All checkbox inside each PA panel

After creating paPanel:

JCheckBox selectAllBox = new JCheckBox("Select All");

selectAllMap.put(card.id, selectAllBox);

paPanel.add(selectAllBox);



Step 3: Connect Select All to individual band checkboxes

Suppose you already have:

List<JCheckBox> checkBoxes = new ArrayList<>();

and later:

checkBoxMap.put(card.id, checkBoxes);

After all module checkboxes are created:


selectAllBox.addActionListener(e -> {

    boolean selected = selectAllBox.isSelected();

    for (JCheckBox cb : checkBoxes) {

        if (cb.isEnabled()) {
            cb.setSelected(selected);
        }
    }
});


checkBox.addActionListener(e -> {

    boolean allSelected = true;

    for (JCheckBox cb : checkBoxes) {

        if (!cb.isSelected()) {
            allSelected = false;
            break;
        }
    }

    selectAllBox.setSelected(allSelected);
});



When creating Select All
JCheckBox selectAllBox = new JCheckBox("Select All");
selectAllBox.setEnabled(false);

selectAllMap.put(card.id, selectAllBox);


isConnected -> {

    updateCardStatus(card.id, isConnected);

    SwingUtilities.invokeLater(() -> {

        List<JCheckBox> boxes =
                checkBoxMap.get(card.id);

        if (boxes != null) {

            for (JCheckBox box : boxes) {
                box.setEnabled(isConnected);

                if (!isConnected) {
                    box.setSelected(false);
                }
            }
        }

        JCheckBox selectAllBox =
                selectAllMap.get(card.id);

        if (selectAllBox != null) {

            selectAllBox.setEnabled(isConnected);

            if (!isConnected) {
                selectAllBox.setSelected(false);
            }
        }
    });
}




Add ping failure counter

Inside class variables add:

private int pingFailCount = 0;


Update startPingChecker()

Find this code:

if (!reachable && socket != null && !socket.isClosed()) {
    System.out.println("Ping lost → closing socket");
    closeSocket();
}

Replace with:

if (reachable) {
    pingFailCount = 0;
} else {
    pingFailCount++;
    System.out.println(ip + " ping fail count = " + pingFailCount);
}

if (pingFailCount >= 3 &&
    socket != null &&
    !socket.isClosed()) {

    System.out.println(
        ip + " ping failed 3 times -> closing socket"
    );

    closeSocket();
}

This prevents disconnecting because of a single missed ping.

3. Update socket connection

Find inside run():

socket = new Socket();
socket.connect(new InetSocketAddress(ip, port), 2000);

Replace with:

socket = new Socket();
socket.connect(new InetSocketAddress(ip, port), 2000);

socket.setKeepAlive(true);
socket.setTcpNoDelay(true);

System.out.println(
    "CONNECTED -> " + ip + ":" + port
);
4. Update catch block

Find:

catch (Exception e) {
    System.out.println("Connection failed for: " + ip + ":" + port);

    connected.set(false);

    if (lastStatus && statusCallback != null) {
        statusCallback.accept(false);
        lastStatus = false;
    }

    closeSocket();

    try {
        Thread.sleep(5000);
    } catch (InterruptedException ignored) {
    }
}

Replace with:

catch (Exception e) {

    System.out.println(
        "DISCONNECTED -> " + ip + ":" + port
    );

    e.printStackTrace();

    connected.set(false);

    if (lastStatus && statusCallback != null) {
        statusCallback.accept(false);
        lastStatus = false;
    }

    closeSocket();

    try {
        Thread.sleep(2000);
    } catch (InterruptedException ignored) {
    }
}