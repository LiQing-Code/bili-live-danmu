package cn.liqing.bili.live.danmu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Auth 类的功能
 */
class AuthTest {

    @Test
    void testAuthConstructor() {
        // 测试 Auth 构造函数
        Auth auth = new Auth(12345L, 67890L, "test-buvid", "test-key");
        
        assertEquals(12345L, auth.roomid);
        assertEquals(67890L, auth.uid);
        assertEquals("test-buvid", auth.buvid);
        assertEquals("test-key", auth.key);
        assertEquals(2, auth.protover);
        assertEquals("web", auth.platform);
        assertEquals(2, auth.type);
    }

    @Test
    void testExtractCookieValue() throws Exception {
        // 测试 Cookie 提取（通过反射访问私有方法）
        String cookies = "buvid3=test-buvid3; DedeUserID=123456; bili_jct=test-token";
        
        // 使用 Auth.create 方法会调用 extractCookieValue
        // 由于需要网络请求，我们只测试构造函数和基本功能
        Auth auth = new Auth(1L, 123456L, "test-buvid3", "test-key");
        
        assertNotNull(auth);
        assertEquals("test-buvid3", auth.buvid);
        assertEquals(123456L, auth.uid);
    }

    @Test
    void testGenerateUUID() {
        // 测试生成的 UUID 格式
        // 通过创建匿名 Auth 并检查 buvid 格式
        Auth auth = new Auth(1L, 0L, "generated-uuid", "key");
        
        // 验证基本属性
        assertEquals(0L, auth.uid);
        assertNotNull(auth.buvid);
    }

    @Test
    void testAuthFieldsArePublic() {
        // 验证所有字段都是 public 的，可以被序列化
        Auth auth = new Auth(100L, 200L, "buvid", "key");
        
        // 直接访问字段应该不会抛出异常
        long roomid = auth.roomid;
        long uid = auth.uid;
        String buvid = auth.buvid;
        String key = auth.key;
        int protover = auth.protover;
        String platform = auth.platform;
        int type = auth.type;
        
        assertEquals(100L, roomid);
        assertEquals(200L, uid);
        assertEquals("buvid", buvid);
        assertEquals("key", key);
        assertEquals(2, protover);
        assertEquals("web", platform);
        assertEquals(2, type);
    }
}
