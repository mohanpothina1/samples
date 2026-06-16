package configui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import config.SystemConfigXML;

public class ConfigurationFrame extends JFrame {

    private SystemConfigXML config;

    public ConfigurationFrame() {

        setTitle("System Configuration");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        config = SystemConfigXML.getInstance().unMarshall();

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab(
                "Entities",
                new EntityPanel(config)
        );

        tabs.addTab(
                "Database",
                new DatabasePanel(config)
        );

        tabs.addTab(
                "Other Parameters",
                new OtherParamPanel(config)
        );

        add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {

        new ConfigurationFrame().setVisible(true);
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

import config.S_DBConfiguration;
import config.SystemConfigXML;

public class DatabasePanel extends JPanel {

    private SystemConfigXML config;

    private JTextField driverField;
    private JTextField urlField;
    private JTextField userField;
    private JTextField passwordField;
    private JTextField portField;

    public DatabasePanel(SystemConfigXML config) {

        this.config = config;

        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(5,2,10,10));

        driverField = new JTextField();
        urlField = new JTextField();
        userField = new JTextField();
        passwordField = new JTextField();
        portField = new JTextField();

        form.add(new JLabel("Driver"));
        form.add(driverField);

        form.add(new JLabel("URL"));
        form.add(urlField);

        form.add(new JLabel("Username"));
        form.add(userField);

        form.add(new JLabel("Password"));
        form.add(passwordField);

        form.add(new JLabel("Port"));
        form.add(portField);

        loadData();

        JButton saveBtn = new JButton("Save");

        saveBtn.addActionListener(e -> saveData());

        add(form, BorderLayout.CENTER);
        add(saveBtn, BorderLayout.SOUTH);
    }

    private void loadData() {

        S_DBConfiguration db = config.getDbConfigObj();

        driverField.setText(db.getDb_DRIVER());
        urlField.setText(db.getDb_URL());
        userField.setText(db.getDb_USERNAME());
        passwordField.setText(db.getDb_PASSWORD());
        portField.setText(String.valueOf(db.getDb_PORT()));
    }

    private void saveData() {

        S_DBConfiguration db = config.getDbConfigObj();

        db.setDb_DRIVER(driverField.getText());
        db.setDb_URL(urlField.getText());
        db.setDb_USERNAME(userField.getText());
        db.setDb_PASSWORD(passwordField.getText());
        db.setDb_PORT(Integer.parseInt(portField.getText()));

        config.marshal();

        JOptionPane.showMessageDialog(
                this,
                "Database Configuration Saved"
        );
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

import config.S_OtherParamConfiguration;
import config.SystemConfigXML;

public class OtherParamPanel extends JPanel {

    private SystemConfigXML config;

    private JTextField crcField;
    private JTextField biteField;
    private JTextField blockField;

    public OtherParamPanel(SystemConfigXML config) {

        this.config = config;

        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(3,2,10,10));

        crcField = new JTextField();
        biteField = new JTextField();
        blockField = new JTextField();

        form.add(new JLabel("CRC Minutes"));
        form.add(crcField);

        form.add(new JLabel("BITE Minutes"));
        form.add(biteField);

        form.add(new JLabel("Block ID"));
        form.add(blockField);

        loadData();

        JButton save = new JButton("Save");

        save.addActionListener(e -> saveData());

        add(form, BorderLayout.CENTER);
        add(save, BorderLayout.SOUTH);
    }

    private void loadData() {

        S_OtherParamConfiguration other =
                config.getOtherParamConfigObj();

        crcField.setText(
                String.valueOf(other.getCRC_Periodicty_In_Minutes())
        );

        biteField.setText(
                String.valueOf(other.getBite_Periodicity_In_Minutes())
        );

        blockField.setText(
                String.valueOf(other.getBlockID())
        );
    }

    private void saveData() {

        S_OtherParamConfiguration other =
                config.getOtherParamConfigObj();

        other.setCRC_Periodicty_In_Minutes(
                Integer.parseInt(crcField.getText())
        );

        other.setBite_Periodicity_In_Minutes(
                Integer.parseInt(biteField.getText())
        );

        other.setBlockID(
                Integer.parseInt(blockField.getText())
        );

        config.marshal();

        JOptionPane.showMessageDialog(
                this,
                "Other Parameters Saved"
        );
    }
}package configui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import config.S_EntityConfiguration;
import config.S_PostConfiguration;
import config.SystemConfigXML;

public class EntityPanel extends JPanel {

    private SystemConfigXML config;

    private JTable table;
    private DefaultTableModel model;

    public EntityPanel(SystemConfigXML config) {

        this.config = config;

        setLayout(new BorderLayout());

        model = new DefaultTableModel(
                new String[]{
                        "Entity",
                        "Post",
                        "IP Address",
                        "Port"
                },
                0
        );

        table = new JTable(model);

        loadData();

        JButton save = new JButton("Save");

        save.addActionListener(e -> saveData());

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(save, BorderLayout.SOUTH);
    }

    private void loadData() {

        model.setRowCount(0);

        for (S_EntityConfiguration entity :
                config.getList_SystemConfigs()) {

            for (S_PostConfiguration post :
                    entity.getList_SystemConfigs()) {

                model.addRow(new Object[]{
                        entity.getName(),
                        post.getName(),
                        post.getIP_Address(),
                        post.getPort()
                });
            }
        }
    }

    private void saveData() {

        int rowIndex = 0;

        for (S_EntityConfiguration entity :
                config.getList_SystemConfigs()) {

            for (S_PostConfiguration post :
                    entity.getList_SystemConfigs()) {

                post.setIP_Address(
                        model.getValueAt(rowIndex,2).toString()
                );

                post.setPort(
                        Integer.parseInt(
                                model.getValueAt(rowIndex,3).toString()
                        )
                );

                rowIndex++;
            }
        }

        config.marshal();

        JOptionPane.showMessageDialog(
                this,
                "Entity Configuration Saved"
        );
    }
}

One Change Needed

Your SystemConfigXML currently has:

@XmlElement(name = "Entity")
private ArrayList<S_EntityConfiguration> list_SystemConfigs
        = new ArrayList<>();

Add getter:

public ArrayList<S_EntityConfiguration> getList_SystemConfigs() {
    return list_SystemConfigs;
}
----------

package configui;

import javax.swing.*;
import java.awt.*;

public class ConfigEditorFrame extends JFrame {

    private EntityPanel entityPanel;
    private DatabasePanel databasePanel;
    private OtherParamPanel otherParamPanel;
    private ESMPanel esmPanel;
    private JammerPanel jammerPanel;

    public ConfigEditorFrame() {

        setTitle("System Configuration");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane mainTabs = new JTabbedPane();

        entityPanel = new EntityPanel();
        databasePanel = new DatabasePanel();
        otherParamPanel = new OtherParamPanel();
        esmPanel = new ESMPanel();
        jammerPanel = new JammerPanel();

        mainTabs.addTab("Entity", entityPanel);
        mainTabs.addTab("Database", databasePanel);
        mainTabs.addTab("Other Param", otherParamPanel);
        mainTabs.addTab("ESM", esmPanel);
        mainTabs.addTab("Jammer", jammerPanel);

        JButton saveBtn = new JButton("Save Configuration");

        saveBtn.addActionListener(e -> {

            ConfigController.saveEntity(entityPanel);
            ConfigController.saveDatabase(databasePanel);
            ConfigController.saveOtherParam(otherParamPanel);
            ConfigController.saveESM(esmPanel);
            ConfigController.saveJammer(jammerPanel);

            SystemConfigXML.getInstance().marshal();

            JOptionPane.showMessageDialog(
                    this,
                    "Configuration Saved Successfully");
        });

        add(mainTabs, BorderLayout.CENTER);
        add(saveBtn, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {

        SystemConfigXML.getInstance().unMarshall();

        entityPanel.loadData();
        databasePanel.loadData();
        otherParamPanel.loadData();
        esmPanel.loadData();
        jammerPanel.loadData();
    }
}


package configui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class EntityPanel extends JPanel {

    private JTabbedPane entityTabs = new JTabbedPane();

    public EntityPanel() {

        setLayout(new BorderLayout());

        addEntityTab("CC");
        addEntityTab("RDFS1");
        addEntityTab("RDFS2");
        addEntityTab("RDFS3");
        addEntityTab("JSHF");
        addEntityTab("JSVU1");
        addEntityTab("JSVU2");
        addEntityTab("JSVU3");
        addEntityTab("JSVU4");
        addEntityTab("SVLRR");
        addEntityTab("SAJ");
        addEntityTab("GPS");

        add(entityTabs);
    }

    private void addEntityTab(String entityName) {

        String[] cols = {
                "Post Name",
                "IP Address",
                "Port"
        };

        JTable table =
                new JTable(new DefaultTableModel(cols,0));

        JScrollPane pane =
                new JScrollPane(table);

        entityTabs.addTab(entityName,pane);
    }

    public void loadData() {

        SystemConfigXML cfg =
                SystemConfigXML.getInstance();

        for(int i=0;i<entityTabs.getTabCount();i++) {

            String entityName =
                    entityTabs.getTitleAt(i);

            S_EntityConfiguration entity =
                    cfg.getEntityConfigObj(entityName);

            JScrollPane scroll =
                    (JScrollPane)entityTabs.getComponentAt(i);

            JTable table =
                    (JTable)scroll.getViewport().getView();

            DefaultTableModel model =
                    (DefaultTableModel)table.getModel();

            model.setRowCount(0);

            for(S_PostConfiguration post :
                    entity.getList_SystemConfigs()) {

                model.addRow(new Object[]{
                        post.getName(),
                        post.getIP_Address(),
                        post.getPort()
                });
            }
        }
    }

    public JTabbedPane getEntityTabs() {
        return entityTabs;
    }
}

package configui;

import javax.swing.*;
import java.awt.*;

public class DatabasePanel extends JPanel {

    JTextField driver = new JTextField();
    JTextField url = new JTextField();
    JTextField username = new JTextField();
    JTextField password = new JTextField();
    JTextField port = new JTextField();

    public DatabasePanel() {

        setLayout(new GridLayout(20,2,5,5));

        add(new JLabel("DB Driver"));
        add(driver);

        add(new JLabel("DB URL"));
        add(url);

        add(new JLabel("DB Username"));
        add(username);

        add(new JLabel("DB Password"));
        add(password);

        add(new JLabel("DB Port"));
        add(port);
    }

    public void loadData() {

        S_DBConfiguration db =
                SystemConfigXML.getInstance()
                        .getDbConfigObj();

        driver.setText(db.getDb_DRIVER());
        url.setText(db.getDb_URL());
        username.setText(db.getDb_USERNAME());
        password.setText(db.getDb_PASSWORD());
        port.setText(String.valueOf(db.getDb_PORT()));
    }
}

package configui;

import javax.swing.*;
import java.awt.*;

public class OtherParamPanel extends JPanel {

    JTextField crc = new JTextField();
    JTextField bite = new JTextField();
    JTextField blockId = new JTextField();

    public OtherParamPanel() {

        setLayout(new GridLayout(20,2));

        add(new JLabel("CRC Periodicity"));
        add(crc);

        add(new JLabel("BITE Periodicity"));
        add(bite);

        add(new JLabel("Block ID"));
        add(blockId);
    }

    public void loadData() {

        S_OtherParamConfiguration obj =
                SystemConfigXML.getInstance()
                        .getOtherParamConfigObj();

        crc.setText(String.valueOf(
                obj.getCRC_Periodicty_In_Minutes()));

        bite.setText(String.valueOf(
                obj.getBite_Periodicity_In_Minutes()));

        blockId.setText(String.valueOf(
                obj.getBlockID()));
    }
}

package configui;

import javax.swing.*;
import java.awt.*;

public class ESMPanel extends JPanel {

    JTextField recce = new JTextField();
    JTextField monitor = new JTextField();
    JTextField fhList = new JTextField();

    public ESMPanel() {

        setLayout(new GridLayout(10,2));

        add(new JLabel("Recce Max Count"));
        add(recce);

        add(new JLabel("Monitor Max Count"));
        add(monitor);

        add(new JLabel("FH List Max Count"));
        add(fhList);
    }

    public void loadData() {

        S_ESM_Tasking_Configuration esm =
                SystemConfigXML.getInstance()
                        .getEsmTaskingConfigObj();

        recce.setText(
                String.valueOf(
                        esm.getRecceTask_MaxCount()));

        monitor.setText(
                String.valueOf(
                        esm.getMonitorTask_MaxCount()));

        fhList.setText(
                String.valueOf(
                        esm.getFHListTask_MaxCount()));
    }
}


package configui;

import javax.swing.*;
import java.awt.*;

public class JammerPanel extends JPanel {

    JTextField normal = new JTextField();
    JTextField immediate = new JTextField();
    JTextField deception = new JTextField();

    public JammerPanel() {

        setLayout(new GridLayout(10,2));

        add(new JLabel("Normal Jam"));
        add(normal);

        add(new JLabel("Immediate Jam"));
        add(immediate);

        add(new JLabel("Deception Jam"));
        add(deception);
    }

    public void loadData() {

        S_JAMMER_Tasking_Configuration jammer =
                SystemConfigXML.getInstance()
                        .getJammerTaskingConfigObj();

        normal.setText(
                String.valueOf(
                        jammer.getNormalJamTask_MaxCount()));

        immediate.setText(
                String.valueOf(
                        jammer.getImmediateJamTask_MaxCount()));

        deception.setText(
                String.valueOf(
                        jammer.getDeceptionJamTask_MaxCount()));
    }
}

package configui;

public class ConfigController {

    public static void saveDatabase(
            DatabasePanel panel) {

        S_DBConfiguration db =
                SystemConfigXML.getInstance()
                        .getDbConfigObj();

        db.setDb_DRIVER(panel.driver.getText());
        db.setDb_URL(panel.url.getText());
        db.setDb_USERNAME(panel.username.getText());
        db.setDb_PASSWORD(panel.password.getText());
        db.setDb_PORT(
                Integer.parseInt(panel.port.getText()));
    }

    public static void saveOtherParam(
            OtherParamPanel panel) {

        S_OtherParamConfiguration obj =
                SystemConfigXML.getInstance()
                        .getOtherParamConfigObj();

        obj.setCRC_Periodicty_In_Minutes(
                Integer.parseInt(panel.crc.getText()));

        obj.setBite_Periodicity_In_Minutes(
                Integer.parseInt(panel.bite.getText()));

        obj.setBlockID(
                Integer.parseInt(panel.blockId.getText()));
    }

    public static void saveESM(
            ESMPanel panel) {

        S_ESM_Tasking_Configuration esm =
                SystemConfigXML.getInstance()
                        .getEsmTaskingConfigObj();

        esm.setRecceTask_MaxCount(
                Integer.parseInt(panel.recce.getText()));

        esm.setMonitorTask_MaxCount(
                Integer.parseInt(panel.monitor.getText()));

        esm.setFHListTask_MaxCount(
                Integer.parseInt(panel.fhList.getText()));
    }

    public static void saveJammer(
            JammerPanel panel) {

        S_JAMMER_Tasking_Configuration jam =
                SystemConfigXML.getInstance()
                        .getJammerTaskingConfigObj();

        jam.setNormalJamTask_MaxCount(
                Integer.parseInt(panel.normal.getText()));

        jam.setImmediateJamTask_MaxCount(
                Integer.parseInt(panel.immediate.getText()));

        jam.setDeceptionJamTask_MaxCount(
                Integer.parseInt(panel.deception.getText()));
    }

    public static void saveEntity(
            EntityPanel panel) {

        // update JTable rows back into
        // S_PostConfiguration objects
    }
}

