package top.aoae.sedis.ui;


import redis.clients.jedis.Jedis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 主窗口
 */
public class MainFrame extends JFrame implements ActionListener {

    /**
     * 添加连接/打开连接窗口
     */
    private JButton addConnect = null;

    /**
     * tab标签页面
     */
    public JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * jedis对象
     */
    public static Map<String, Jedis> connectMap = new LinkedHashMap<>();

    /**
     * Redis连接窗口
     */
    private ConnectDialog connectDialog = null;

    public MainFrame() {
        this.setTitle("Sedis 1.0");
        this.setSize(860, 540);
        this.setLocationRelativeTo(null); // 窗口居中
        this.setResizable(false); // false-窗口不能最大化
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        addConnect = new JButton("连接", new ImageIcon(MainFrame.class.getClassLoader().getResource("connection.png")));
        addConnect.addActionListener(this);
        toolBar.add(addConnect);

        JButton b2 = new JButton("断开", new ImageIcon(MainFrame.class.getClassLoader().getResource("disConnection.png")));
        JButton b3 = new JButton("全部断开", new ImageIcon(MainFrame.class.getClassLoader().getResource("disAllConnection.png")));
        b2.addActionListener(this);
        b3.addActionListener(this);
        toolBar.add(b2);
        toolBar.add(b3);

        this.setLayout(new BorderLayout());
        this.add(toolBar, BorderLayout.NORTH);
        this.add(tabbedPane, BorderLayout.CENTER);

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            String txt = ((JButton) e.getSource()).getText();
            if ("连接".equals(txt)) {
                openRedisConnectionDialog();
            } else if ("断开".equals(txt)) {
                String key = tabbedPane.getSelectedComponent().getName();
                if (connectMap.containsKey(key)) {
                    connectMap.get(key).disconnect(); // 判开连接
                    connectMap.remove(key);
                }

                // 当前选中的标签页
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex != -1) {
                    System.out.println(selectedIndex);
                    tabbedPane.remove(selectedIndex);
                }
            } else if ("全部断开".equals(txt)) {
                if (!connectMap.isEmpty()) {
                    for (String key : connectMap.keySet()) {
                        connectMap.get(key).disconnect();
                    }
                    connectMap.clear();
                }
                tabbedPane.removeAll();
            }

        }
    }

    /**
     * 添加标签页面
     *
     * @param name
     * @param jedis
     */
    public void addTab(String name, Jedis jedis) {
        connectMap.put(name, jedis);
        tabbedPane.addTab(name, new TabPanel(this, name));
    }


    /**
     * 打开Redis连接对话框
     */
    public void openRedisConnectionDialog() {
        if(connectDialog == null) {
            connectDialog = new ConnectDialog(this);
        }
        connectDialog.setVisible(true);
    }


}
