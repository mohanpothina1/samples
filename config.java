


private String isForEWTS = "No";

@XmlElement(name = "IsForEWTS")
public String getIsForEWTS() {
    return isForEWTS;
}

public void setIsForEWTS(String isForEWTS) {
    this.isForEWTS = isForEWTS;
}

private JRadioButton yesRadio;
private JRadioButton noRadio;

Change GridLayout from:

JPanel form = new JPanel(new GridLayout(3,2,10,10));

to

JPanel form = new JPanel(new GridLayout(4,2,10,10));


yesRadio = new JRadioButton("Yes");
noRadio = new JRadioButton("No");

ButtonGroup group = new ButtonGroup();
group.add(yesRadio);
group.add(noRadio);

JPanel radioPanel = new JPanel();
radioPanel.add(yesRadio);
radioPanel.add(noRadio);

form.add(new JLabel("Is For EWTS"));
form.add(radioPanel);

private void loadData() {

    S_OtherParamConfiguration other =
            config.getOtherParamConfigObj();

    crcField.setText(
            String.valueOf(other.getCRC_Periodicty_In_Minutes()));

    biteField.setText(
            String.valueOf(other.getBite_Periodicity_In_Minutes()));

    blockField.setText(
            String.valueOf(other.getBlockID()));

    if ("Yes".equalsIgnoreCase(other.getIsForEWTS())) {
        yesRadio.setSelected(true);
    } else {
        noRadio.setSelected(true);
    }
}

private void saveData() {

    S_OtherParamConfiguration other =
            config.getOtherParamConfigObj();

    other.setCRC_Periodicty_In_Minutes(
            Integer.parseInt(crcField.getText()));

    other.setBite_Periodicity_In_Minutes(
            Integer.parseInt(biteField.getText()));

    other.setBlockID(
            Integer.parseInt(blockField.getText()));

    if (yesRadio.isSelected()) {
        other.setIsForEWTS("Yes");
    } else {
        other.setIsForEWTS("No");
    }

    config.marshal();

    JOptionPane.showMessageDialog(
            this,
            "Other Parameters Saved");
}


package configui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import config.S_EntityConfiguration;
import config.S_PostConfiguration;
import config.SystemConfigXML;

public class EntityPanel extends JPanel {

    private SystemConfigXML config;

    public EntityPanel(SystemConfigXML config) {

        this.config = config;

        setLayout(new BorderLayout());

        JTabbedPane entityTabs = new JTabbedPane();

        for (S_EntityConfiguration entity :
                config.getList_SystemConfigs()) {

            JTabbedPane postTabs = new JTabbedPane();

            for (S_PostConfiguration post :
                    entity.getList_SystemConfigs()) {

                postTabs.addTab(
                        post.getName(),
                        new PostConfigPanel(
                                config,
                                post
                        )
                );
            }

            entityTabs.addTab(
                    entity.getName(),
                    postTabs
            );
        }

        add(entityTabs, BorderLayout.CENTER);
    }
}

package configui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import config.S_PostConfiguration;
import config.SystemConfigXML;

public class PostConfigPanel extends JPanel {

    private SystemConfigXML config;

    private S_PostConfiguration post;

    private JTextField ipField;
    private JTextField portField;

    public PostConfigPanel(
            SystemConfigXML config,
            S_PostConfiguration post) {

        this.config = config;
        this.post = post;

        setLayout(new BorderLayout());

        JPanel form = new JPanel(
                new GridLayout(2,2,10,10));

        ipField = new JTextField();
        portField = new JTextField();

        form.add(new JLabel("IP Address"));
        form.add(ipField);

        form.add(new JLabel("Port"));
        form.add(portField);

        loadData();

        JButton save = new JButton("Save");

        save.addActionListener(
                e -> saveData()
        );

        add(form, BorderLayout.CENTER);
        add(save, BorderLayout.SOUTH);
    }

    private void loadData() {

        ipField.setText(
                post.getIP_Address()
        );

        portField.setText(
                String.valueOf(
                        post.getPort()
                )
        );
    }

    private void saveData() {

        try {

            post.setIP_Address(
                    ipField.getText().trim()
            );

            post.setPort(
                    Integer.parseInt(
                            portField.getText().trim()
                    )
            );

            config.marshal();

            JOptionPane.showMessageDialog(
                    this,
                    "Configuration Saved"
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage()
            );
        }
    }
}


--------

    package configui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import config.S_EntityConfiguration;
import config.S_PostConfiguration;
import config.SystemConfigXML;

public class EntityPanel extends JPanel {

    private SystemConfigXML config;

    public EntityPanel(SystemConfigXML config) {

        this.config = config;

        setLayout(new BorderLayout());

        JTabbedPane entityTabs = new JTabbedPane();

        for (S_EntityConfiguration entity :
                config.getList_SystemConfigs()) {

            JTabbedPane postTabs = new JTabbedPane();

            for (S_PostConfiguration post :
                    entity.getList_SystemConfigs()) {

                postTabs.addTab(
                        post.getName(),
                        new PostConfigPanel(config, post)
                );
            }

            entityTabs.addTab(
                    entity.getName(),
                    postTabs
            );
        }

        add(entityTabs, BorderLayout.CENTER);
    }
}

package configui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import config.S_PostConfiguration;
import config.SystemConfigXML;

public class PostConfigPanel extends JPanel {

    private SystemConfigXML config;
    private S_PostConfiguration post;

    private JTextField txtIp;
    private JTextField txtPort;

    public PostConfigPanel(
            SystemConfigXML config,
            S_PostConfiguration post) {

        this.config = config;
        this.post = post;

        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(2,2,10,10));

        txtIp = new JTextField();
        txtPort = new JTextField();

        form.add(new JLabel("IP Address"));
        form.add(txtIp);

        form.add(new JLabel("Port"));
        form.add(txtPort);

        loadData();

        JButton btnSave = new JButton("Save");

        btnSave.addActionListener(e -> saveData());

        add(form, BorderLayout.CENTER);
        add(btnSave, BorderLayout.SOUTH);
    }

    private void loadData() {

        txtIp.setText(post.getIP_Address());

        txtPort.setText(
                String.valueOf(post.getPort()));
    }

    private void saveData() {

        try {

            post.setIP_Address(
                    txtIp.getText().trim());

            post.setPort(
                    Integer.parseInt(
                            txtPort.getText().trim()));

            config.marshal();

            JOptionPane.showMessageDialog(
                    this,
                    "Configuration Saved");

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage());
        }
    }
}

JTabbedPane mainTabs = new JTabbedPane();

mainTabs.addTab(
        "Entity",
        new EntityPanel(config)
);

mainTabs.addTab(
        "Database",
        new DatabasePanel(config)
);

mainTabs.addTab(
        "Other Params",
        new OtherParamPanel(config)
);

mainTabs.addTab(
        "ESM Tasking",
        new ESMTaskingPanel(config)
);

mainTabs.addTab(
        "Jammer Tasking",
        new JammerTaskingPanel(config)
);


