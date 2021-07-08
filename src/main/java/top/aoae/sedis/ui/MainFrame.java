package top.aoae.sedis.ui;


import org.apache.commons.lang3.StringUtils;
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
     * 用于保存当前连接的信息，便于故障恢复
     */
    private static Map<String, String> connectInfoMap = new LinkedHashMap<>();

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
    public void addTab(String name, Jedis jedis, String connectInfo) {
        connectMap.put(name, jedis);            // 保存jedis连接对象
        connectInfoMap.put(name, connectInfo);  // 保存jedis连接信息
        tabbedPane.addTab(name, new TabPanel(this, name));
    }


    /**
     * 打开Redis连接对话框
     */
    public void openRedisConnectionDialog() {
        if (connectDialog == null) {
            connectDialog = new ConnectDialog(this);
        }
        connectDialog.setVisible(true);
    }

    /**
     * jedis连接，的故障恢复操作
     */
    public void jedisRestoration(){
        new Thread(){
            @Override
            public void run() {
                while (true) {
                    if (!connectMap.isEmpty()) {
                        for (String key : connectMap.keySet()) {
                            Jedis jedis = connectMap.get(key);
                            if (jedis == null || !jedis.isConnected()) {
                                System.out.println(":( -- " + key + " 连接断开 ---------------------------");
                                if (connectInfoMap.containsKey(key)) {
                                    String info = connectInfoMap.get(key);
                                    if(!StringUtils.isEmpty(info)) {
                                        String[] split = info.split(" ");
                                        if ("null".equalsIgnoreCase(split[1])) {
                                            split[1] = "6379";
                                        }
                                        jedis = new Jedis(split[0], Integer.valueOf(split[1]), 10000);
                                        if (!"null".equalsIgnoreCase(split[2])) {
                                            jedis.auth(split[2]);
                                        }
                                        connectMap.put(key, jedis);
                                        System.out.println(":) -- " + key + " 重建连接 ---------------------------");
                                    }
                                }
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // System.out.println(":) -- 连接失效恢复线程 ---------------------------");
                }
            }
        }.start();

    }

}
