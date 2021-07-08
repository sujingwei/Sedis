package top.aoae.sedis.ui;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;

public class AddDialog extends JDialog implements ActionListener {
    private JFrame frame;
    private String name;
    private TabPanel tabPanel;

    private JTextField keyTextField = new JTextField("Key");
    private JTextField timeoutTextField = new JTextField("-1");
    private JComboBox typeComboBox = new JComboBox();
    private JTextArea valueTextArea = new JTextArea();


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

    public AddDialog(JFrame frame, TabPanel tabPanel, String name) {
        super(frame, "添加key");
        this.setSize(480, 350);
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(null); // 窗口居中
        this.setResizable(false); // false-窗口不能最大化

        this.frame = frame;
        this.tabPanel = tabPanel;
        this.name = name;

        init();

        this.setVisible(false);
    }

    @Override
    public void setVisible(boolean b) {
        if (!b) {
            // 还原
            keyTextField.setText("Key");
            timeoutTextField.setText("-1");
            valueTextArea.setText("");
            typeComboBox.setSelectedIndex(0);
        }
        super.setVisible(b);
    }

    private void init() {
        // this.add(contentPanel(), BorderLayout.CENTER);
        String labelString = String.format("<html><head></head><body style=\"padding:0 10px 10px 10px;\">%s %s %s %s %s %s</body></html>",
                "<h4>增加说明:</h4>",
                "<div><b>String: </b>TEXT</div>",
                "<div><b>List: </b>每一行表示list中的一个元素</div>",
                "<div><b>Set: </b>每一行表示Set中的一个元素</div>",
                "<div><b>Hash: </b>每一行表示一个元素，k和v使用\"###\"符号分隔如:key###value </div>",
                "<div<b>ZSet: </b>每一行表示一个元素，z和v使用\"###\"符号分隔如: 3##I am Value</div>"
        );
        this.add(new JLabel(labelString), BorderLayout.SOUTH);
        this.add(contentPanel(), BorderLayout.CENTER);
    }

    private JPanel contentPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(valueTextArea), BorderLayout.CENTER);
        p.add(northPanel(), BorderLayout.NORTH);

        JButton addBtn = new JButton("添加(Add)");
        addBtn.setToolTipText("add");
        addBtn.addActionListener(this);
        p.add(addBtn, BorderLayout.SOUTH);

        return p;
    }


    private JPanel northPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 1, 1));
        typeComboBox.addItem("String");
        typeComboBox.addItem("List");
        typeComboBox.addItem("Set");
        typeComboBox.addItem("Hash");
        typeComboBox.addItem("ZSet");

        keyTextField.setForeground(Color.GRAY);
        keyTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (keyTextField.getText().equals("Key")) {
                    keyTextField.setText("");
                    keyTextField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (keyTextField.getText().equals("")) {
                    keyTextField.setText("Key");
                    keyTextField.setForeground(Color.GRAY);
                }
            }
        });

        p.add(typeComboBox);
        p.add(keyTextField);
        p.add(new JLabel("Timeout(s):"));
        p.add(timeoutTextField);
        return p;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            String tipText = ((JButton) e.getSource()).getToolTipText();
            if ("add".equalsIgnoreCase(tipText)) {
                // 添加redis操作
                String key = keyTextField.getText();
                if (StringUtils.isBlank(key) || "key".equalsIgnoreCase(key)) {
                    JOptionPane.showMessageDialog(this, "key为空");
                    return;
                }
                String value = valueTextArea.getText();
                if (StringUtils.isBlank(value)) {
                    JOptionPane.showMessageDialog(this, "value为空");
                    return;
                }
                String type = typeComboBox.getSelectedItem().toString();
                String timeoutStr = timeoutTextField.getText();

                try {
                    int timeOut = Integer.valueOf(timeoutStr).intValue();
                    addAction(type, key, value, timeOut);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                }
            }
        }
    }

    private void addAction(String type, String key, String value, int timeout) {
        Jedis jedis = getJedis();
        if (jedis == null || !jedis.isConnected()) {
            this.setVisible(false);
            JOptionPane.showMessageDialog(frame, "redis连接失败");
            return;
        }

        if ("list".equalsIgnoreCase(type)) {
            String[] split = value.split("\\n");
            if (split != null && split.length > 0) {
                jedis.lpush(key, split);
            }
        } else if ("set".equalsIgnoreCase(type)) {
            String[] split = value.split("\\n");
            if (split != null && split.length > 0) {
                jedis.sadd(key, split);
            }
        } else if ("hash".equalsIgnoreCase(type)) {
            String[] split = value.split("\\n");
            Map<String, String> redisHashMap = new HashMap<>();
            if (split != null && split.length > 0) {
                for (int i = 0; i < split.length; i++) {
                    int index = split[i].indexOf("###");
                    if (index <= 0) {
                        JOptionPane.showMessageDialog(this, "hash数据格式错误");
                        return;
                    }
                    redisHashMap.put(split[i].substring(0, index), split[i].substring(index + 3));
                }
            }
            if (!redisHashMap.isEmpty()) {
                jedis.hmset(key, redisHashMap);
            }
        } else if ("ZSet".equalsIgnoreCase(type)) {
            Map<String, Double> zsetMap = new HashMap<>();
            String[] split = value.split("\\n");
            if (split != null && split.length > 0) {
                for (int i = 0; i < split.length; i++) {
                    int index = split[i].indexOf("###");
                    if (index <= 0) {
                        JOptionPane.showMessageDialog(this, "ZSet数据格式错误");
                        return;
                    }
                    try {
                        Double aDouble = Double.valueOf(split[i].substring(0, index));
                        zsetMap.put(split[i].substring(index + 3), aDouble);
                    }catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage());
                        return; // 结束当前方法
                    }

                }
            }
            if (!zsetMap.isEmpty()) {
                 jedis.zadd(key, zsetMap);
            }
        } else {
            // 字符串类型
            jedis.set(key, value);
        }
        if (timeout > 0) {
            if(jedis.exists(key)) {
                // 设置过期时间
                jedis.expire(key, timeout);
            }
        }
        tabPanel.updateKeyTableData();
        this.setVisible(false);
    }

}
