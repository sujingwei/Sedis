package top.aoae.sedis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;


public class JedisTest {

    private Jedis jedis = null;

    @Before
    public void init() {
        jedis = new Jedis("10.79.4.214", 7678);
        jedis.auth("redis123456");
        System.out.println("+ |" + jedis);
        System.out.println("+ |----------------------------------------\n");
    }

    @After
    public void disconnect() {
        if (jedis != null) {
            jedis.disconnect();
            jedis = null;
        }
        System.out.println("\n+ |----------------------------------------");
        System.out.println("+ |" + jedis);
    }

    @Test
    public void setnxTest() {


        String str = "{\"content\":\"this is the msg content.\",\"tousers\":\"user1|user2\",\"msgtype\":\"texturl\",\"appkey\":\"test\",\"domain\":\"test\","
                + "\"system\":{\"wechat\":{\"safe\":\"1\"}},\"texturl\":{\"urltype\":\"0\",\"user1\":{\"spStatus\":\"user01\",\"workid\":\"work01\"},\"user2\":{\"spStatus\":\"user02\",\"workid\":\"work02\"}}}";

        jedis.set("jsonStr", str);

    }

    @Test
    public void redisCluster(){
        System.out.println(isCluster());
    }

    /**
     * 判断是否为Redis集群
     * @return
     */
    public boolean isCluster(){
        boolean result = false;
        try {
            String s = jedis.clusterInfo();
            if(s!=null && s.length()>0) {
                result = s.indexOf("cluster_state:ok") >= 0;
            }
        }catch (Exception e) {
            // 不是集群
            System.out.println(e.getMessage());
        }
        return result;
    }
}
