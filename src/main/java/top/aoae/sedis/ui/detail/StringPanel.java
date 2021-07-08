package top.aoae.sedis.ui.detail;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import top.aoae.sedis.ui.MainFrame;
import top.aoae.sedis.util.JsonFormatUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 字符串类型的窗口
 */
public class StringPanel extends JPanel implements ActionListener {

    private JFrame frame;
    private String name;
    private String key;
    private String type;


    private JTextArea value = new JTextArea();
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

    public StringPanel(JFrame frame, String name) {
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
        value.setEnabled(false);
        p.add(new JScrollPane(value), BorderLayout.CENTER);
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
    public void updateDate(){
        formatValue.setText("");
        Jedis jedis = getJedis();
        if (jedis != null && StringUtils.isNotBlank(key)) {
            new Thread(){
                @Override
                public void run() {
                    detail.setText(String.format("Key: %s, Type: %s", key, type));
                    String s = jedis.get(key);
                    if (StringUtils.isNotBlank(s)) {
                        value.setText(s);
                    }
                }
            }.start();
        } else {

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
                    String text = value.getText();
                    if (StringUtils.isNotBlank(text)) {
                        String s = JsonFormatUtil.formatJson(text);
                        formatValue.setText(s);
                    }
                }catch (Exception e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage());
                }
            }
        }.start();;

    }
}
