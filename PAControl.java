import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ControlSystemUI extends JFrame {
    
	private static final long serialVersionUID = 1L;
	private final Map<String, ControlCardSocketHandler> cardHandlers = new HashMap<>();
	private final Map<String, PowerSupplySocketHandler> psuHandlers = new HashMap<>();
	
	private JButton powerOnButton;
	private JButton powerOffButton;
	private boolean paEnabled = false; // Track if PA can be controlled
	
	// For displaying band LAN status
	private Map<String, BandInfo> bandInfoMap = new LinkedHashMap<>();
	private List<ControlCard> allCards = new ArrayList<>();
	
	private class BandInfo {
		String bandName;
		String cardId;
		JLabel statusLight;
		JLabel statusTextLabel;
		JLabel connectionLabel;
		boolean isConnected = false;
	}
	
	public ControlSystemUI(Config config) throws InterruptedException {
		setTitle("4-Band Jammer Control System");
		setSize(550, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));
		
		// NORTH PANEL - Control buttons
		JPanel northPanel = createControlPanel();
		add(northPanel, BorderLayout.NORTH);
		
		// CENTER PANEL - Band LAN Status
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(new TitledBorder(new LineBorder(Color.BLACK), "Band Connection Status"));
		
		JPanel bandsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
		bandsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
		
		// Create 4 band displays
		createBandDisplays(config, bandsPanel);
		
		centerPanel.add(bandsPanel, BorderLayout.CENTER);
		add(centerPanel, BorderLayout.CENTER);
		
		// SOUTH PANEL - Instructions
		JPanel southPanel = createInfoPanel();
		add(southPanel, BorderLayout.SOUTH);
		
		// Initialize connections
		initializeConnections(config);
		
		// Initially disable power buttons until all bands connect
		powerOnButton.setEnabled(false);
		powerOffButton.setEnabled(false);
		
		setVisible(true);
		setLocationRelativeTo(null);
		
		// Add window listener to power off on exit
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				powerOffAllBands();
				try { Thread.sleep(500); } catch (Exception ex) {}
			}
		});
	}
	
	private JPanel createControlPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		powerOnButton = new JButton("POWER ON ALL BANDS");
		powerOnButton.setFont(new Font("Arial", Font.BOLD, 16));
		powerOnButton.setPreferredSize(new Dimension(200, 60));
		powerOnButton.setBackground(new Color(0, 150, 0));
		powerOnButton.setForeground(Color.WHITE);
		powerOnButton.setFocusPainted(false);
		powerOnButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		powerOnButton.setEnabled(false);
		
		powerOffButton = new JButton("POWER OFF ALL BANDS");
		powerOffButton.setFont(new Font("Arial", Font.BOLD, 16));
		powerOffButton.setPreferredSize(new Dimension(200, 60));
		powerOffButton.setBackground(new Color(150, 0, 0));
		powerOffButton.setForeground(Color.WHITE);
		powerOffButton.setFocusPainted(false);
		powerOffButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		powerOffButton.setEnabled(false);
		
		powerOnButton.addActionListener(e -> powerOnAllBands());
		powerOffButton.addActionListener(e -> powerOffAllBands());
		
		panel.add(powerOnButton);
		panel.add(powerOffButton);
		
		// Legend
		JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
		legendPanel.add(createLegendItem(Color.GREEN, "Connected"));
		legendPanel.add(createLegendItem(Color.RED, "Disconnected"));
		legendPanel.add(createLegendItem(Color.ORANGE, "Connecting..."));
		panel.add(legendPanel);
		
		return panel;
	}
	
	private JPanel createLegendItem(Color color, String text) {
		JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		JLabel icon = new JLabel("●");
		icon.setForeground(color);
		icon.setFont(new Font("Arial", Font.BOLD, 16));
		JLabel label = new JLabel(text);
		label.setFont(new Font("Arial", Font.PLAIN, 11));
		item.add(icon);
		item.add(label);
		return item;
	}
	
	private JPanel createInfoPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JLabel infoLabel = new JLabel("💡 Wait for all bands to show GREEN (Connected) before using Power buttons");
		infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
		infoLabel.setForeground(Color.BLUE);
		panel.add(infoLabel);
		
		return panel;
	}
	
	private void createBandDisplays(Config config, JPanel bandsPanel) {
		// Collect all cards and their modules (limit to 4 bands)
		for (PowerSupply psu : config.power_supplies) {
			allCards.addAll(psu.control_cards);
		}
		allCards.addAll(config.standalone_control_cards);
		
		int bandCount = 0;
		for (ControlCard card : allCards) {
			for (Module module : card.modules) {
				if (bandCount >= 4) break;
				
				BandInfo bandInfo = new BandInfo();
				bandInfo.bandName = module.name;
				bandInfo.cardId = card.id;
				bandInfo.isConnected = false;
				
				JPanel bandPanel = createBandStatusPanel(bandInfo);
				bandsPanel.add(bandPanel);
				bandInfoMap.put(card.id + "_" + module.name, bandInfo);
				bandCount++;
			}
			if (bandCount >= 4) break;
		}
		
		// If less than 4 bands, add placeholders
		while (bandCount < 4) {
			JPanel placeholder = new JPanel(new BorderLayout());
			placeholder.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(Color.GRAY, 1),
				new EmptyBorder(20, 20, 20, 20)
			));
			placeholder.setBackground(Color.WHITE);
			JLabel label = new JLabel("No Band Configured", SwingConstants.CENTER);
			label.setFont(new Font("Arial", Font.ITALIC, 14));
			label.setForeground(Color.GRAY);
			placeholder.add(label, BorderLayout.CENTER);
			bandsPanel.add(placeholder);
			bandCount++;
		}
	}
	
	private JPanel createBandStatusPanel(BandInfo bandInfo) {
		JPanel panel = new JPanel(new BorderLayout(15, 10));
		panel.setBorder(BorderFactory.createCompoundBorder(
			new LineBorder(Color.GRAY, 2),
			new EmptyBorder(15, 20, 15, 20)
		));
		panel.setBackground(Color.WHITE);
		
		// Status Light (Big circle)
		bandInfo.statusLight = new JLabel("●");
		bandInfo.statusLight.setFont(new Font("Arial", Font.BOLD, 50));
		bandInfo.statusLight.setForeground(Color.RED);
		bandInfo.statusLight.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(bandInfo.statusLight, BorderLayout.WEST);
		
		// Band info
		JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
		infoPanel.setBackground(Color.WHITE);
		
		JLabel bandNameLabel = new JLabel(bandInfo.bandName);
		bandNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
		infoPanel.add(bandNameLabel);
		
		bandInfo.statusTextLabel = new JLabel("STATUS: DISCONNECTED");
		bandInfo.statusTextLabel.setFont(new Font("Arial", Font.BOLD, 12));
		bandInfo.statusTextLabel.setForeground(Color.RED);
		infoPanel.add(bandInfo.statusTextLabel);
		
		bandInfo.connectionLabel = new JLabel("🔌 Waiting for connection...");
		bandInfo.connectionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
		bandInfo.connectionLabel.setForeground(Color.GRAY);
		infoPanel.add(bandInfo.connectionLabel);
		
		panel.add(infoPanel, BorderLayout.CENTER);
		
		// LAN icon
		JLabel lanIcon = new JLabel("🌐");
		lanIcon.setFont(new Font("Segoe UI", Font.PLAIN, 30));
		panel.add(lanIcon, BorderLayout.EAST);
		
		return panel;
	}
	
	private void initializeConnections(Config config) throws InterruptedException {
		// Keep track of how many bands are connected
		Map<String, Boolean> bandConnectionStatus = new HashMap<>();
		
		// Initialize Control Card handlers
		for (ControlCard card : allCards) {
			final String cardId = card.id;
			
			// Initialize connection status for this card's bands
			for (BandInfo bandInfo : bandInfoMap.values()) {
				if (bandInfo.cardId.equals(cardId)) {
					bandConnectionStatus.put(bandInfo.bandName, false);
				}
			}
			
			ControlCardSocketHandler handler = new ControlCardSocketHandler(
				card.ip, card.port,
				isConnected -> {
					SwingUtilities.invokeLater(() -> {
						// Update all bands for this card
						for (BandInfo bandInfo : bandInfoMap.values()) {
							if (bandInfo.cardId.equals(cardId)) {
								bandInfo.isConnected = isConnected;
								
								if (isConnected) {
									bandInfo.statusLight.setForeground(Color.GREEN);
									bandInfo.statusTextLabel.setText("STATUS: CONNECTED");
									bandInfo.statusTextLabel.setForeground(Color.GREEN);
									bandInfo.connectionLabel.setText("✅ Connected to " + card.ip + ":" + card.port);
									bandConnectionStatus.put(bandInfo.bandName, true);
								} else {
									bandInfo.statusLight.setForeground(Color.RED);
									bandInfo.statusTextLabel.setText("STATUS: DISCONNECTED");
									bandInfo.statusTextLabel.setForeground(Color.RED);
									bandInfo.connectionLabel.setText("❌ Connection lost - Reconnecting...");
									bandConnectionStatus.put(bandInfo.bandName, false);
								}
							}
						}
						
						// Check if all bands are connected
						checkAllBandsConnected(bandConnectionStatus);
					});
				}
			);
			handler.start();
			cardHandlers.put(card.id, handler);
			Thread.sleep(100);
		}
	}
	
	private void checkAllBandsConnected(Map<String, Boolean> bandConnectionStatus) {
		boolean allConnected = true;
		for (Boolean connected : bandConnectionStatus.values()) {
			if (!connected) {
				allConnected = false;
				break;
			}
		}
		
		if (allConnected && !paEnabled) {
			paEnabled = true;
			powerOnButton.setEnabled(true);
			powerOffButton.setEnabled(true);
			System.out.println("All bands connected! Power buttons enabled.");
		} else if (!allConnected && paEnabled) {
			paEnabled = false;
			powerOnButton.setEnabled(false);
			powerOffButton.setEnabled(false);
			System.out.println("Some bands disconnected. Power buttons disabled.");
		}
	}
	
	private void powerOnAllBands() {
		if (!paEnabled) {
			JOptionPane.showMessageDialog(this, 
				"Wait for all bands to connect (GREEN status) before powering on.", 
				"Not Connected", 
				JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		powerOnButton.setEnabled(false);
		powerOffButton.setEnabled(false);
		
		// Send power on commands to all bands
		for (ControlCard card : allCards) {
			ControlCardSocketHandler cardHandler = cardHandlers.get(card.id);
			
			if (cardHandler != null && cardHandler.isConnected()) {
				// Send module-level on commands
				for (Module module : card.modules) {
					if (module.power_on != null && !module.power_on.isEmpty()) {
						byte[] command = hexStringToBytes(module.power_on);
						cardHandler.enqueueCommand(command);
						System.out.println("Sent POWER ON to " + module.name);
					}
				}
			}
		}
		
		JOptionPane.showMessageDialog(this, 
			"✅ POWER ON command sent to all " + allCards.size() + " bands.", 
			"Command Sent", 
			JOptionPane.INFORMATION_MESSAGE);
		
		// Re-enable buttons after delay
		Timer timer = new Timer(2000, e -> {
			powerOnButton.setEnabled(true);
			powerOffButton.setEnabled(true);
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	private void powerOffAllBands() {
		if (!paEnabled) {
			return;
		}
		
		powerOnButton.setEnabled(false);
		powerOffButton.setEnabled(false);
		
		// Send power off commands to all bands
		for (ControlCard card : allCards) {
			ControlCardSocketHandler cardHandler = cardHandlers.get(card.id);
			
			if (cardHandler != null && cardHandler.isConnected()) {
				// Send module-level off commands
				for (Module module : card.modules) {
					if (module.power_off != null && !module.power_off.isEmpty()) {
						byte[] command = hexStringToBytes(module.power_off);
						cardHandler.enqueueCommand(command);
						System.out.println("Sent POWER OFF to " + module.name);
					}
				}
			}
		}
		
		JOptionPane.showMessageDialog(this, 
			"✅ POWER OFF command sent to all " + allCards.size() + " bands.", 
			"Command Sent", 
			JOptionPane.INFORMATION_MESSAGE);
		
		// Re-enable buttons after delay
		Timer timer = new Timer(2000, e -> {
			powerOnButton.setEnabled(true);
			powerOffButton.setEnabled(true);
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	private byte[] hexStringToBytes(String hex) {
		if (hex == null || hex.trim().isEmpty()) {
			return new byte[0];
		}
		
		String[] parts = hex.trim().split(",");
		List<Byte> byteList = new ArrayList<>();
		
		for (String part : parts) {
			String value = part.trim();
			if (!value.isEmpty()) {
				try {
					byteList.add((byte) Integer.parseInt(value, 16));
				} catch (NumberFormatException e) {
					// Skip invalid values
				}
			}
		}
		
		byte[] bytes = new byte[byteList.size()];
		for (int i = 0; i < byteList.size(); i++) {
			bytes[i] = byteList.get(i);
		}
		
		return bytes;
	}
	
	@Override
	public void dispose() {
		for (ControlCardSocketHandler handler : cardHandlers.values()) {
			handler.shutdown();
		}
		for (PowerSupplySocketHandler handler : psuHandlers.values()) {
			handler.shutdown();
		}
		super.dispose();
	}
}


public class PowerSupplySocketHandler extends Thread {
    private final String ip;
    private final int port;
    private final Consumer<Boolean> statusCallback;
    private final ArrayBlockingQueue<byte[]> commandQueue = new ArrayBlockingQueue<>(20);
    private volatile boolean running = true;
    private volatile boolean connected = false;
    private Socket socket;
    private OutputStream out;

    public PowerSupplySocketHandler(String ip, int port, Consumer<Boolean> statusCallback) {
        this.ip = ip;
        this.port = port;
        this.statusCallback = statusCallback;
    }

    public boolean isConnected() {
        return connected;
    }

    public void enqueueCommand(byte[] command) {
        try {
            commandQueue.put(command);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
        closeSocket();
    }

    @Override
    public void run() {
        while (running) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 2000);
                System.out.println("PSU Connected to " + ip + ":" + port);
                
                out = socket.getOutputStream();
                connected = true;
                statusCallback.accept(true);

                while (running && !socket.isClosed()) {
                    byte[] command = commandQueue.take();
                    out.write(command);
                    out.flush();
                    System.out.println("PSU Command sent to " + ip);
                    Thread.sleep(100);
                }

            } catch (Exception e) {
                connected = false;
                statusCallback.accept(false);
                closeSocket();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
    
    private void closeSocket() {
        try {
            if (socket != null)
                socket.close();
        } catch (Exception ignored) {
        }
    }
}

public class ControlCardSocketHandler implements Runnable {

	private final String ip;
	private final int port;
	private Thread senderThread;
	private final Consumer<Boolean> statusCallback;
	private final ArrayBlockingQueue<byte[]> sendQueue;
	private volatile boolean running = true;
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private Socket socket;
	private OutputStream out;
	
	public ControlCardSocketHandler(String ip, int port, Consumer<Boolean> statusCallback) {
		this.ip = ip;
		this.port = port;
		this.statusCallback = statusCallback;
		this.sendQueue = new ArrayBlockingQueue<>(100);
	}

	public void enqueueCommand(byte[] command) {
		if (command != null && connected.get()) {
			sendQueue.offer(command);
		}
	}

	public boolean isConnected() {
		return connected.get();
	}

	public void shutdown() {
		running = false;
		closeSocket();
	}

	public void start() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		boolean lastStatus = false;

		while (running) {
			try {
				socket = new Socket();
				socket.connect(new InetSocketAddress(ip, port), 2000);
				System.out.println("Connected to " + ip + ":" + port);

				out = socket.getOutputStream();
				connected.set(true);

				if (!lastStatus && statusCallback != null) {
					statusCallback.accept(true);  // Connection established → GREEN
					lastStatus = true;
				}

				startSender();

				// Keep connection alive
				while (running && socket != null && !socket.isClosed()) {
					Thread.sleep(1000);
				}

			} catch (Exception e) {
				System.out.println("Connection failed for: " + ip + ":" + port);
				connected.set(false);

				if (lastStatus && statusCallback != null) {
					statusCallback.accept(false);  // Connection lost → RED
					lastStatus = false;
				}
				closeSocket();

				try {
					Thread.sleep(5000); // Wait 5 seconds before reconnecting
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	private void closeSocket() {
		connected.set(false);
		try {
			if (socket != null)
				socket.close();
		} catch (Exception ignored) {
		}
	}

	private void startSender() {
		senderThread = new Thread(() -> {
			try {
				while (running && socket != null && !socket.isClosed()) {
					byte[] cmd = sendQueue.take();
					if (out != null) {
						out.write(cmd);
						out.flush();
						System.out.println("Sent command to " + ip);
					}
				}
			} catch (Exception e) {
				System.out.println("Sender stopped for " + ip);
			}
		});
		senderThread.start();
	}
}