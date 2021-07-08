package top.aoae.sedis.ui;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import top.aoae.sedis.ui.detail.ListPanel;
import top.aoae.sedis.ui.detail.StringPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.Set;

/**
 * 标签页的核心内容，包含key列表及key的操作
 */
public class TabPanel extends JPanel implements ActionListener {

    /**
     * 标签页面的名称,用于查询集合中的Redis
     */
    private String name;

    /**
     * 父窗口
     */
    private JFrame frame;

    /**
     * keyTable
     */
    private JTable keyTable = new JTable();

    /**
     * keyTable 表头
     */
    private Object[] keyColumnTitle = {"key"};

    /**
     * 搜索框
     */
    private JTextField so = new JTextField("<Enter>搜索");

    /**
     * db选择器
     */
    private JComboBox selectDBComboBox = new JComboBox();

    /**
     * 加载按键
     */
    private JButton loadDataBtn = new JButton("加载数据(Load Data)");

    /**
     * 卡片布局（双击key后，针对不同的key类型，显示不同的卡片面板）
     */
    private CardLayout cardLayout;
    /**
     * 卡片布局所在的主面板
     */
    private JPanel valueCardPanel;

    /**
     * 字符串面板
     */
    private StringPanel stringPanel = null;

    /**
     * list集合面板
     */
    private ListPanel listPanel = null;
    /**
     * set集合面板
     */
    private ListPanel setPanel = null;

    /**
     * hash集合面板
     */
    private ListPanel hashPanel = null;

    /**
     * zset集合面板
     */
    private ListPanel zsetPanel = null;

    /**
     * 添加key窗口
     */
    private AddDialog addDialog;


    /**
     * 返回Jedis对象
     *
     * @return
     */
    private Jedis getJedis() {
        if (!StringUtils.isEmpty(name) && MainFrame.connectMap.containsKey(name)) {
            Jedis jedis = MainFrame.connectMap.get(name);
            if (jedis!=null && jedis.isConnected()) {
                return jedis;
            }
        }
        return null;
    }

    public TabPanel(JFrame frame, String name) {
        this.name = name;
        this.frame = frame;
        this.setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.add(northPanel(), BorderLayout.NORTH);
        this.add(splitPane, BorderLayout.CENTER);
        splitPane.add(leftPanel());

        // 初始化卡片布局
        getContentPanel();
        splitPane.add(valueCardPanel);

        // 添加 key 窗口
        addDialog = new AddDialog(frame, this, name);

        /**
         * 打开页面后，载数据
         */
        updateKeyTableData();
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取一个新的面板
     *
     * @param layout
     * @return
     */
    private JPanel getPanel(LayoutManager layout) {
        JPanel p = new JPanel();
        if (layout == null) {
            p.setLayout(new BorderLayout());
        } else {
            p.setLayout(layout);
        }
        return p;
    }

    private JPanel northPanel() {
        JPanel p = getPanel(null);
        if (isCluster()) {
            // 如果当前连接是一个集群的话，那就只显示1个数据库，db0
            selectDBComboBox.addItem(String.valueOf(0));
        } else {
            for (int i = 0; i < 16; i++)
                selectDBComboBox.addItem(String.valueOf(i));
        }
        selectDBComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    System.out.println(e.getItem().toString());
                }
            }
        });

        /**
         * 搜索框焦点
         */
        so.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (so.getText().equals("<Enter>搜索")) {
                    so.setText("");     //将提示文字清空
                    so.setForeground(Color.BLACK);  //设置用户输入的字体颜色为黑色
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (so.getText().equals("")) {
                    so.setText("<Enter>搜索");     //将提示文字清空
                    so.setForeground(Color.GRAY);  //设置用户输入的字体颜色为灰色
                }
            }
        });
        /**
         * 搜索条回车事件
         */
        so.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                     updateKeyTableData();
                }
            }
        });
        so.setForeground(Color.GRAY);

        JButton cleanBtn = new JButton("clean");
        cleanBtn.addActionListener(this);

        p.add(so, BorderLayout.CENTER);
        p.add(cleanBtn, BorderLayout.EAST);
        p.add(selectDBComboBox, BorderLayout.WEST);
        return p;
    }

    /**
     * 左边面板
     *
     * @return
     */
    private JPanel leftPanel() {
        JPanel p = getPanel(null);
        keyTable.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        keyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 禁止多选
        keyTable.setModel(getEmptyTableModel());
        keyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                /* 双击表格的行 */
                if (e.getClickCount() == 2) {
                    Jedis jedis = getJedis();
                    int rowCount = keyTable.getSelectedRow();
                    Object key = keyTable.getValueAt(rowCount, 0);
                    if (key == null) {
                        JOptionPane.showMessageDialog(p, "获取key的失败");
                        return;
                    }
                    keyTableRowMouseClicked((String) key);
                }
            }
        });
        p.add(new JLabel("<双击Key>显示详情"), BorderLayout.NORTH);
        p.add(new JScrollPane(keyTable), BorderLayout.CENTER);

        JButton delKeyBtn = new JButton("-");
        JButton addKeyBtn = new JButton("+");
        JPanel f = getPanel(new FlowLayout(FlowLayout.LEFT));
        f.add(loadDataBtn);
        f.add(addKeyBtn);
        f.add(delKeyBtn);
        loadDataBtn.addActionListener(this);
        delKeyBtn.addActionListener(this);
        addKeyBtn.addActionListener(this);
        p.add(f, BorderLayout.SOUTH);
        return p;
    }


    /**
     * 双击表示行(key)，加载key数据
     */
    private void keyTableRowMouseClicked (String key) {
        new Thread(){
            @Override
            public void run() {
                Jedis jedis = getJedis();
                String type = jedis.type(key);
                if (StringUtils.isEmpty(type)) {
                    JOptionPane.showMessageDialog(frame, "获取key的类型失败");
                    return;
                }
                if (type.equals("string")) {
                    stringPanel.setDate((String) key, type);
                    stringPanel.updateDate(); // 显示redis
                } else if (type.equals("list")) {
                    listPanel.setDate((String) key, type);
                    listPanel.updateDate(); // 显示数据
                } else if (type.equals("set")) {
                    setPanel.setDate((String) key, type);
                    setPanel.updateDate(); // 显示数据
                } else if (type.equals("hash")) {
                    hashPanel.setDate((String) key, type);
                    hashPanel.updateDate(); // 显示数据
                } else if (type.equals("zset")) {
                    zsetPanel.setDate((String) key, type);
                    zsetPanel.updateDate(); // 显示数据
                }

                cardLayout.show(valueCardPanel, type);
            }
        }.start();
    }

    /**
     * 返回一个空的DefaultTableModel
     *
     * @return
     */
    private DefaultTableModel getEmptyTableModel() {
        String[][] content = new String[0][keyColumnTitle.length];
        return new DefaultTableModel(content, keyColumnTitle);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            String txt = ((JButton) e.getSource()).getText();
            if ("加载数据(Load Data)".equalsIgnoreCase(txt)) {
                updateKeyTableData();
            } else if ("+".equalsIgnoreCase(txt)) {
                this.addDialog.setVisible(true);
            } else if ("-".equalsIgnoreCase(txt)) {
                deleteKeyAction();
            } else if ("clean".equalsIgnoreCase(txt)) {
                cleanSo();
            }
        }
    }


    /**
     * 调用线程，返回redis的key列表
     */
    public void updateKeyTableData() {
        Jedis jedis = getJedis();
        if (jedis == null || !jedis.isConnected()) {
            JOptionPane.showMessageDialog(frame, "连接断开");
        }
        if (jedis != null && jedis.isConnected()) {
            new Thread() {
                @Override
                public void run() {
                    DefaultTableModel emptyTableModel = getEmptyTableModel();
                    keyTable.setModel(emptyTableModel);
                    loadDataBtn.setEnabled(false);
                    String soText = so.getText();
                    String pattern = "*";
                    if (!StringUtils.isBlank(soText) && !"<Enter>搜索".equals(soText)) {
                        pattern = "*" + soText + "*";
                    }
                    try {
                        Set<String> keys = jedis.keys(pattern);
                        if (keys != null && !keys.isEmpty()) {
                            String[][] content = new String[keys.size()][keyColumnTitle.length];
                            Iterator<String> iterator = keys.iterator();
                            int i = 0;
                            while (iterator.hasNext()) {
                                String next = iterator.next();
                                content[i][0] = next;
                                if (i >= 500) {
                                    break;
                                }
                                i++;
                            }
                            DefaultTableModel model = new DefaultTableModel(content, keyColumnTitle) {
                                @Override
                                public boolean isCellEditable(int row, int column) {
                                    return false;
                                }
                            };

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            keyTable.setModel(model);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                    } finally {
                        loadDataBtn.setEnabled(true);
                    }
                }
            }.start();
        } else {
            JOptionPane.showMessageDialog(this, "redis连接失败");
        }
    }

    /**
     * 删除key
     */
    private void deleteKeyAction(){
        int rowCount = keyTable.getSelectedRow();
        if (rowCount < 0) {
            JOptionPane.showMessageDialog(this, "选择key后才可以进行删除");
            return;
        }
        Object key = keyTable.getValueAt(rowCount, 0);
        Jedis jedis = getJedis();
        if (jedis != null && jedis.isConnected()) {
            jedis.del((String) key);
            updateKeyTableData();
        } else {
            JOptionPane.showMessageDialog(this, "redis连接失败");
        }
    }

    /**
     * 判断是否为Redis集群
     *
     * @return
     */
    private boolean isCluster() {
        boolean result = false;
        try {
            Jedis jedis = getJedis();
            if (jedis != null) {
                String s = jedis.clusterInfo();
                if (s != null && s.length() > 0) {
                    result = s.indexOf("cluster_state:ok") >= 0;
                }
            }
        } catch (Exception e) {
            // 不是集群
            System.out.println(e.getMessage());
        }
        return result;
    }

    /**
     * 清空搜索条件
     */
    private void cleanSo(){
        so.setText("<Enter>搜索");     //将提示文字清空
        so.setForeground(Color.GRAY);  //设置用户输入的字体颜色为灰色
        updateKeyTableData();
    }

    /**valueCardPanel
     * 内容使用卡片布局
     */
    private void getContentPanel() {
        JPanel empty = getPanel(null);
        empty.add(new JLabel("Empty Panel"));
        empty.setMinimumSize(new Dimension(500, 300));
        stringPanel = new StringPanel(frame, name);
        listPanel = new ListPanel(frame, name);
        setPanel = new ListPanel(frame, name);
        hashPanel = new ListPanel(frame, name);
        zsetPanel = new ListPanel(frame, name);

        cardLayout = new CardLayout(3, 3);
        valueCardPanel = getPanel(cardLayout);

        valueCardPanel.add(empty, "empty");
        valueCardPanel.add(stringPanel, "string");
        valueCardPanel.add(listPanel, "list");
        valueCardPanel.add(setPanel, "set");  // 集合和有序集合都用你
        valueCardPanel.add(hashPanel, "hash");
        valueCardPanel.add(zsetPanel, "zset");
        cardLayout.show(valueCardPanel, "empty");
    }
}
