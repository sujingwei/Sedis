package top.aoae.sedis.ui;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;

/**
 * 连接窗口
 */
public class ConnectDialog extends JDialog implements ActionListener {
    /**
     * 父窗口
     */
    private MainFrame frame;
    /**
     * 选择历史连接记录
     */
    private JComboBox selectConnectionComboBox = new JComboBox();

    private JTextField nameJTextField = new JTextField(28);
    private JTextField hostJTextField = new JTextField(28);
    private JTextField portJTextField = new JTextField("6379", 28);
    private JPasswordField passwordJTextField = new JPasswordField(28);
    private JCheckBox isSaveJCheckbox = new JCheckBox();
    private JButton connectBtn = new JButton("Connect");

    public ConnectDialog(MainFrame frame) {
        super(frame, "Redis连接配置", true);
        this.frame = frame;
        this.setSize(430, 260);
        this.setLocationRelativeTo(null); // 窗口居中
        this.setResizable(false); // false-窗口不能最大化
        FlowLayout f = new FlowLayout();
        f.setAlignment(FlowLayout.LEFT);
        this.setLayout(new GridLayout(9, 1, 3, 3));

        connectBtn.addActionListener(this);
        this.selectConnectionComboBoxInit();  // 初始化连接记录选择

        this.add(new FromItem(new JLabel(formatFull("Old:")), selectConnectionComboBox));
        this.add(new FromItem(new JLabel(formatFull("Name:")), nameJTextField));
        this.add(new FromItem(new JLabel(formatFull("Host:")), hostJTextField));
        this.add(new FromItem(new JLabel(formatFull("Port:")), portJTextField));
        this.add(new FromItem(new JLabel(formatFull("Password:")), passwordJTextField));
        this.add(getCheckboxPanel());
        this.add(new FromItem(new JLabel(formatFull("")), connectBtn));
    }

    private String formatFull(String str) {
        return String.format("%12s", str);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            String txt = ((JButton) e.getSource()).getText();
            if ("Connect".equals(txt)) {
                String name = nameJTextField.getText();
                if (StringUtils.isBlank(name)) {
                    JOptionPane.showMessageDialog(this, "Name为空");
                    return;
                }
                if (MainFrame.connectMap.containsKey(name)) {
                    JOptionPane.showMessageDialog(this, "Name已存在");
                    return;
                }
                String host = hostJTextField.getText();
                if (StringUtils.isBlank(host)) {
                    JOptionPane.showMessageDialog(this, "host为空");
                    return;
                }
                Jedis jedis;
                try {
                    String port = portJTextField.getText();
                    if (StringUtils.isNotBlank(port)) {
                        jedis = new Jedis(host, Integer.valueOf(port), 10000);
                    } else {
                        jedis = new Jedis(host, 6379, 10000);
                    }

                    char[] password = passwordJTextField.getPassword();
                    String pass = null;
                    if (password != null && password.length > 0) {
                        pass = new String(password);
                        jedis.auth(pass); // 设置密码
                    }
                    jedis.connect();
                    if (!jedis.isConnected()) {
                        JOptionPane.showMessageDialog(this, "redis连接失败");
                        return;
                    }
                    // 登录信息
                    String loginMsg = String.format("%s %s %s",
                            host,
                            StringUtils.isBlank(port) ? "null" : port,
                            StringUtils.isBlank(pass) ? "null" : pass);
                    if (isSaveJCheckbox.isSelected()) {
                        // 保存登录信息
                        writeLoginMessage(name, loginMsg);
                    }
                    // 创建 tab窗口
                    this.frame.addTab(name, jedis, loginMsg);
                    this.setVisible(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                }
            }
        }
    }

    private class FromItem extends JPanel {
        public FromItem(JLabel label, Component component) {
            this.setLayout(new BorderLayout());
            this.add(label, BorderLayout.WEST);
            this.add(component, BorderLayout.EAST);
        }
    }

    private JPanel getCheckboxPanel() {
        FlowLayout f = new FlowLayout();
        f.setAlignment(FlowLayout.LEFT);
        JPanel p = new JPanel(f);
        p.add(new JLabel(formatFull("isSave:")));
        p.add(isSaveJCheckbox);  // 是否保存登录数据
        return p;
    }

    /**
     * 获取连接信息保存路径
     *
     * @return
     */
    private File getLoginPath() {
        String userHome = System.getProperty("user.home");
        File path = new File(userHome, ".Sedis");
        if (!path.exists()) {
            path.mkdirs();
        }
        return path;
    }

    /**
     * 得到保存登录信息的文件
     * - 每一个连接，就一份文件
     *
     * @return
     */
    private File getLoginFile(String name) {
        name += ".d";
        File file = new File(getLoginPath(), name);
        return file;
    }

    /**
     * 记录登录信息
     *
     * @param name
     * @param msg
     * @throws IOException
     */
    private void writeLoginMessage(String name, String msg) throws IOException {
        File file = getLoginFile(name);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(msg);
        fileWriter.close();
    }

    /**
     * 初始化selectConnectionComboBox组件
     */
    public void selectConnectionComboBoxInit() {
        selectConnectionComboBox.addItem("选择历史连接记录");
        File path = getLoginPath();
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                if (files[i].getName().endsWith(".d")) {
                    String name = files[i].getName();
                    selectConnectionComboBox.addItem(name.substring(0, name.length() - 2));
                }
            }
        }
        selectConnectionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String name = e.getItem().toString();
                    if ("选择历史连接记录".equals(name)) {
                        nameJTextField.setText("");
                        hostJTextField.setText("");
                        portJTextField.setText("6379");
                        passwordJTextField.setText("");
                        return;
                    }
                    File readFile = new File(getLoginPath(), name + ".d");
                    if (!readFile.exists()) {
                        JOptionPane.showMessageDialog(null, "记录不存在");
                        return;
                    }
                    Long fileLength = readFile.length();
                    byte[] fileContent = new byte[fileLength.intValue()];
                    try {
                        FileInputStream in = new FileInputStream(readFile);
                        in.read(fileContent);
                        in.close();
                        String s = new String(fileContent, "UTF-8");
                        System.out.println(s);
                        String[] split = s.split(" ");
                        if (split.length >=3) {
                            nameJTextField.setText(name);
                            hostJTextField.setText(split[0]);
                            portJTextField.setText("null".equalsIgnoreCase(split[1]) ? "" : split[1]);
                            passwordJTextField.setText("null".equalsIgnoreCase(split[2]) ? "" : split[2]);
                        } else {
                            JOptionPane.showMessageDialog(frame, "缓存连接信息异常");
                        }
                    } catch (FileNotFoundException fileNotFoundException) {
                        fileNotFoundException.printStackTrace();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });
    }
}
