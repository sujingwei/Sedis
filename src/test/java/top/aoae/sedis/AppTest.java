package top.aoae.sedis;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import top.aoae.sedis.util.JsonFormatUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        String userHome = System.getProperty("user.home");
        File file = new File(new File(userHome, ".Sedis"), "sedis.d");
        System.out.println(file.getAbsolutePath());
        System.out.println(file.getPath());
    }

    @Test
    public void writeFile() {
        String userHome = System.getProperty("user.home");
        File path = new File(userHome, ".Sedis");
        if (!path.exists()) {
            path.mkdirs();
        }
        File file = new File(path, "sedis.d");

        try {
            FileWriter fw = new FileWriter(file);
            fw.write("abcdefg");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void formatJson() {
        String str = "{\"content\":\"this is the msg content.\",\"tousers\":\"user1|user2\",\"msgtype\":\"texturl\",\"appkey\":\"test\",\"domain\":\"test\","
                + "\"system\":{\"wechat\":{\"safe\":\"1\"}},\"texturl\":{\"urltype\":\"0\",\"user1\":{\"spStatus\":\"user01\",\"workid\":\"work01\"},\"user2\":{\"spStatus\":\"user02\",\"workid\":\"work02\"}}}";
        String s = JsonFormatUtil.formatJson(str);
        System.out.println(s);
    }
}
