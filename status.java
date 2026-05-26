// In ControlSystemUI constructor, replace the entire Control Card handlers section

// Control card handlers and UI
for (ControlCard card : allCards) {
    final String cardId = card.id;
    int startIndex = vswrList.size();
    
    // Create status label for this card
    JLabel label = new JLabel("CTRL: " + cardId);
    label.setOpaque(true);
    label.setBackground(Color.RED);
    label.setForeground(Color.WHITE);
    label.setPreferredSize(new Dimension(100, 60));
    label.setFont(new Font("Segoe UI", Font.BOLD, 14));
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);
    statusLabels.put(cardId, label);
    southPanel.add(label);
    
    List<JToggleButton> toggles = new ArrayList<>();
    
    // Check if this card has any special band modules
    boolean hasSpecialBand = false;
    for (Module m : card.modules) {
        if (isSpecialBand(m.name)) {
            hasSpecialBand = true;
            break;
        }
    }
    
    // ==================== IF BLOCK: SPECIAL BANDS (Dual Port) ====================
    if (hasSpecialBand) {
        System.out.println("Setting up SPECIAL band card: " + cardId + " with dual ports");
        
        // Create ASCII status handler on Port 10
        ASCIIStatusHandler statusHandler = new ASCIIStatusHandler(
            card.ip, 
            10,  // Port 10 for ASCII status updates
            isConnected -> {
                SwingUtilities.invokeLater(() -> {
                    JLabel lbl = statusLabels.get(cardId);
                    if (lbl != null) {
                        lbl.setBackground(isConnected ? Color.GREEN : Color.RED);
                    }
                    // Enable/disable toggles based on status connection
                    List<JToggleButton> btns = toggleMap.get(card.id);
                    if (btns != null) {
                        for (JToggleButton btn : btns) {
                            btn.setEnabled(isConnected);
                        }
                    }
                });
            },
            data -> {
                SwingUtilities.invokeLater(() -> {
                    for (int i = 0; i < card.modules.size(); i++) {
                        JTextField vswrField = vswrList.get(startIndex + i);
                        
                        if (!data.paOn) {
                            vswrField.setText("--");
                            vswrField.setBackground(Color.WHITE);
                            therList.get(startIndex + i).setText("--");
                            fwdList.get(startIndex + i).setText("--");
                            rsvList.get(startIndex + i).setText("--");
                            return;
                        }
                        
                        double vswr = data.vswr;
                        vswrField.setText(String.format("%.2f", vswr));
                        
                        if (vswr >= 3.5) {
                            vswrField.setBackground(Color.RED);
                            vswrField.setForeground(Color.WHITE);
                            vswrField.setToolTipText("VSWR CRITICAL");
                        } else if (vswr >= 2.5) {
                            vswrField.setBackground(Color.YELLOW);
                            vswrField.setForeground(Color.BLACK);
                            vswrField.setToolTipText("Warning");
                        } else {
                            vswrField.setBackground(Color.GREEN);
                            vswrField.setForeground(Color.BLACK);
                            vswrField.setToolTipText(null);
                        }
                        
                        therList.get(startIndex + i).setText(String.valueOf(data.temp));
                        fwdList.get(startIndex + i).setText(String.valueOf(data.fwd));
                        rsvList.get(startIndex + i).setText(String.valueOf(data.rfl));
                    }
                });
            }
        );
        statusHandler.start();
        asciiStatusHandlers.put(cardId, statusHandler);
        
        // Create control handler on Port 7
        ControlCardSocketHandler controlHandler = new ControlCardSocketHandler(
            card.ip, 
            7,   // Port 7 for control commands
            null,  // No status command needed
            isConnected -> {
                System.out.println("Control connection to " + cardId + " is " + isConnected);
            },
            null  // No data callback needed
        );
        controlHandler.start();
        controlHandlers.put(cardId, controlHandler);
        cardHandlers.put(cardId, controlHandler);
        
        // Create UI components for each module in this card
        for (Module m : card.modules) {
            JToggleButton toggle = new JToggleButton(m.name);
            toggle.setFont(new Font("Arial", Font.BOLD, 28));
            toggle.setForeground(Color.RED);
            toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
            toggle.setEnabled(false);
            
            toggle.addItemListener(e -> {
                if (!toggle.isEnabled()) return;
                
                ControlCardSocketHandler handler = controlHandlers.get(card.id);
                if (handler == null || !handler.isConnected()) return;
                
                boolean isOn = toggle.isSelected();
                toggle.setForeground(isOn ? Color.GREEN : Color.RED);
                
                // Send ASCII command through control port
                String command = isOn ? "CO.PS.S,1" : "CO.PS.S,0";
                handler.enqueueCommand(command.getBytes());
                
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String logMsg = time + " Band: " + m.name + (isOn ? " Powered ON" : " Powered OFF");
                addLog(logMsg);
            });
            
            panel_3.add(toggle);
            toggles.add(toggle);
            
            // Add to frequency band panel
            JLabel bandsLabel = new JLabel(m.name);
            bandsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            bandsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            freqband_names_panel.add(bandsLabel);
            
            // LAN checkboxes
            JCheckBox lanBox = new JCheckBox("");
            lanBox.setHorizontalAlignment(SwingConstants.CENTER);
            lanBox.setEnabled(false);
            lan_checkbox_panel.add(lanBox);
            
            // VSWR fields
            JTextField vswrBox = new JTextField("--");
            vswrBox.setHorizontalAlignment(SwingConstants.CENTER);
            vswrBox.setEditable(false);
            vswrBox.setBackground(Color.WHITE);
            vswr_checkbox_panel.add(vswrBox);
            vswrList.add(vswrBox);
            
            // Temperature fields
            JTextField therBox = new JTextField("--");
            therBox.setHorizontalAlignment(SwingConstants.CENTER);
            therBox.setEditable(false);
            therBox.setBackground(Color.WHITE);
            ther_checkbox_panel.add(therBox);
            therList.add(therBox);
            
            // Forward power fields
            JTextField fwdField = new JTextField("--");
            fwdField.setHorizontalAlignment(SwingConstants.CENTER);
            fwdField.setEditable(false);
            fwdField.setBackground(Color.WHITE);
            fwd_textfields_panel.add(fwdField);
            fwdList.add(fwdField);
            
            // Reverse power fields
            JTextField rflField = new JTextField("--");
            rflField.setHorizontalAlignment(SwingConstants.CENTER);
            rflField.setEditable(false);
            rflField.setBackground(Color.WHITE);
            rsv_textfields_panel.add(rflField);
            rsvList.add(rflField);
        }
        
    } 
    // ==================== ELSE BLOCK: NORMAL BANDS (Single Port) ====================
    else {
        System.out.println("Setting up NORMAL band card: " + cardId + " with single port");
        
        // Create array for LAN checkboxes
        ArrayList<JCheckBox> temp = new ArrayList<>();
        
        // Create the socket handler (single port for both status and control)
        ControlCardSocketHandler handler = new ControlCardSocketHandler(
            card.ip, 
            card.port,
            hexStringToBytes(card.status_command), 
            isConnected -> {
                updateCardStatus(cardId, isConnected);
                List<JToggleButton> btns = toggleMap.get(card.id);
                if (btns != null) {
                    for (JToggleButton btn : btns) {
                        btn.setEnabled(isConnected);
                        // Send OFF command when disconnected to ensure clean state
                        if (!isConnected) {
                            btn.setSelected(false);
                            btn.setForeground(Color.RED);
                        }
                    }
                }
            }, 
            data -> {
                SwingUtilities.invokeLater(() -> {
                    for (int i = 0; i < card.modules.size(); i++) {
                        JTextField vswrField = vswrList.get(startIndex + i);
                        
                        if (!data.paOn) {
                            vswrField.setText("--");
                            vswrField.setBackground(Color.WHITE);
                            vswrField.setForeground(Color.BLACK);
                            vswrField.setToolTipText(null);
                            
                            therList.get(startIndex + i).setText("--");
                            fwdList.get(startIndex + i).setText("--");
                            rsvList.get(startIndex + i).setText("--");
                            continue;
                        }
                        
                        double vswr = data.vswr;
                        vswrField.setText(String.valueOf(vswr));
                        
                        if (vswr >= 3.5) {
                            vswrField.setBackground(Color.RED);
                            vswrField.setForeground(Color.WHITE);
                            vswrField.setToolTipText("VSWR CRITICAL");
                        } else if (vswr >= 2.5) {
                            vswrField.setBackground(Color.YELLOW);
                            vswrField.setForeground(Color.BLACK);
                            vswrField.setToolTipText("Warning");
                        } else {
                            vswrField.setBackground(Color.GREEN);
                            vswrField.setForeground(Color.BLACK);
                            vswrField.setToolTipText(null);
                        }
                        
                        therList.get(startIndex + i).setText(String.valueOf(data.temp));
                        fwdList.get(startIndex + i).setText(String.valueOf(data.fwd));
                        rsvList.get(startIndex + i).setText(String.valueOf(data.rfl));
                    }
                });
            }
        );
        
        handler.setLans(temp);
        Thread t = new Thread(handler);
        t.start();
        cardHandlers.put(card.id, handler);
        
        // Create UI components for each module in this card
        for (int moduleIndex = 0; moduleIndex < card.modules.size(); moduleIndex++) {
            Module m = card.modules.get(moduleIndex);
            
            JToggleButton toggle = new JToggleButton(m.name);
            toggle.setFont(new Font("Arial", Font.BOLD, 28));
            toggle.setForeground(Color.RED);
            toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (!m.name.equalsIgnoreCase("")) {
                toggle.setEnabled(false);
            }
            
            final ControlCardSocketHandler finalHandler = handler;
            final int finalModuleIndex = moduleIndex;
            
            toggle.addItemListener(e -> {
                if (!toggle.isEnabled())
                    return;
                
                if (finalHandler == null || !finalHandler.isConnected())
                    return;
                
                boolean isOn = toggle.isSelected();
                String command = isOn ? m.power_on : m.power_off;
                
                toggle.setForeground(isOn ? Color.GREEN : Color.RED);
                
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String logMsg = time + " Band: " + m.name + (isOn ? " Powered ON" : " Powered OFF");
                addLog(logMsg);
                
                // Check if this specific module is a special band (though card already marked as non-special)
                if(isSpecialBand(m.name)) {
                    splitCmd(command, finalHandler);
                } else {
                    finalHandler.enqueueCommand(hexStringToBytes(command));
                }
            });
            
            panel_3.add(toggle);
            toggles.add(toggle);
            
            // Add to frequency band panel
            JLabel bandsLabel = new JLabel(m.name);
            bandsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
            bandsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            freqband_names_panel.add(bandsLabel);
            
            // LAN checkboxes
            JCheckBox lanBox = new JCheckBox("");
            temp.add(lanBox);
            lanBox.setHorizontalAlignment(SwingConstants.CENTER);
            lanBox.setEnabled(false);
            lan_checkbox_panel.add(lanBox);
            
            // VSWR fields
            JTextField vswrBox = new JTextField("--");
            vswrBox.setHorizontalAlignment(SwingConstants.CENTER);
            vswrBox.setEditable(false);
            vswrBox.setBackground(Color.WHITE);
            vswr_checkbox_panel.add(vswrBox);
            vswrList.add(vswrBox);
            
            // Temperature fields
            JTextField therBox = new JTextField("--");
            therBox.setHorizontalAlignment(SwingConstants.CENTER);
            therBox.setEditable(false);
            therBox.setBackground(Color.WHITE);
            ther_checkbox_panel.add(therBox);
            therList.add(therBox);
            
            // Forward power fields
            JTextField fwdField = new JTextField("--");
            fwdField.setHorizontalAlignment(SwingConstants.CENTER);
            fwdField.setEditable(false);
            fwdField.setBackground(Color.WHITE);
            fwd_textfields_panel.add(fwdField);
            fwdList.add(fwdField);
            
            // Reverse power fields
            JTextField rflField = new JTextField("--");
            rflField.setHorizontalAlignment(SwingConstants.CENTER);
            rflField.setEditable(false);
            rflField.setBackground(Color.WHITE);
            rsv_textfields_panel.add(rflField);
            rsvList.add(rflField);
        }
    }
    
    toggleMap.put(card.id, toggles);
    Thread.sleep(10);
}

----------------------------
// ==================== IF BLOCK: SPECIAL BANDS (Dual Port) ====================
if (hasSpecialBand) {
    System.out.println("Setting up SPECIAL band card: " + cardId + " with dual ports");
    
    // Create ArrayList for LAN checkboxes BEFORE creating the status handler
    ArrayList<JCheckBox> tempLanBoxes = new ArrayList<>();
    
    // Create UI components for each module in this card FIRST
    for (Module m : card.modules) {
        JToggleButton toggle = new JToggleButton(m.name);
        toggle.setFont(new Font("Arial", Font.BOLD, 28));
        toggle.setForeground(Color.RED);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setEnabled(false);
        
        toggle.addItemListener(e -> {
            if (!toggle.isEnabled()) return;
            
            ControlCardSocketHandler handler = controlHandlers.get(card.id);
            if (handler == null || !handler.isConnected()) return;
            
            boolean isOn = toggle.isSelected();
            toggle.setForeground(isOn ? Color.GREEN : Color.RED);
            
            // Send ASCII command through control port
            String command = isOn ? "CO.PS.S,1" : "CO.PS.S,0";
            handler.enqueueCommand(command.getBytes());
            
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logMsg = time + " Band: " + m.name + (isOn ? " Powered ON" : " Powered OFF");
            addLog(logMsg);
        });
        
        panel_3.add(toggle);
        toggles.add(toggle);
        
        // Add to frequency band panel
        JLabel bandsLabel = new JLabel(m.name);
        bandsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bandsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        freqband_names_panel.add(bandsLabel);
        
        // LAN checkboxes - add to temp list
        JCheckBox lanBox = new JCheckBox("");
        lanBox.setHorizontalAlignment(SwingConstants.CENTER);
        lanBox.setEnabled(false);
        lan_checkbox_panel.add(lanBox);
        tempLanBoxes.add(lanBox);
        
        // VSWR fields
        JTextField vswrBox = new JTextField("--");
        vswrBox.setHorizontalAlignment(SwingConstants.CENTER);
        vswrBox.setEditable(false);
        vswrBox.setBackground(Color.WHITE);
        vswr_checkbox_panel.add(vswrBox);
        vswrList.add(vswrBox);
        
        // Temperature fields
        JTextField therBox = new JTextField("--");
        therBox.setHorizontalAlignment(SwingConstants.CENTER);
        therBox.setEditable(false);
        therBox.setBackground(Color.WHITE);
        ther_checkbox_panel.add(therBox);
        therList.add(therBox);
        
        // Forward power fields
        JTextField fwdField = new JTextField("--");
        fwdField.setHorizontalAlignment(SwingConstants.CENTER);
        fwdField.setEditable(false);
        fwdField.setBackground(Color.WHITE);
        fwd_textfields_panel.add(fwdField);
        fwdList.add(fwdField);
        
        // Reverse power fields
        JTextField rflField = new JTextField("--");
        rflField.setHorizontalAlignment(SwingConstants.CENTER);
        rflField.setEditable(false);
        rflField.setBackground(Color.WHITE);
        rsv_textfields_panel.add(rflField);
        rsvList.add(rflField);
    }
    
    // Now create ASCII status handler on Port 10 with the LAN checkboxes
    ASCIIStatusHandler statusHandler = new ASCIIStatusHandler(
        card.ip, 
        10,  // Port 10 for ASCII status updates
        isConnected -> {
            SwingUtilities.invokeLater(() -> {
                JLabel lbl = statusLabels.get(cardId);
                if (lbl != null) {
                    lbl.setBackground(isConnected ? Color.GREEN : Color.RED);
                }
                
                // Update LAN checkbox colors based on connection status
                for (JCheckBox lanBox : tempLanBoxes) {
                    lanBox.setIcon(new ColoredBoxIcon(isConnected ? Color.GREEN : Color.RED));
                    lanBox.setOpaque(true);
                    lanBox.revalidate();
                    lanBox.repaint();
                }
                
                // Enable/disable toggles based on status connection
                List<JToggleButton> btns = toggleMap.get(card.id);
                if (btns != null) {
                    for (JToggleButton btn : btns) {
                        btn.setEnabled(isConnected);
                    }
                }
            });
        },
        data -> {
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < card.modules.size(); i++) {
                    JTextField vswrField = vswrList.get(startIndex + i);
                    
                    if (!data.paOn) {
                        vswrField.setText("--");
                        vswrField.setBackground(Color.WHITE);
                        vswrField.setForeground(Color.BLACK);
                        vswrField.setToolTipText(null);
                        
                        therList.get(startIndex + i).setText("--");
                        fwdList.get(startIndex + i).setText("--");
                        rsvList.get(startIndex + i).setText("--");
                        continue;
                    }
                    
                    double vswr = data.vswr;
                    vswrField.setText(String.format("%.2f", vswr));
                    
                    if (vswr >= 3.5) {
                        vswrField.setBackground(Color.RED);
                        vswrField.setForeground(Color.WHITE);
                        vswrField.setToolTipText("VSWR CRITICAL");
                    } else if (vswr >= 2.5) {
                        vswrField.setBackground(Color.YELLOW);
                        vswrField.setForeground(Color.BLACK);
                        vswrField.setToolTipText("Warning");
                    } else {
                        vswrField.setBackground(Color.GREEN);
                        vswrField.setForeground(Color.BLACK);
                        vswrField.setToolTipText(null);
                    }
                    
                    therList.get(startIndex + i).setText(String.valueOf(data.temp));
                    fwdList.get(startIndex + i).setText(String.valueOf(data.fwd));
                    rsvList.get(startIndex + i).setText(String.valueOf(data.rfl));
                }
            });
        }
    );
    statusHandler.start();
    asciiStatusHandlers.put(cardId, statusHandler);
    
    // Create control handler on Port 7
    ControlCardSocketHandler controlHandler = new ControlCardSocketHandler(
        card.ip, 
        7,   // Port 7 for control commands
        null,  // No status command needed
        isConnected -> {
            System.out.println("Control connection to " + cardId + " is " + isConnected);
        },
        null  // No data callback needed
    );
    controlHandler.start();
    controlHandlers.put(cardId, controlHandler);
    cardHandlers.put(cardId, controlHandler);
}

public class ASCIIStatusHandler implements Runnable {
    private final String ip;
    private final int port;
    private volatile HardwareData lastData;
    private volatile boolean running = true;
    private volatile boolean connected = false;
    private Socket socket;
    private BufferedReader reader;
    private final Consumer<Boolean> statusCallback;
    private final Consumer<HardwareData> dataCallback;
    private ArrayList<JCheckBox> lans; // Add this field

    public ASCIIStatusHandler(String ip, int port, Consumer<Boolean> statusCallback, 
                              Consumer<HardwareData> dataCallback) {
        this.ip = ip;
        this.port = port;
        this.statusCallback = statusCallback;
        this.dataCallback = dataCallback;
    }
    
    // Add this method to set LAN checkboxes
    public void setLans(ArrayList<JCheckBox> lans) {
        this.lans = lans;
    }

    public void start() {
        new Thread(this).start();
    }

    public void shutdown() {
        running = false;
        closeSocket();
    }

    public boolean isConnected() {
        return connected;
    }

    public HardwareData getLastData() {
        return lastData;
    }
    
    private void updateLanStatus(boolean connected) {
        if (lans != null) {
            SwingUtilities.invokeLater(() -> {
                for (JCheckBox lanBox : lans) {
                    lanBox.setIcon(new ColoredBoxIcon(connected ? Color.GREEN : Color.RED));
                    lanBox.setOpaque(true);
                    lanBox.revalidate();
                    lanBox.repaint();
                }
            });
        }
    }

    @Override
    public void run() {
        boolean lastStatus = false;
        
        while (running) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 2000);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                connected = true;
                updateLanStatus(true); // Update LAN when connected
                System.out.println("ASCII Status connected to " + ip + ":" + port);
                
                if (!lastStatus && statusCallback != null) {
                    statusCallback.accept(true);
                    lastStatus = true;
                }
                
                // Read ASCII lines (received every 300ms)
                String line;
                while (running && !socket.isClosed() && (line = reader.readLine()) != null) {
                    if (line.startsWith("CO.HL.G")) {
                        HardwareData data = parseASCIIStatus(line);
                        this.lastData = data;
                        if (dataCallback != null) {
                            dataCallback.accept(data);
                        }
                        System.out.println("Status: " + line);
                    }
                }
                
            } catch (Exception e) {
                System.out.println("ASCII Status connection lost: " + ip + ":" + port);
                connected = false;
                updateLanStatus(false); // Update LAN when disconnected
                
                if (lastStatus && statusCallback != null) {
                    statusCallback.accept(false);
                    lastStatus = false;
                }
                
                closeSocket();
                
                try {
                    Thread.sleep(5000); // Reconnect every 5 seconds
                } catch (InterruptedException ignored) {}
            }
        }
    }
    
    private HardwareData parseASCIIStatus(String statusLine) {
        // Format: CO.HL.G,PAOnOff,vswr_alarm,temp_alarm,FwdPower,RefPower
        // Example: CO.HL.G,1,0,0,50.00,30.00
        HardwareData data = new HardwareData();
        
        try {
            String[] parts = statusLine.split(",");
            if (parts.length >= 6) {
                int paOnOff = Integer.parseInt(parts[1].trim());
                data.paOn = (paOnOff == 1);
                
                int vswrAlarm = Integer.parseInt(parts[2].trim());
                int tempAlarm = Integer.parseInt(parts[3].trim());
                
                data.fwd = (int) Double.parseDouble(parts[4].trim());
                data.rfl = (int) Double.parseDouble(parts[5].trim());
                
                // Calculate VSWR from forward and reflected power
                if (data.rfl > 0 && data.fwd > 0) {
                    double sqrtReflected = Math.sqrt(data.rfl);
                    double sqrtForward = Math.sqrt(data.fwd);
                    data.vswr = (sqrtForward + sqrtReflected) / (sqrtForward - sqrtReflected);
                    if (data.vswr < 1.0) data.vswr = 1.0;
                } else {
                    data.vswr = 1.0;
                }
                
                // Temperature from alarm (you might need to map this properly)
                data.temp = tempAlarm; // Placeholder - adjust based on your actual temp data
            }
        } catch (Exception e) {
            System.out.println("Error parsing status: " + statusLine);
        }
        
        return data;
    }
    
    private void closeSocket() {
        try {
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}

// After creating statusHandler, set the LAN checkboxes
ASCIIStatusHandler statusHandler = new ASCIIStatusHandler(...);
statusHandler.setLans(tempLanBoxes); // Add this line
statusHandler.start();

This should fix the LAN status updates for special bands. The LAN checkboxes will now show:

Green when connected to port 10

Red when disconnected from port 10

2. Fix the ASCIIStatusHandler to properly parse the status format:

public class ASCIIStatusHandler implements Runnable {
    private final String ip;
    private final int port;
    private volatile HardwareData lastData;
    private volatile boolean running = true;
    private volatile boolean connected = false;
    private Socket socket;
    private BufferedReader reader;
    private final Consumer<Boolean> statusCallback;
    private final Consumer<HardwareData> dataCallback;
    private ArrayList<JCheckBox> lans;
    private String cardId; // Add card ID for debugging

    public ASCIIStatusHandler(String ip, int port, Consumer<Boolean> statusCallback, 
                              Consumer<HardwareData> dataCallback, String cardId) {
        this.ip = ip;
        this.port = port;
        this.statusCallback = statusCallback;
        this.dataCallback = dataCallback;
        this.cardId = cardId;
    }
    
    public void setLans(ArrayList<JCheckBox> lans) {
        this.lans = lans;
    }

    public void start() {
        new Thread(this).start();
    }

    public void shutdown() {
        running = false;
        closeSocket();
    }

    public boolean isConnected() {
        return connected;
    }

    public HardwareData getLastData() {
        return lastData;
    }
    
    private void updateLanStatus(boolean connected) {
        if (lans != null) {
            SwingUtilities.invokeLater(() -> {
                for (JCheckBox lanBox : lans) {
                    lanBox.setIcon(new ColoredBoxIcon(connected ? Color.GREEN : Color.RED));
                    lanBox.setOpaque(true);
                    lanBox.revalidate();
                    lanBox.repaint();
                }
            });
        }
    }

    @Override
    public void run() {
        boolean lastStatus = false;
        
        while (running) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 2000);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                connected = true;
                updateLanStatus(true);
                System.out.println("ASCII Status connected to " + ip + ":" + port + " for " + cardId);
                
                if (!lastStatus && statusCallback != null) {
                    statusCallback.accept(true);
                    lastStatus = true;
                }
                
                // Read ASCII lines
                String line;
                while (running && !socket.isClosed() && (line = reader.readLine()) != null) {
                    System.out.println("Received from " + cardId + ": " + line);
                    
                    if (line.startsWith("CO.HL.G")) {
                        HardwareData data = parseASCIIStatus(line);
                        this.lastData = data;
                        if (dataCallback != null) {
                            dataCallback.accept(data);
                        }
                    }
                }
                
            } catch (Exception e) {
                System.out.println("ASCII Status connection lost for " + cardId + ": " + ip + ":" + port);
                connected = false;
                updateLanStatus(false);
                
                if (lastStatus && statusCallback != null) {
                    statusCallback.accept(false);
                    lastStatus = false;
                }
                
                closeSocket();
                
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
    
    private HardwareData parseASCIIStatus(String statusLine) {
        // Format: CO.HL.G,PAOnOff,VSWR_Alarm,Temp_Alarm,FwdPower,RefPower,Temperature
        // Example: CO.HL.G,0,0,0,19,28,25.8
        HardwareData data = new HardwareData();
        
        try {
            String[] parts = statusLine.split(",");
            if (parts.length >= 7) {
                int paOnOff = Integer.parseInt(parts[1].trim());
                data.paOn = (paOnOff == 1);
                
                int vswrAlarm = Integer.parseInt(parts[2].trim());
                int tempAlarm = Integer.parseInt(parts[3].trim());
                
                data.fwd = (int) Double.parseDouble(parts[4].trim());
                data.rfl = (int) Double.parseDouble(parts[5].trim());
                data.temp = (int) Double.parseDouble(parts[6].trim());
                
                // Calculate VSWR from forward and reflected power if available
                if (data.rfl > 0 && data.fwd > 0) {
                    double vswr = (1 + Math.sqrt(data.rfl / data.fwd)) / (1 - Math.sqrt(data.rfl / data.fwd));
                    data.vswr = Math.round(vswr * 100.0) / 100.0;
                } else if (vswrAlarm > 0) {
                    data.vswr = 3.0; // Default high VSWR
                } else {
                    data.vswr = 1.0;
                }
                
                System.out.println("Parsed data for " + cardId + " - PA On: " + data.paOn + 
                                 ", FWD: " + data.fwd + ", RFL: " + data.rfl + 
                                 ", VSWR: " + data.vswr + ", Temp: " + data.temp);
            }
        } catch (Exception e) {
            System.out.println("Error parsing status for " + cardId + ": " + statusLine);
            e.printStackTrace();
        }
        
        return data;
    }
    
    private void closeSocket() {
        try {
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}

3. Fix the ControlCardSocketHandler to properly send ASCII commands:
Update the enqueueCommand method in ControlCardSocketHandler:

public void enqueueCommand(byte[] command) {
    if (command != null && connected.get()) {
        sendQueue.offer(command);
    }
}

// Add this new method for sending ASCII strings directly
public void enqueueASCIICommand(String command) {
    if (command != null && connected.get()) {
        byte[] cmdBytes = command.getBytes(StandardCharsets.US_ASCII);
        sendQueue.offer(cmdBytes);
        System.out.println("Enqueued ASCII command: " + command + " -> " + bytesToHex(cmdBytes));
    }
}

4. Fix the IF block for special bands in ControlSystemUI:
// ==================== IF BLOCK: SPECIAL BANDS (Dual Port) ====================
if (hasSpecialBand) {
    System.out.println("Setting up SPECIAL band card: " + cardId + " on IP: " + card.ip);
    
    // Create ArrayList for LAN checkboxes
    ArrayList<JCheckBox> tempLanBoxes = new ArrayList<>();
    
    // Create UI components for each module in this card FIRST
    for (Module m : card.modules) {
        JToggleButton toggle = new JToggleButton(m.name);
        toggle.setFont(new Font("Arial", Font.BOLD, 28));
        toggle.setForeground(Color.RED);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setEnabled(false);
        
        toggle.addItemListener(e -> {
            if (!toggle.isEnabled()) return;
            
            ControlCardSocketHandler handler = controlHandlers.get(card.id);
            if (handler == null || !handler.isConnected()) {
                System.out.println("Control handler not connected for " + card.id);
                return;
            }
            
            boolean isOn = toggle.isSelected();
            toggle.setForeground(isOn ? Color.GREEN : Color.RED);
            
            // Send ASCII command directly as string, not hex
            String command = isOn ? "CO.PS.S,1\r\n" : "CO.PS.S,0\r\n";
            handler.enqueueASCIICommand(command);
            
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logMsg = time + " Band: " + m.name + (isOn ? " Powered ON" : " Powered OFF");
            addLog(logMsg);
            System.out.println("Sent command to " + cardId + ": " + command);
        });
        
        panel_3.add(toggle);
        toggles.add(toggle);
        
        // Add to frequency band panel
        JLabel bandsLabel = new JLabel(m.name);
        bandsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bandsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        freqband_names_panel.add(bandsLabel);
        
        // LAN checkboxes
        JCheckBox lanBox = new JCheckBox("");
        lanBox.setHorizontalAlignment(SwingConstants.CENTER);
        lanBox.setEnabled(false);
        lan_checkbox_panel.add(lanBox);
        tempLanBoxes.add(lanBox);
        
        // VSWR fields
        JTextField vswrBox = new JTextField("--");
        vswrBox.setHorizontalAlignment(SwingConstants.CENTER);
        vswrBox.setEditable(false);
        vswrBox.setBackground(Color.WHITE);
        vswr_checkbox_panel.add(vswrBox);
        vswrList.add(vswrBox);
        
        // Temperature fields
        JTextField therBox = new JTextField("--");
        therBox.setHorizontalAlignment(SwingConstants.CENTER);
        therBox.setEditable(false);
        therBox.setBackground(Color.WHITE);
        ther_checkbox_panel.add(therBox);
        therList.add(therBox);
        
        // Forward power fields
        JTextField fwdField = new JTextField("--");
        fwdField.setHorizontalAlignment(SwingConstants.CENTER);
        fwdField.setEditable(false);
        fwdField.setBackground(Color.WHITE);
        fwd_textfields_panel.add(fwdField);
        fwdList.add(fwdField);
        
        // Reverse power fields
        JTextField rflField = new JTextField("--");
        rflField.setHorizontalAlignment(SwingConstants.CENTER);
        rflField.setEditable(false);
        rflField.setBackground(Color.WHITE);
        rsv_textfields_panel.add(rflField);
        rsvList.add(rflField);
    }
    
    // Create ASCII status handler on Port 10 (NOT 20108)
    ASCIIStatusHandler statusHandler = new ASCIIStatusHandler(
        card.ip, 
        10,  // Port 10 for ASCII status updates
        isConnected -> {
            SwingUtilities.invokeLater(() -> {
                JLabel lbl = statusLabels.get(cardId);
                if (lbl != null) {
                    lbl.setBackground(isConnected ? Color.GREEN : Color.RED);
                }
                
                // Update LAN checkbox colors
                for (JCheckBox lanBox : tempLanBoxes) {
                    lanBox.setIcon(new ColoredBoxIcon(isConnected ? Color.GREEN : Color.RED));
                    lanBox.setOpaque(true);
                    lanBox.revalidate();
                    lanBox.repaint();
                }
                
                // Enable/disable toggles based on connection
                List<JToggleButton> btns = toggleMap.get(card.id);
                if (btns != null) {
                    for (JToggleButton btn : btns) {
                        btn.setEnabled(isConnected);
                    }
                }
            });
        },
        data -> {
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < card.modules.size(); i++) {
                    JTextField vswrField = vswrList.get(startIndex + i);
                    
                    if (!data.paOn) {
                        vswrField.setText("--");
                        vswrField.setBackground(Color.WHITE);
                        vswrField.setForeground(Color.BLACK);
                        vswrField.setToolTipText(null);
                        
                        therList.get(startIndex + i).setText("--");
                        fwdList.get(startIndex + i).setText("--");
                        rsvList.get(startIndex + i).setText("--");
                        continue;
                    }
                    
                    double vswr = data.vswr;
                    vswrField.setText(String.format("%.2f", vswr));
                    
                    if (vswr >= 3.5) {
                        vswrField.setBackground(Color.RED);
                        vswrField.setForeground(Color.WHITE);
                        vswrField.setToolTipText("VSWR CRITICAL");
                    } else if (vswr >= 2.5) {
                        vswrField.setBackground(Color.YELLOW);
                        vswrField.setForeground(Color.BLACK);
                        vswrField.setToolTipText("Warning");
                    } else {
                        vswrField.setBackground(Color.GREEN);
                        vswrField.setForeground(Color.BLACK);
                        vswrField.setToolTipText(null);
                    }
                    
                    therList.get(startIndex + i).setText(String.valueOf(data.temp));
                    fwdList.get(startIndex + i).setText(String.valueOf(data.fwd));
                    rsvList.get(startIndex + i).setText(String.valueOf(data.rfl));
                }
            });
        },
        cardId
    );
    
    statusHandler.setLans(tempLanBoxes);
    statusHandler.start();
    asciiStatusHandlers.put(cardId, statusHandler);
    
    // Create control handler on Port 7
    ControlCardSocketHandler controlHandler = new ControlCardSocketHandler(
        card.ip, 
        7,   // Port 7 for control commands
        null,
        isConnected -> {
            System.out.println("Control connection to " + cardId + " is " + isConnected);
        },
        null
    );
    controlHandler.start();
    controlHandlers.put(cardId, controlHandler);
    cardHandlers.put(cardId, controlHandler);
}

5. Add the enqueueASCIICommand method to ControlCardSocketHandler:
import java.nio.charset.StandardCharsets;Add this import at the top of ControlCardSocketHandler:

Then add this method:

public void enqueueASCIICommand(String command) {
    if (command != null && connected.get()) {
        byte[] cmdBytes = command.getBytes(StandardCharsets.US_ASCII);
        sendQueue.offer(cmdBytes);
        System.out.println("Enqueued ASCII: " + command);
    }
}

----------------------------------
1. First, update the ControlCardSocketHandler to properly send ASCII commands:
Add this method to ControlCardSocketHandler:

public void enqueueASCIICommand(String command) {
    if (command != null && connected.get()) {
        byte[] cmdBytes = command.getBytes(StandardCharsets.US_ASCII);
        sendQueue.offer(cmdBytes);
        System.out.println("Sent ASCII command: " + command + " (hex: " + bytesToHex(cmdBytes) + ")");
    }
}

2. Update the IF block for special bands to use correct ports and commands:
// ==================== IF BLOCK: SPECIAL BANDS (Dual Port) ====================
if (hasSpecialBand) {
    System.out.println("Setting up SPECIAL band card: " + cardId + " on IP: " + card.ip);
    
    // Create ArrayList for LAN checkboxes
    ArrayList<JCheckBox> tempLanBoxes = new ArrayList<>();
    
    // Create UI components for each module in this card
    for (Module m : card.modules) {
        JToggleButton toggle = new JToggleButton(m.name);
        toggle.setFont(new Font("Arial", Font.BOLD, 28));
        toggle.setForeground(Color.RED);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setEnabled(false);
        
        toggle.addItemListener(e -> {
            if (!toggle.isEnabled()) return;
            
            ControlCardSocketHandler handler = controlHandlers.get(card.id);
            if (handler == null || !handler.isConnected()) {
                System.out.println("Control handler not connected for " + card.id);
                return;
            }
            
            boolean isOn = toggle.isSelected();
            toggle.setForeground(isOn ? Color.GREEN : Color.RED);
            
            // Send ASCII command - IMPORTANT: Use \r\n as line terminator
            String command = isOn ? "CO.PS.S,1\r\n" : "CO.PS.S,0\r\n";
            handler.enqueueASCIICommand(command);
            
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logMsg = time + " Band: " + m.name + (isOn ? " Powered ON" : " Powered OFF");
            addLog(logMsg);
        });
        
        panel_3.add(toggle);
        toggles.add(toggle);
        
        // Add to frequency band panel
        JLabel bandsLabel = new JLabel(m.name);
        bandsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bandsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        freqband_names_panel.add(bandsLabel);
        
        // LAN checkboxes
        JCheckBox lanBox = new JCheckBox("");
        lanBox.setHorizontalAlignment(SwingConstants.CENTER);
        lanBox.setEnabled(false);
        lan_checkbox_panel.add(lanBox);
        tempLanBoxes.add(lanBox);
        
        // VSWR fields
        JTextField vswrBox = new JTextField("--");
        vswrBox.setHorizontalAlignment(SwingConstants.CENTER);
        vswrBox.setEditable(false);
        vswrBox.setBackground(Color.WHITE);
        vswr_checkbox_panel.add(vswrBox);
        vswrList.add(vswrBox);
        
        // Temperature fields
        JTextField therBox = new JTextField("--");
        therBox.setHorizontalAlignment(SwingConstants.CENTER);
        therBox.setEditable(false);
        therBox.setBackground(Color.WHITE);
        ther_checkbox_panel.add(therBox);
        therList.add(therBox);
        
        // Forward power fields
        JTextField fwdField = new JTextField("--");
        fwdField.setHorizontalAlignment(SwingConstants.CENTER);
        fwdField.setEditable(false);
        fwdField.setBackground(Color.WHITE);
        fwd_textfields_panel.add(fwdField);
        fwdList.add(fwdField);
        
        // Reverse power fields
        JTextField rflField = new JTextField("--");
        rflField.setHorizontalAlignment(SwingConstants.CENTER);
        rflField.setEditable(false);
        rflField.setBackground(Color.WHITE);
        rsv_textfields_panel.add(rflField);
        rsvList.add(rflField);
    }
    
    // Create ASCII status handler on Port 20108 (to enable feedback)
    ASCIIStatusHandler statusHandler = new ASCIIStatusHandler(
        card.ip, 
        20108,  // Port 20108 for ASCII status updates
        isConnected -> {
            SwingUtilities.invokeLater(() -> {
                JLabel lbl = statusLabels.get(cardId);
                if (lbl != null) {
                    lbl.setBackground(isConnected ? Color.GREEN : Color.RED);
                }
                
                // Update LAN checkbox colors
                for (JCheckBox lanBox : tempLanBoxes) {
                    lanBox.setIcon(new ColoredBoxIcon(isConnected ? Color.GREEN : Color.RED));
                    lanBox.setOpaque(true);
                    lanBox.revalidate();
                    lanBox.repaint();
                }
                
                // Enable/disable toggles based on connection
                List<JToggleButton> btns = toggleMap.get(card.id);
                if (btns != null) {
                    for (JToggleButton btn : btns) {
                        btn.setEnabled(isConnected);
                    }
                }
            });
        },
        data -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Updating UI for " + cardId + " - PA On: " + data.paOn);
                
                for (int i = 0; i < card.modules.size(); i++) {
                    JTextField vswrField = vswrList.get(startIndex + i);
                    
                    if (!data.paOn) {
                        // PA is OFF - show default values
                        vswrField.setText("--");
                        vswrField.setBackground(Color.WHITE);
                        vswrField.setForeground(Color.BLACK);
                        vswrField.setToolTipText("PA is OFF");
                        
                        therList.get(startIndex + i).setText("--");
                        fwdList.get(startIndex + i).setText("--");
                        rsvList.get(startIndex + i).setText("--");
                    } else {
                        // PA is ON - show actual values
                        double vswr = data.vswr;
                        vswrField.setText(String.format("%.2f", vswr));
                        
                        if (vswr >= 3.5) {
                            vswrField.setBackground(Color.RED);
                            vswrField.setForeground(Color.WHITE);
                            vswrField.setToolTipText("VSWR CRITICAL");
                        } else if (vswr >= 2.5) {
                            vswrField.setBackground(Color.YELLOW);
                            vswrField.setForeground(Color.BLACK);
                            vswrField.setToolTipText("Warning");
                        } else {
                            vswrField.setBackground(Color.GREEN);
                            vswrField.setForeground(Color.BLACK);
                            vswrField.setToolTipText(null);
                        }
                        
                        therList.get(startIndex + i).setText(String.valueOf(data.temp) + "°C");
                        fwdList.get(startIndex + i).setText(String.valueOf(data.fwd) + "W");
                        rsvList.get(startIndex + i).setText(String.valueOf(data.rfl) + "W");
                    }
                }
            });
        },
        cardId
    );
    
    statusHandler.setLans(tempLanBoxes);
    statusHandler.start();
    asciiStatusHandlers.put(cardId, statusHandler);
    
    // Small delay to ensure status connection is established first
    try {
        Thread.sleep(500);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    
    // Create control handler on Port 7
    ControlCardSocketHandler controlHandler = new ControlCardSocketHandler(
        card.ip, 
        7,   // Port 7 for control commands
        null,
        isConnected -> {
            System.out.println("Control connection to " + cardId + " is " + isConnected);
        },
        null
    );
    controlHandler.start();
    controlHandlers.put(cardId, controlHandler);
    cardHandlers.put(cardId, controlHandler);
}

3. Update the ASCIIStatusHandler to properly parse and update:
private HardwareData parseASCIIStatus(String statusLine) {
    // Format: CO.HL.G,PAOnOff,VSWR_Alarm,Temp_Alarm,FwdPower,RefPower,Temperature
    // Example when OFF: CO.HL.G,0,0,0,19,28,28.6
    // Example when ON:  CO.HL.G,1,0,0,19,48,37.1
    HardwareData data = new HardwareData();
    
    try {
        // Remove any carriage return or newline
        statusLine = statusLine.trim();
        String[] parts = statusLine.split(",");
        
        if (parts.length >= 7) {
            int paOnOff = Integer.parseInt(parts[1].trim());
            data.paOn = (paOnOff == 1);
            
            int vswrAlarm = Integer.parseInt(parts[2].trim());
            int tempAlarm = Integer.parseInt(parts[3].trim());
            
            data.fwd = (int) Double.parseDouble(parts[4].trim());
            data.rfl = (int) Double.parseDouble(parts[5].trim());
            data.temp = (int) Double.parseDouble(parts[6].trim());
            
            // Calculate VSWR from forward and reflected power
            if (data.rfl > 0 && data.fwd > 0) {
                double vswr = (1 + Math.sqrt(data.rfl / (double)data.fwd)) / 
                              (1 - Math.sqrt(data.rfl / (double)data.fwd));
                data.vswr = Math.round(vswr * 100.0) / 100.0;
            } else {
                data.vswr = 1.0;
            }
            
            // If VSWR alarm is active, set a high VSWR value
            if (vswrAlarm > 0 && data.vswr < 3.0) {
                data.vswr = 3.0;
            }
            
            System.out.println("[" + cardId + "] PA: " + (data.paOn ? "ON" : "OFF") + 
                             " | FWD: " + data.fwd + "W | RFL: " + data.rfl + "W" +
                             " | VSWR: " + data.vswr + " | Temp: " + data.temp + "°C");
        }
    } catch (Exception e) {
        System.out.println("Error parsing status for " + cardId + ": " + statusLine);
        e.printStackTrace();
    }
    
    return data;
}

4. Also update the logging in ASCIIStatusHandler to see what's being received:
// In the run() method, update the reading loop:
while (running && !socket.isClosed() && (line = reader.readLine()) != null) {
    System.out.println("[" + cardId + "] Raw status: " + line);
    
    if (line.startsWith("CO.HL.G")) {
        HardwareData data = parseASCIIStatus(line);
        this.lastData = data;
        if (dataCallback != null) {
            dataCallback.accept(data);
        }
    }
}

5. For the powerOffAllPAs method, update to handle special bands correctly:
private void powerOffAllPAs(Config config) {
    for (PowerSupply psu : config.power_supplies) {
        PowerSupplySocketHandler psuHandler = psuHandlers.get(psu.id);
        if (psuHandler != null && psuHandler.isConnected()) {
            if (psu.id.equals("psu_02")) {
                psuHandler.enqueueCommand("CO.PS.S,0,0".getBytes());
            } else {
                for (ControlCard card : psu.control_cards) {
                    // Check if this is a special band card
                    boolean isSpecial = false;
                    for (Module m : card.modules) {
                        if (isSpecialBand(m.name)) {
                            isSpecial = true;
                            break;
                        }
                    }
                    
                    if (isSpecial) {
                        // Use control handler on port 7 with ASCII command
                        ControlCardSocketHandler controlHandler = controlHandlers.get(card.id);
                        if (controlHandler != null && controlHandler.isConnected()) {
                            controlHandler.enqueueASCIICommand("CO.PS.S,0\r\n");
                        }
                    } else if (card.off_command != null && !card.off_command.isEmpty()) {
                        splitCmd(card.off_command, psuHandler);
                        psuHandler.enqueueCommand(hexStringToBytes(card.off_command));
                    } else {
                        for (Module m : card.modules) {
                            psuHandler.enqueueCommand(hexStringToBytes(m.power_off));
                        }
                    }
                }
            }
        }
    }
    
    // Handle standalone special bands
    for (ControlCard card : config.standalone_control_cards) {
        boolean isSpecial = false;
        for (Module m : card.modules) {
            if (isSpecialBand(m.name)) {
                isSpecial = true;
                break;
            }
        }
        
        if (isSpecial) {
            ControlCardSocketHandler controlHandler = controlHandlers.get(card.id);
            if (controlHandler != null && controlHandler.isConnected()) {
                controlHandler.enqueueASCIICommand("CO.PS.S,0\r\n");
            }
        } else {
            ControlCardSocketHandler handler = cardHandlers.get(card.id);
            if (handler != null && handler.isConnected()) {
                for (Module m : card.modules) {
                    handler.enqueueCommand(hexStringToBytes(m.power_off));
                }
            }
        }
    }
}

Update the data callback in the IF block to show values even when PA is OFF:
data -> {
    SwingUtilities.invokeLater(() -> {
        System.out.println("Updating UI for " + cardId + " - PA On: " + data.paOn + 
                          " | FWD: " + data.fwd + " | RFL: " + data.rfl + " | Temp: " + data.temp);
        
        for (int i = 0; i < card.modules.size(); i++) {
            JTextField vswrField = vswrList.get(startIndex + i);
            JTextField therField = therList.get(startIndex + i);
            JTextField fwdField = fwdList.get(startIndex + i);
            JTextField rflField = rsvList.get(startIndex + i);
            
            // ALWAYS show values from status (even when PA is OFF)
            // Because CO.HL.G gives us values regardless of PA state
            double vswr = data.vswr;
            vswrField.setText(String.format("%.2f", vswr));
            
            if (vswr >= 3.5) {
                vswrField.setBackground(Color.RED);
                vswrField.setForeground(Color.WHITE);
                vswrField.setToolTipText("VSWR CRITICAL");
            } else if (vswr >= 2.5) {
                vswrField.setBackground(Color.YELLOW);
                vswrField.setForeground(Color.BLACK);
                vswrField.setToolTipText("Warning");
            } else {
                vswrField.setBackground(Color.GREEN);
                vswrField.setForeground(Color.BLACK);
                vswrField.setToolTipText(null);
            }
            
            // Show temperature
            therField.setText(String.valueOf(data.temp) + "°C");
            
            // Show forward and reflected power (even when PA is OFF, these are default values)
            fwdField.setText(String.valueOf(data.fwd) + "W");
            rflField.setText(String.valueOf(data.rfl) + "W");
            
            // Optionally, indicate if PA is OFF by changing text color or adding indicator
            if (!data.paOn) {
                fwdField.setForeground(Color.GRAY);
                rflField.setForeground(Color.GRAY);
                therField.setForeground(Color.GRAY);
                vswrField.setForeground(Color.GRAY);
            } else {
                fwdField.setForeground(Color.BLACK);
                rflField.setForeground(Color.BLACK);
                therField.setForeground(Color.BLACK);
                vswrField.setForeground(Color.BLACK);
            }
        }
    });
}
Also update the ASCIIStatusHandler to properly calculate VSWR from the values:
private HardwareData parseASCIIStatus(String statusLine) {
    // Format: CO.HL.G,PAOnOff,VSWR_Alarm,Temp_Alarm,FwdPower,RefPower,Temperature
    // Example when OFF: CO.HL.G,0,0,0,19,28,28.6
    // Example when ON:  CO.HL.G,1,0,0,19,48,37.1
    HardwareData data = new HardwareData();
    
    try {
        // Remove any carriage return or newline
        statusLine = statusLine.trim();
        String[] parts = statusLine.split(",");
        
        if (parts.length >= 7) {
            int paOnOff = Integer.parseInt(parts[1].trim());
            data.paOn = (paOnOff == 1);
            
            int vswrAlarm = Integer.parseInt(parts[2].trim());
            int tempAlarm = Integer.parseInt(parts[3].trim());
            
            data.fwd = (int) Double.parseDouble(parts[4].trim());
            data.rfl = (int) Double.parseDouble(parts[5].trim());
            data.temp = (int) Double.parseDouble(parts[6].trim());
            
            // Calculate VSWR from forward and reflected power
            // This works for both PA ON and OFF states
            if (data.rfl > 0 && data.fwd > 0) {
                double vswr = (1 + Math.sqrt(data.rfl / (double)data.fwd)) / 
                              (1 - Math.sqrt(data.rfl / (double)data.fwd));
                data.vswr = Math.round(vswr * 100.0) / 100.0;
            } else if (data.fwd > 0 && data.rfl == 0) {
                // Perfect match, no reflection
                data.vswr = 1.0;
            } else if (data.fwd == 0 && data.rfl > 0) {
                // No forward power, infinite VSWR
                data.vswr = 99.99;
            } else {
                data.vswr = 1.0;
            }
            
            // If VSWR alarm is active, ensure VSWR is marked as high
            if (vswrAlarm > 0 && data.vswr < 3.0) {
                data.vswr = 3.0;
            }
            
            System.out.println("[" + cardId + "] PA: " + (data.paOn ? "ON" : "OFF") + 
                             " | FWD: " + data.fwd + "W | RFL: " + data.rfl + "W" +
                             " | VSWR: " + data.vswr + " | Temp: " + data.temp + "°C" +
                             " | Alarm(VSWR:" + vswrAlarm + ",Temp:" + tempAlarm + ")");
        }
    } catch (Exception e) {
        System.out.println("Error parsing status for " + cardId + ": " + statusLine);
        e.printStackTrace();
    }
    
    return data;
}

If you want to show "PA OFF" or default values more clearly, update the UI like this:
data -> {
    SwingUtilities.invokeLater(() -> {
        System.out.println("Updating UI for " + cardId + " - PA On: " + data.paOn + 
                          " | FWD: " + data.fwd + " | RFL: " + data.rfl + " | Temp: " + data.temp);
        
        for (int i = 0; i < card.modules.size(); i++) {
            JTextField vswrField = vswrList.get(startIndex + i);
            JTextField therField = therList.get(startIndex + i);
            JTextField fwdField = fwdList.get(startIndex + i);
            JTextField rflField = rsvList.get(startIndex + i);
            
            // Show all values from the status packet
            double vswr = data.vswr;
            vswrField.setText(String.format("%.2f", vswr));
            
            // Color coding based on VSWR (works for both ON and OFF states)
            if (vswr >= 3.5) {
                vswrField.setBackground(Color.RED);
                vswrField.setForeground(Color.WHITE);
                vswrField.setToolTipText("VSWR CRITICAL");
            } else if (vswr >= 2.5) {
                vswrField.setBackground(Color.YELLOW);
                vswrField.setForeground(Color.BLACK);
                vswrField.setToolTipText("Warning");
            } else {
                vswrField.setBackground(Color.GREEN);
                vswrField.setForeground(Color.BLACK);
                vswrField.setToolTipText(null);
            }
            
            // Temperature
            therField.setText(String.valueOf(data.temp) + "°C");
            
            // If PA is OFF, show values with a different style
            if (!data.paOn) {
                fwdField.setText(data.fwd + "W (Standby)");
                rflField.setText(data.rfl + "W (Standby)");
                fwdField.setForeground(Color.GRAY);
                rflField.setForeground(Color.GRAY);
                therField.setForeground(Color.GRAY);
                
                // Optionally add a tooltip
                fwdField.setToolTipText("PA is OFF - Standby values");
                rflField.setToolTipText("PA is OFF - Standby values");
            } else {
                fwdField.setText(data.fwd + "W");
                rflField.setText(data.rfl + "W");
                fwdField.setForeground(Color.BLACK);
                rflField.setForeground(Color.BLACK);
                therField.setForeground(Color.BLACK);
                fwdField.setToolTipText(null);
                rflField.setToolTipText(null);
            }
            
            // Temperature color coding (optional)
            if (data.temp > 70) {
                therField.setBackground(Color.RED);
                therField.setForeground(Color.WHITE);
                therField.setToolTipText("High Temperature!");
            } else if (data.temp > 50) {
                therField.setBackground(Color.YELLOW);
                therField.setForeground(Color.BLACK);
                therField.setToolTipText("Temperature Warning");
            } else {
                therField.setBackground(Color.WHITE);
                therField.setForeground(Color.BLACK);
                therField.setToolTipText(null);
            }
        }
    });
}

Also, ensure the toggle button updates the UI immediately when clicked:
toggle.addItemListener(e -> {
    if (!toggle.isEnabled()) return;
    
    ControlCardSocketHandler handler = controlHandlers.get(card.id);
    if (handler == null || !handler.isConnected()) {
        System.out.println("Control handler not connected for " + card.id);
        return;
    }
    
    boolean isOn = toggle.isSelected();
    toggle.setForeground(isOn ? Color.GREEN : Color.RED);
    
    // Send ASCII command
    String command = isOn ? "CO.PS.S,1\r\n" : "CO.PS.S,0\r\n";
    handler.enqueueASCIICommand(command);
    
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String logMsg = time + " Band: " + m.name + (isOn ? " Powered ON" : " Powered OFF");
    addLog(logMsg);
    
    // Don't update UI here - wait for status message from device
    // The status will come in the next 300ms CO.HL.G message
});

Complete ASCIIStatusHandler class with debugging:
public class ASCIIStatusHandler implements Runnable {
    private final String ip;
    private final int port;
    private volatile HardwareData lastData;
    private volatile boolean running = true;
    private volatile boolean connected = false;
    private Socket socket;
    private BufferedReader reader;
    private final Consumer<Boolean> statusCallback;
    private final Consumer<HardwareData> dataCallback;
    private ArrayList<JCheckBox> lans;
    private String cardId;

    public ASCIIStatusHandler(String ip, int port, Consumer<Boolean> statusCallback, 
                              Consumer<HardwareData> dataCallback, String cardId) {
        this.ip = ip;
        this.port = port;
        this.statusCallback = statusCallback;
        this.dataCallback = dataCallback;
        this.cardId = cardId;
    }
    
    public void setLans(ArrayList<JCheckBox> lans) {
        this.lans = lans;
    }

    public void start() {
        new Thread(this).start();
    }

    public void shutdown() {
        running = false;
        closeSocket();
    }

    public boolean isConnected() {
        return connected;
    }

    public HardwareData getLastData() {
        return lastData;
    }
    
    private void updateLanStatus(boolean connected) {
        if (lans != null) {
            SwingUtilities.invokeLater(() -> {
                for (JCheckBox lanBox : lans) {
                    lanBox.setIcon(new ColoredBoxIcon(connected ? Color.GREEN : Color.RED));
                    lanBox.setOpaque(true);
                    lanBox.revalidate();
                    lanBox.repaint();
                }
            });
        }
    }

    @Override
    public void run() {
        boolean lastStatus = false;
        
        while (running) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 2000);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                connected = true;
                updateLanStatus(true);
                System.out.println("[" + cardId + "] ASCII Status connected to " + ip + ":" + port);
                
                if (!lastStatus && statusCallback != null) {
                    statusCallback.accept(true);
                    lastStatus = true;
                }
                
                // Read ASCII lines
                String line;
                while (running && !socket.isClosed() && (line = reader.readLine()) != null) {
                    System.out.println("[" + cardId + "] Received: " + line);
                    
                    if (line.startsWith("CO.HL.G")) {
                        HardwareData data = parseASCIIStatus(line);
                        this.lastData = data;
                        
                        System.out.println("[" + cardId + "] Parsed - PA On: " + data.paOn + 
                                         ", FWD: " + data.fwd + 
                                         ", RFL: " + data.rfl + 
                                         ", VSWR: " + data.vswr + 
                                         ", Temp: " + data.temp);
                        
                        if (dataCallback != null) {
                            System.out.println("[" + cardId + "] Calling dataCallback...");
                            dataCallback.accept(data);
                        } else {
                            System.out.println("[" + cardId + "] WARNING: dataCallback is NULL!");
                        }
                    }
                }
                
            } catch (Exception e) {
                System.out.println("[" + cardId + "] ASCII Status connection lost: " + ip + ":" + port);
                e.printStackTrace();
                connected = false;
                updateLanStatus(false);
                
                if (lastStatus && statusCallback != null) {
                    statusCallback.accept(false);
                    lastStatus = false;
                }
                
                closeSocket();
                
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
    
    private HardwareData parseASCIIStatus(String statusLine) {
        HardwareData data = new HardwareData();
        
        try {
            statusLine = statusLine.trim();
            String[] parts = statusLine.split(",");
            
            if (parts.length >= 7) {
                int paOnOff = Integer.parseInt(parts[1].trim());
                data.paOn = (paOnOff == 1);
                
                int vswrAlarm = Integer.parseInt(parts[2].trim());
                int tempAlarm = Integer.parseInt(parts[3].trim());
                
                data.fwd = (int) Double.parseDouble(parts[4].trim());
                data.rfl = (int) Double.parseDouble(parts[5].trim());
                data.temp = (int) Double.parseDouble(parts[6].trim());
                
                // Calculate VSWR
                if (data.rfl > 0 && data.fwd > 0) {
                    double vswr = (1 + Math.sqrt(data.rfl / (double)data.fwd)) / 
                                  (1 - Math.sqrt(data.rfl / (double)data.fwd));
                    data.vswr = Math.round(vswr * 100.0) / 100.0;
                } else if (data.fwd > 0) {
                    data.vswr = 1.0;
                } else {
                    data.vswr = 99.99;
                }
            }
        } catch (Exception e) {
            System.out.println("[" + cardId + "] Error parsing: " + statusLine);
            e.printStackTrace();
        }
        
        return data;
    }
    
    private void closeSocket() {
        try {
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}

Complete IF block for special bands with proper UI update:
// ==================== IF BLOCK: SPECIAL BANDS ====================
if (hasSpecialBand) {
    System.out.println("Setting up SPECIAL band card: " + cardId + " on IP: " + card.ip);
    
    // Create ArrayList for LAN checkboxes
    ArrayList<JCheckBox> tempLanBoxes = new ArrayList<>();
    final int cardStartIndex = startIndex;  // Store for use in callback
    final String finalCardId = cardId;       // Store for use in callback
    
    // Create UI components for each module
    for (Module m : card.modules) {
        JToggleButton toggle = new JToggleButton(m.name);
        toggle.setFont(new Font("Arial", Font.BOLD, 28));
        toggle.setForeground(Color.RED);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setEnabled(false);
        
        toggle.addItemListener(e -> {
            if (!toggle.isEnabled()) return;
            
            ControlCardSocketHandler handler = controlHandlers.get(card.id);
            if (handler == null || !handler.isConnected()) {
                System.out.println("Control handler not connected for " + card.id);
                return;
            }
            
            boolean isOn = toggle.isSelected();
            toggle.setForeground(isOn ? Color.GREEN : Color.RED);
            
            String command = isOn ? "CO.PS.S,1\r\n" : "CO.PS.S,0\r\n";
            handler.enqueueASCIICommand(command);
            
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logMsg = time + " Band: " + m.name + (isOn ? " Powered ON" : " Powered OFF");
            addLog(logMsg);
        });
        
        panel_3.add(toggle);
        toggles.add(toggle);
        
        // Frequency band label
        JLabel bandsLabel = new JLabel(m.name);
        bandsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bandsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        freqband_names_panel.add(bandsLabel);
        
        // LAN checkbox
        JCheckBox lanBox = new JCheckBox("");
        lanBox.setHorizontalAlignment(SwingConstants.CENTER);
        lanBox.setEnabled(false);
        lan_checkbox_panel.add(lanBox);
        tempLanBoxes.add(lanBox);
        
        // VSWR field
        JTextField vswrBox = new JTextField("--");
        vswrBox.setHorizontalAlignment(SwingConstants.CENTER);
        vswrBox.setEditable(false);
        vswrBox.setBackground(Color.WHITE);
        vswr_checkbox_panel.add(vswrBox);
        vswrList.add(vswrBox);
        
        // Temperature field
        JTextField therBox = new JTextField("--");
        therBox.setHorizontalAlignment(SwingConstants.CENTER);
        therBox.setEditable(false);
        therBox.setBackground(Color.WHITE);
        ther_checkbox_panel.add(therBox);
        therList.add(therBox);
        
        // Forward power field
        JTextField fwdField = new JTextField("--");
        fwdField.setHorizontalAlignment(SwingConstants.CENTER);
        fwdField.setEditable(false);
        fwdField.setBackground(Color.WHITE);
        fwd_textfields_panel.add(fwdField);
        fwdList.add(fwdField);
        
        // Reverse power field
        JTextField rflField = new JTextField("--");
        rflField.setHorizontalAlignment(SwingConstants.CENTER);
        rflField.setEditable(false);
        rflField.setBackground(Color.WHITE);
        rsv_textfields_panel.add(rflField);
        rsvList.add(rflField);
    }
    
    // Create ASCII status handler on Port 20108
    ASCIIStatusHandler statusHandler = new ASCIIStatusHandler(
        card.ip, 
        20108,
        isConnected -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("[" + finalCardId + "] Status callback - Connected: " + isConnected);
                JLabel lbl = statusLabels.get(finalCardId);
                if (lbl != null) {
                    lbl.setBackground(isConnected ? Color.GREEN : Color.RED);
                }
                
                for (JCheckBox lanBox : tempLanBoxes) {
                    lanBox.setIcon(new ColoredBoxIcon(isConnected ? Color.GREEN : Color.RED));
                    lanBox.revalidate();
                    lanBox.repaint();
                }
                
                List<JToggleButton> btns = toggleMap.get(card.id);
                if (btns != null) {
                    for (JToggleButton btn : btns) {
                        btn.setEnabled(isConnected);
                    }
                }
            });
        },
        data -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("[" + finalCardId + "] UI UPDATE CALLBACK - Updating " + card.modules.size() + " modules");
                
                for (int i = 0; i < card.modules.size(); i++) {
                    int index = cardStartIndex + i;
                    
                    if (index >= vswrList.size()) {
                        System.out.println("ERROR: Index " + index + " out of bounds! vswrList size: " + vswrList.size());
                        continue;
                    }
                    
                    JTextField vswrField = vswrList.get(index);
                    JTextField therField = therList.get(index);
                    JTextField fwdField = fwdList.get(index);
                    JTextField rflField = rsvList.get(index);
                    
                    // Update VSWR
                    vswrField.setText(String.format("%.2f", data.vswr));
                    
                    if (data.vswr >= 3.5) {
                        vswrField.setBackground(Color.RED);
                        vswrField.setForeground(Color.WHITE);
                    } else if (data.vswr >= 2.5) {
                        vswrField.setBackground(Color.YELLOW);
                        vswrField.setForeground(Color.BLACK);
                    } else {
                        vswrField.setBackground(Color.GREEN);
                        vswrField.setForeground(Color.BLACK);
                    }
                    
                    // Update Temperature
                    therField.setText(data.temp + "°C");
                    
                    // Update Power values
                    fwdField.setText(data.fwd + "W");
                    rflField.setText(data.rfl + "W");
                    
                    System.out.println("[" + finalCardId + "] Module " + i + " - VSWR: " + data.vswr + 
                                     ", Temp: " + data.temp + ", FWD: " + data.fwd + ", RFL: " + data.rfl);
                }
            });
        },
        cardId
    );
    
    statusHandler.setLans(tempLanBoxes);
    statusHandler.start();
    asciiStatusHandlers.put(cardId, statusHandler);
    
    // Delay to ensure status connection is established
    Thread.sleep(500);
    
    // Create control handler on Port 7
    ControlCardSocketHandler controlHandler = new ControlCardSocketHandler(
        card.ip, 
        7,
        null,
        isConnected -> {
            System.out.println("[" + cardId + "] Control connection: " + isConnected);
        },
        null
    );
    controlHandler.start();
    controlHandlers.put(cardId, controlHandler);
    cardHandlers.put(cardId, controlHandler);
}

Add the enqueueASCIICommand method to ControlCardSocketHandler:
public void enqueueASCIICommand(String command) {
    if (command != null && connected.get()) {
        byte[] cmdBytes = command.getBytes(StandardCharsets.US_ASCII);
        sendQueue.offer(cmdBytes);
        System.out.println("[" + ip + ":" + port + "] Sent ASCII: " + command.trim());
    }
}