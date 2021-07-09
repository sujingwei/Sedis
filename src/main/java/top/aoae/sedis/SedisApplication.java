package top.aoae.sedis;

import redis.clients.jedis.Jedis;
import top.aoae.sedis.ui.MainFrame;

import java.util.Map;

/**
 * Hello world!
 *
 */
public class SedisApplication
{

    public static void main( String[] args )
    {
        MainFrame f = new MainFrame();
        f.setVisible(true);

        /**
         * 启用连接失效恢复功能
         */
        f.jedisRestoration();
    }
}
