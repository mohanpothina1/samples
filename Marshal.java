What is Marshaling and Unmarshaling?
Marshaling

Converting Java objects → XML file.

Ex

Config config = new Config();
config.setPrimaryIp("192.168.1.10");
config.setPrimaryPort(5432);

becomes
<config>
    <primaryDb>
        <ip>192.168.1.10</ip>
        <port>5432</port>
    </primaryDb>
</config>

This process is called Marshaling.

Unmarshaling

Reading XML file → Java objects.

Example:

<config>
    <primaryDb>
        <ip>192.168.1.10</ip>
        <port>5432</port>
    </primaryDb>
</config>

becomes:

config.getPrimaryDb().getIp();

This process is called Unmarshaling.


Your Save button may generate something like:

<configuration>

    <cc>
        <primaryDb>
            <ip>192.168.1.10</ip>
            <port>5432</port>
        </primaryDb>

        <secondaryDb>
            <ip>192.168.1.11</ip>
            <port>5433</port>
        </secondaryDb>
    </cc>

    <cm>
        <primaryDb>
            <ip>192.168.1.20</ip>
            <port>5432</port>
        </primaryDb>

        <secondaryDb>
            <ip>192.168.1.21</ip>
            <port>5433</port>
        </secondaryDb>
    </cm>

</configuration>


JAXB Model Classes
DbConfig.java


import jakarta.xml.bind.annotation.XmlElement;

public class DbConfig {

    private String ip;
    private int port;

    @XmlElement
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @XmlElement
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

import jakarta.xml.bind.annotation.XmlElement;

public class ModuleConfig {

    private DbConfig primaryDb;
    private DbConfig secondaryDb;

    @XmlElement
    public DbConfig getPrimaryDb() {
        return primaryDb;
    }

    public void setPrimaryDb(DbConfig primaryDb) {
        this.primaryDb = primaryDb;
    }

    @XmlElement
    public DbConfig getSecondaryDb() {
        return secondaryDb;
    }

    public void setSecondaryDb(DbConfig secondaryDb) {
        this.secondaryDb = secondaryDb;
    }
}

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlElement;

@XmlRootElement(name = "configuration")
public class Configuration {

    private ModuleConfig cc;
    private ModuleConfig cm;

    @XmlElement
    public ModuleConfig getCc() {
        return cc;
    }

    public void setCc(ModuleConfig cc) {
        this.cc = cc;
    }

    @XmlElement
    public ModuleConfig getCm() {
        return cm;
    }

    public void setCm(ModuleConfig cm) {
        this.cm = cm;
    }
}

Save Button (Marshaling)
JAXBContext context = JAXBContext.newInstance(Configuration.class);

Marshaller marshaller = context.createMarshaller();
marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

marshaller.marshal(configuration,
        new File("config.xml"));


Load Button (Unmarshaling)
        JAXBContext context = JAXBContext.newInstance(Configuration.class);

Unmarshaller unmarshaller = context.createUnmarshaller();

Configuration config =
        (Configuration) unmarshaller.unmarshal(
                new File("config.xml"));



After loading:

ccPrimaryIpField.setText(
        config.getCc()
              .getPrimaryDb()
              .getIp());

and similarly populate all text fields.


Workflow
Application starts.
Check if config.xml exists.
If exists → Unmarshal XML → Fill all text fields.
User modifies IP/Port values.
User clicks Save.
Marshal Java objects → Generate/update config.xml.

Show message:

Configuration Saved Successfully


-------

When this runs:

SystemConfigXML.getInstance().marshal();

it generates:

<Configuration>

    <Entity name="CC">
        ...
    </Entity>

    <Database_Configuration>
        ...
    </Database_Configuration>

    <Other_Param_Configuration>
        ...
    </Other_Param_Configuration>

</Configuration>


And:

SystemConfigXML.getInstance().unMarshall();

loads the XML back into Java objects.

so probably asking for an external configuration utility that edits only some values and regenerates config.xml


On Startup

Load existing config:

File file = new File("config.xml");

if(file.exists())
{
    SystemConfigXML.getInstance().unMarshall();
}

Fill text fields:

S_DBConfiguration db =
        SystemConfigXML.getInstance().getDbConfigObj();

txtCCPrimaryIP.setText(
        extractIp(db.getPrimary_DB_URL()));

txtCCPrimaryPort.setText(
        String.valueOf(db.getPrimary_DB_PORT()));



Save Button

When Save clicked:


S_DBConfiguration db =
        SystemConfigXML.getInstance().getDbConfigObj();

db.setPrimary_DB_URL(
        txtCCPrimaryIP.getText());

db.setPrimary_DB_PORT(
        Integer.parseInt(txtCCPrimaryPort.getText()));

db.setSecondary_DB_URL(
        txtCCSecondaryIP.getText());

db.setSecondary_DB_PORT(
        Integer.parseInt(txtCCSecondaryPort.getText()));

db.setPrimary_DB2_URL(
        txtCMPrimaryIP.getText());

db.setPrimary_DB2_PORT(
        Integer.parseInt(txtCMPrimaryPort.getText()));

db.setSecondary_DB2_URL(
        txtCMSecondaryIP.getText());

db.setSecondary_DB2_PORT(
        Integer.parseInt(txtCMSecondaryPort.getText()));

SystemConfigXML.getInstance().marshal();


This immediately regenerates:

<Database_Configuration>

    <PRIMARY_DB_URL>
        jdbc:postgresql://192.30.0.10:5441/
    </PRIMARY_DB_URL>

    <PRIMARY_DB_PORT>
        5441
    </PRIMARY_DB_PORT>

    <SECONDARY_DB_URL>
        jdbc:postgresql://192.30.0.11:5441/
    </SECONDARY_DB_URL>

    <SECONDARY_DB_PORT>
        5441
    </SECONDARY_DB_PORT>

</Database_Configuration>


Application Startup
File file = new File("config.xml");

if(file.exists()) {
    SystemConfigXML.getInstance().unMarshall();
}

Load values:

S_DBConfiguration db =
        SystemConfigXML.getInstance().getDbConfigObj();

txtCCPrimaryIp.setText(db.getPrimary_DB_URL());
txtCCPrimaryPort.setText(
        String.valueOf(db.getPrimary_DB_PORT()));



CC Tab
Primary DB
  IP
  Port

Secondary DB
  IP
  Port
CM Tab
Primary DB
  IP
  Port

Secondary DB
  IP
  Port

When user clicks Save:


Java Object  ---> XML
(Marshalling)

When application starts:

XML ---> Java Object
(Unmarshalling)



Step 1: Create Model Classes
ConfigXML.java

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Configuration")
public class ConfigXML {

    private ModuleConfig cc;
    private ModuleConfig cm;

    @XmlElement(name = "CC")
    public ModuleConfig getCc() {
        return cc;
    }

    public void setCc(ModuleConfig cc) {
        this.cc = cc;
    }

    @XmlElement(name = "CM")
    public ModuleConfig getCm() {
        return cm;
    }

    public void setCm(ModuleConfig cm) {
        this.cm = cm;
    }
}

import jakarta.xml.bind.annotation.XmlElement;

public class ModuleConfig {

    private DBConfig primaryDb;
    private DBConfig secondaryDb;

    @XmlElement(name = "PrimaryDB")
    public DBConfig getPrimaryDb() {
        return primaryDb;
    }

    public void setPrimaryDb(DBConfig primaryDb) {
        this.primaryDb = primaryDb;
    }

    @XmlElement(name = "SecondaryDB")
    public DBConfig getSecondaryDb() {
        return secondaryDb;
    }

    public void setSecondaryDb(DBConfig secondaryDb) {
        this.secondaryDb = secondaryDb;
    }
}

import jakarta.xml.bind.annotation.XmlElement;

public class DBConfig {

    private String ip;
    private int port;

    @XmlElement(name = "IP")
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @XmlElement(name = "Port")
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}


Generated XML

When marshaled:

<Configuration>

    <CC>
        <PrimaryDB>
            <IP>192.168.1.10</IP>
            <Port>5441</Port>
        </PrimaryDB>

        <SecondaryDB>
            <IP>192.168.1.11</IP>
            <Port>5441</Port>
        </SecondaryDB>
    </CC>

    <CM>
        <PrimaryDB>
            <IP>192.168.1.12</IP>
            <Port>5441</Port>
        </PrimaryDB>

        <SecondaryDB>
            <IP>192.168.1.13</IP>
            <Port>5441</Port>
        </SecondaryDB>
    </CM>

</Configuration>


Marshalling (Java → XML)

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

public class XMLUtil {

    public static void save(ConfigXML config) {

        try {

            JAXBContext context =
                    JAXBContext.newInstance(ConfigXML.class);

            Marshaller marshaller =
                    context.createMarshaller();

            marshaller.setProperty(
                    Marshaller.JAXB_FORMATTED_OUTPUT,
                    true);

            marshaller.marshal(
                    config,
                    new java.io.File("config.xml"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


ConfigXML config = new ConfigXML();

DBConfig ccPrimary = new DBConfig();
ccPrimary.setIp("192.168.1.10");
ccPrimary.setPort(5441);

DBConfig ccSecondary = new DBConfig();
ccSecondary.setIp("192.168.1.11");
ccSecondary.setPort(5441);

ModuleConfig cc = new ModuleConfig();
cc.setPrimaryDb(ccPrimary);
cc.setSecondaryDb(ccSecondary);

config.setCc(cc);

XMLUtil.save(config);

Unmarshalling (XML → Java)


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

public class XMLUtil {

    public static ConfigXML load() {

        try {

            JAXBContext context =
                    JAXBContext.newInstance(ConfigXML.class);

            Unmarshaller unmarshaller =
                    context.createUnmarshaller();

            return (ConfigXML)
                    unmarshaller.unmarshal(
                            new java.io.File("config.xml"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}


Usage:

ConfigXML config = XMLUtil.load();

String ip =
        config.getCc()
              .getPrimaryDb()
              .getIp();

int port =
        config.getCc()
              .getPrimaryDb()
              .getPort();


import javax.swing.*;
import java.awt.*;
import java.io.File;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

public class DBConfigurationUI extends JFrame {

    // CC
    private JTextField ccPrimaryIp;
    private JTextField ccPrimaryPort;
    private JTextField ccSecondaryIp;
    private JTextField ccSecondaryPort;

    // CM
    private JTextField cmPrimaryIp;
    private JTextField cmPrimaryPort;
    private JTextField cmSecondaryIp;
    private JTextField cmSecondaryPort;

    private static final String XML_FILE = "config.xml";

    public DBConfigurationUI() {

        setTitle("Configuration Manager");
        setSize(700, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("CC", createCCTab());
        tabs.addTab("CM", createCMTab());

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveConfiguration());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(saveBtn);

        add(tabs, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        loadConfiguration();
    }

    private JPanel createCCTab() {

        JPanel panel = new JPanel(new GridLayout(2,1,10,10));

        JPanel primaryPanel = new JPanel(new GridLayout(2,2,10,10));
        primaryPanel.setBorder(
                BorderFactory.createTitledBorder("Primary DB"));

        ccPrimaryIp = new JTextField();
        ccPrimaryPort = new JTextField();

        primaryPanel.add(new JLabel("IP Address"));
        primaryPanel.add(ccPrimaryIp);

        primaryPanel.add(new JLabel("Port"));
        primaryPanel.add(ccPrimaryPort);

        JPanel secondaryPanel = new JPanel(new GridLayout(2,2,10,10));
        secondaryPanel.setBorder(
                BorderFactory.createTitledBorder("Secondary DB"));

        ccSecondaryIp = new JTextField();
        ccSecondaryPort = new JTextField();

        secondaryPanel.add(new JLabel("IP Address"));
        secondaryPanel.add(ccSecondaryIp);

        secondaryPanel.add(new JLabel("Port"));
        secondaryPanel.add(ccSecondaryPort);

        panel.add(primaryPanel);
        panel.add(secondaryPanel);

        return panel;
    }

    private JPanel createCMTab() {

        JPanel panel = new JPanel(new GridLayout(2,1,10,10));

        JPanel primaryPanel = new JPanel(new GridLayout(2,2,10,10));
        primaryPanel.setBorder(
                BorderFactory.createTitledBorder("Primary DB"));

        cmPrimaryIp = new JTextField();
        cmPrimaryPort = new JTextField();

        primaryPanel.add(new JLabel("IP Address"));
        primaryPanel.add(cmPrimaryIp);

        primaryPanel.add(new JLabel("Port"));
        primaryPanel.add(cmPrimaryPort);

        JPanel secondaryPanel = new JPanel(new GridLayout(2,2,10,10));
        secondaryPanel.setBorder(
                BorderFactory.createTitledBorder("Secondary DB"));

        cmSecondaryIp = new JTextField();
        cmSecondaryPort = new JTextField();

        secondaryPanel.add(new JLabel("IP Address"));
        secondaryPanel.add(cmSecondaryIp);

        secondaryPanel.add(new JLabel("Port"));
        secondaryPanel.add(cmSecondaryPort);

        panel.add(primaryPanel);
        panel.add(secondaryPanel);

        return panel;
    }

    private void saveConfiguration() {

        try {

            ConfigXML config = new ConfigXML();

            // CC
            DBConfig ccPrimary = new DBConfig();
            ccPrimary.setIp(ccPrimaryIp.getText());
            ccPrimary.setPort(
                    Integer.parseInt(ccPrimaryPort.getText()));

            DBConfig ccSecondary = new DBConfig();
            ccSecondary.setIp(ccSecondaryIp.getText());
            ccSecondary.setPort(
                    Integer.parseInt(ccSecondaryPort.getText()));

            ModuleConfig cc = new ModuleConfig();
            cc.setPrimaryDb(ccPrimary);
            cc.setSecondaryDb(ccSecondary);

            // CM
            DBConfig cmPrimary = new DBConfig();
            cmPrimary.setIp(cmPrimaryIp.getText());
            cmPrimary.setPort(
                    Integer.parseInt(cmPrimaryPort.getText()));

            DBConfig cmSecondary = new DBConfig();
            cmSecondary.setIp(cmSecondaryIp.getText());
            cmSecondary.setPort(
                    Integer.parseInt(cmSecondaryPort.getText()));

            ModuleConfig cm = new ModuleConfig();
            cm.setPrimaryDb(cmPrimary);
            cm.setSecondaryDb(cmSecondary);

            config.setCc(cc);
            config.setCm(cm);

            JAXBContext context =
                    JAXBContext.newInstance(ConfigXML.class);

            Marshaller marshaller =
                    context.createMarshaller();

            marshaller.setProperty(
                    Marshaller.JAXB_FORMATTED_OUTPUT,
                    true);

            marshaller.marshal(config,
                    new File(XML_FILE));

            JOptionPane.showMessageDialog(
                    this,
                    "Configuration Saved Successfully");

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage());

            ex.printStackTrace();
        }
    }

    private void loadConfiguration() {

        try {

            File file = new File(XML_FILE);

            if (!file.exists()) {
                return;
            }

            JAXBContext context =
                    JAXBContext.newInstance(ConfigXML.class);

            Unmarshaller unmarshaller =
                    context.createUnmarshaller();

            ConfigXML config =
                    (ConfigXML) unmarshaller.unmarshal(file);

            // CC
            ccPrimaryIp.setText(
                    config.getCc().getPrimaryDb().getIp());

            ccPrimaryPort.setText(
                    String.valueOf(
                            config.getCc()
                                    .getPrimaryDb()
                                    .getPort()));

            ccSecondaryIp.setText(
                    config.getCc().getSecondaryDb().getIp());

            ccSecondaryPort.setText(
                    String.valueOf(
                            config.getCc()
                                    .getSecondaryDb()
                                    .getPort()));

            // CM
            cmPrimaryIp.setText(
                    config.getCm().getPrimaryDb().getIp());

            cmPrimaryPort.setText(
                    String.valueOf(
                            config.getCm()
                                    .getPrimaryDb()
                                    .getPort()));

            cmSecondaryIp.setText(
                    config.getCm().getSecondaryDb().getIp());

            cmSecondaryPort.setText(
                    String.valueOf(
                            config.getCm()
                                    .getSecondaryDb()
                                    .getPort()));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() ->
                new DBConfigurationUI().setVisible(true));
    }
}


---------------

ConfigEditor/
│
├── src/
│   └── com/company/configeditor/
│
│       ├── ui/
│       │   └── DBConfigurationUI.java
│       │
│       ├── model/
│       │   ├── ConfigXML.java
│       │   ├── ModuleConfig.java
│       │   └── DBConfig.java
│       │
│       ├── service/
│       │   └── ConfigService.java
│       │
│       └── Main.java
│
├── config.xml
│
└── lib/
    └── jakarta.xml.bind-api.jar





    DBConfig.java
public class DBConfig {
    private String ip;
    private int port;

    // getters setters
}



ModuleConfig.java
public class ModuleConfig {

    private DBConfig primaryDb;
    private DBConfig secondaryDb;

    // getters setters
}



ConfigXML.java
@XmlRootElement(name = "Configuration")
public class ConfigXML {

    private ModuleConfig cc;
    private ModuleConfig cm;

    // getters setters
}



ConfigService.java

Handles:

XML → Java
Java → XML

Example:

public class ConfigService {

    private static final String FILE_NAME = "config.xml";

    public static ConfigXML load() {
        ...
    }

    public static void save(ConfigXML config) {
        ...
    }
}


Contains:

Unmarshaller
Marshaller

logic.

3. ui package
DBConfigurationUI.java

Contains:

JFrame
JTabbedPane
JTextFields
JButtons



4. Main.java

Application entry point.

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new DBConfigurationUI().setVisible(true);
        });

    }
}


In this case:

SystemConfigXML.getInstance().unMarshall();

loads existing XML.

UI updates:

config.getDbConfigObj()
      .setPrimary_DB_URL(...);

and Save does:

config.marshal();


-------------


If your XML is:

<Configuration>
    <CC>
        <PrimaryDB>
            <IP>192.30.0.10</IP>
            <Port>5441</Port>
        </PrimaryDB>
        <SecondaryDB>
            <IP>192.30.0.11</IP>
            <Port>5441</Port>
        </SecondaryDB>
    </CC>

    <CM>
        <PrimaryDB>
            <IP>192.30.0.12</IP>
            <Port>5441</Port>
        </PrimaryDB>
        <SecondaryDB>
            <IP>192.30.0.13</IP>
            <Port>5441</Port>
        </SecondaryDB>
    </CM>
</Configuration>

Then classes are typically:

ConfigXML
 ├── CC (ModuleConfig)
 │     ├── PrimaryDB (DBConfig)
 │     └── SecondaryDB (DBConfig)
 │
 └── CM (ModuleConfig)
       ├── PrimaryDB (DBConfig)
       └── SecondaryDB (DBConfig)
Why?

Because JAXB needs to know:

<IP>192.30.0.10</IP>

maps to

private String ip;

and

<Port>5441</Port>

maps to

private int port;

through annotations:

@XmlElement(name = "IP")
public String getIp() {
    return ip;
}
In Your Existing Project

Look at your class:

public class S_PostConfiguration {

    private String iP_Address;
    private int port;

    @XmlElement(name = "IP_Address")
    public String getIP_Address() {
        return iP_Address;
    }

    @XmlElement(name = "Port")
    public int getPort() {
        return port;
    }
}

This already maps:

<Post>
    <IP_Address>192.30.0.20</IP_Address>
    <Port>5000</Port>
</Post>

to

S_PostConfiguration
So for your new UI

You first need to see what the final config.xml should look like.

For example, if your sir wants:

<Configuration>
    <CC>
        <PrimaryDB>
            <IP>192.30.0.10</IP>
            <Port>5441</Port>
        </PrimaryDB>
    </CC>
</Configuration>

then create matching classes.

If your sir already has a predefined XML format, then create Java classes that mirror that structure exactly.

Development Order
Decide XML structure.
Create Java model classes matching XML.
Add JAXB annotations (@XmlRootElement, @XmlElement, @XmlAttribute).
Test:
XML → Object (unmarshal)
Object → XML (marshal)
Build Swing UI.
Load object values into UI.
Save UI values back to object.
Marshal object to XML.

This is the same pattern used by your existing SystemConfigXML, S_DBConfiguration, S_EntityConfiguration, and S_PostConfiguration classes. They are essentially Java representations of the XML structure.

then how load in to UI and how save UI values back to xml

This is the key concept. Think of the UI as sitting between the XML and Java objects.

Loading XML into UI
Step 1: Read XML
ConfigXML config = XMLUtil.load();

After unmarshalling:

config
 ├── CC
 │    ├── PrimaryDB
 │    │      ├── ip = 192.30.0.10
 │    │      └── port = 5441
 │    └── SecondaryDB
 │           ├── ip = 192.30.0.11
 │           └── port = 5441
Step 2: Set UI Fields

Suppose you have:

private JTextField txtCCPrimaryIp;
private JTextField txtCCPrimaryPort;
private JTextField txtCCSecondaryIp;
private JTextField txtCCSecondaryPort;

Load values:

txtCCPrimaryIp.setText(
        config.getCc()
              .getPrimaryDb()
              .getIp());

txtCCPrimaryPort.setText(
        String.valueOf(
                config.getCc()
                      .getPrimaryDb()
                      .getPort()));

txtCCSecondaryIp.setText(
        config.getCc()
              .getSecondaryDb()
              .getIp());

txtCCSecondaryPort.setText(
        String.valueOf(
                config.getCc()
                      .getSecondaryDb()
                      .getPort()));

Now UI shows:

CC

Primary DB
IP   : 192.30.0.10
Port : 5441

Secondary DB
IP   : 192.30.0.11
Port : 5441
Saving UI values back to XML

Suppose user changes:

Primary DB IP

192.30.0.10
      ↓
192.40.0.10
Step 1: Read UI Values
String ip =
        txtCCPrimaryIp.getText();

int port =
        Integer.parseInt(
                txtCCPrimaryPort.getText());
Step 2: Update Object
config.getCc()
      .getPrimaryDb()
      .setIp(ip);

config.getCc()
      .getPrimaryDb()
      .setPort(port);

Now Java object contains:

PrimaryDB
{
    ip = "192.40.0.10";
    port = 5441;
}
Step 3: Marshal
XMLUtil.save(config);

or

config.marshal();

JAXB automatically writes:

<PrimaryDB>
    <IP>192.40.0.10</IP>
    <Port>5441</Port>
</PrimaryDB>

to config.xml.

Real Swing Example
Load
private void loadConfig() {

    ConfigXML config = XMLUtil.load();

    txtCCPrimaryIp.setText(
            config.getCc()
                  .getPrimaryDb()
                  .getIp());

    txtCCPrimaryPort.setText(
            String.valueOf(
                    config.getCc()
                          .getPrimaryDb()
                          .getPort()));
}

Call:

loadConfig();

inside constructor.

Save Button
btnSave.addActionListener(e -> saveConfig());
Save Method
private void saveConfig() {

    ConfigXML config = XMLUtil.load();

    config.getCc()
          .getPrimaryDb()
          .setIp(
                  txtCCPrimaryIp.getText());

    config.getCc()
          .getPrimaryDb()
          .setPort(
                  Integer.parseInt(
                          txtCCPrimaryPort.getText()));

    XMLUtil.save(config);

    JOptionPane.showMessageDialog(
            this,
            "Configuration Saved");
}
In Your Existing Project

Since you already have:

SystemConfigXML
S_DBConfiguration

you would do:

Load
SystemConfigXML config =
        SystemConfigXML.getInstance()
                       .unMarshall();

S_DBConfiguration db =
        config.getDbConfigObj();

txtPrimaryIp.setText(
        db.getPrimary_DB_URL());

txtPrimaryPort.setText(
        String.valueOf(
                db.getPrimary_DB_PORT()));
Save
db.setPrimary_DB_URL(
        txtPrimaryIp.getText());

db.setPrimary_DB_PORT(
        Integer.parseInt(
                txtPrimaryPort.getText()));

config.marshal();
Simple Flow
config.xml
    ↓
Unmarshal
    ↓
Java Objects
    ↓
setText(...)
    ↓
UI

User edits values

UI
    ↓
getText(...)
    ↓
Java Objects
    ↓
Marshal
    ↓
config.xml

That's the complete cycle every configuration editor follows.