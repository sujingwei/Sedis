package top.aoae.sedis.ui.detail;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import top.aoae.sedis.ui.MainFrame;
import top.aoae.sedis.util.JsonFormatUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * 字符串类型的窗口
 */
public class ListPanel extends JPanel implements ActionListener {

    private JFrame frame;
    private String name;  // 通过name可以获取到 jedis 连接
    private String key;
    private String type;


    private JTable value = new JTable();
    private Object[] columnTitle = {"值(Value)"};
    private Object[] hashTitle = {"键(Key)", "值(Value)"};
    private Object[] zsetTitle = {"#", "值(Value)"};
    private JTextArea formatValue = new JTextArea();
    private JLabel detail = new JLabel();
    private JButton toJson = new JButton("toJson");

    public void setDate(String key, String type) {
        this.key = key;
        this.type = type;
    }

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

    public ListPanel(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;
        this.setLayout(new BorderLayout());
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.add(jSplitPane, BorderLayout.CENTER);

        jSplitPane.add(topPanel(), JSplitPane.TOP);

        jSplitPane.add(bottomPanel(), JSplitPane.BOTTOM);
    }

    private JPanel topPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.setMinimumSize(new Dimension(400, 180));
        JPanel f = new JPanel();
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        f.setLayout(flowLayout);
        p.add(f, BorderLayout.NORTH);
        f.add(detail);
        p.add(new JScrollPane(value), BorderLayout.CENTER);
        value.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 禁止多选
        return p;
    }

    private JPanel bottomPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JPanel f = new JPanel();
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        f.setLayout(flowLayout);
        p.add(f, BorderLayout.NORTH);
        f.add(toJson);
        // formatValue.setEnabled(false);
        p.add(new JScrollPane(formatValue), BorderLayout.CENTER);

        toJson.addActionListener(this);
        return p;
    }

    /**
     * 更新数据
     */
    public void updateDate() {
        Jedis jedis = getJedis();
        if (jedis != null && StringUtils.isNotBlank(key) && StringUtils.isNotBlank(type)) {
            new Thread() {
                @Override
                public void run() {
                    detail.setText(String.format("Key: %s, Type: %s", key, type));

                    List<String> data = null;
                    Map<String, String> hashMap = null;
                    Set<String> zrangeSet = null;

                    if ("list".equalsIgnoreCase(type)) {
                        data = jedis.lrange(key, 0, 1000);
                        System.out.println(data);
                    } else if ("set".equalsIgnoreCase(type)) {
                        Set<String> smembers = jedis.smembers(key);
                        System.out.println(smembers);
                        if (smembers != null && !smembers.isEmpty()) {
                            data = new ArrayList<>(smembers);
                        }
                    } else if ("hash".equalsIgnoreCase(type)) {
                        hashMap = jedis.hgetAll(key);
                        System.out.println(hashMap);
                    }  else if ("zset".equalsIgnoreCase(type)) {
                        zrangeSet = jedis.zrange(key, 0, 1000);
                        System.out.println(zrangeSet);
                    }

                    if (data != null && !data.isEmpty()) {
                        String[][] content = new String[data.size()][columnTitle.length];
                        for (int i = 0; i < data.size(); i++) {
                            content[i][0] = data.get(i);
                        }
                        DefaultTableModel model = new DefaultTableModel(content, columnTitle) {
                            @Override
                            public boolean isCellEditable(int row, int column) {
                                return false;
                            }
                        };
                        value.setModel(model);
                    } else if (hashMap != null && !hashMap.isEmpty()) {
                        List<String> hashKeys = new ArrayList<>(hashMap.keySet());
                        String[][] content = new String[hashKeys.size()][hashTitle.length];
                        for (int i = 0; i < hashKeys.size(); i++) {
                            content[i][0] = hashKeys.get(i);
                            content[i][1] = hashMap.get(hashKeys.get(i));
                        }
                        DefaultTableModel model = new DefaultTableModel(content, hashTitle) {
                            @Override
                            public boolean isCellEditable(int row, int column) {
                                return false;
                            }
                        };
                        value.setModel(model);
                    } else if (zrangeSet != null && !zrangeSet.isEmpty()) {
                        String[][] content = new String[zrangeSet.size()][zsetTitle.length];
                        Iterator<String> iterator = zrangeSet.iterator();
                        int i = 0;
                        while (iterator.hasNext()) {
                            String t = iterator.next();
                            content[i][1] = t;
                            Double zscore = jedis.zscore(key, t);
                            content[i][0] = String.valueOf(Math.round(zscore));
                            i++;
                        }
                        DefaultTableModel model = new DefaultTableModel(content, zsetTitle) {
                            @Override
                            public boolean isCellEditable(int row, int column) {
                                return false;
                            }
                        };
                        value.setModel(model);
                    }
                }
            }.start();
        } else {
            JOptionPane.showMessageDialog(frame, "Redis连接失败");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            String text = ((JButton) e.getSource()).getText();
            if ("toJson".equalsIgnoreCase(text)) {
                toJsonAction();
            }
        }
    }

    /**
     * 字符串转json格式
     */
    private void toJsonAction(){
        new Thread(){
            @Override
            public void run() {
                try {
                    int selectedRow = value.getSelectedRow();
                    if (selectedRow >= 0) {
                        String text = null;
                        if ("list".equalsIgnoreCase(type) || "set".equalsIgnoreCase(type)) {
                            Object valueAt = value.getValueAt(selectedRow, 0);
                            text = (String) valueAt;
                        } else if ("hash".equalsIgnoreCase(type) || "zset".equalsIgnoreCase(type)) {
                            Object valueAt = value.getValueAt(selectedRow, 1);
                            text = (String) valueAt;
                        }

                        if (StringUtils.isNotBlank(text)) {
                            formatValue.setText(JsonFormatUtil.formatJson(text));
                        }
                    }
                }catch (Exception e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage());
                }
            }
        }.start();
    }
}
