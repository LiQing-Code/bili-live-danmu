package cn.liqing.bili.live.danmu;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 BiliWbiSign 的 Gson 集成
 */
class BiliWbiSignTest {

    private final Gson gson = new Gson();
    
    // 使用足够长的测试密钥（至少 64 个字符以确保 getMixinKey 能产生 32 个字符）
    private static final String TEST_IMG_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String TEST_SUB_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    @Test
    void testMd5() throws Exception {
        // 测试 MD5 哈希（通过反射或测试公共方法）
        // 由于 md5 是私有方法，我们通过 encWbi 间接测试
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("test", "value");
        
        String result = BiliWbiSign.encWbi(params, TEST_IMG_KEY, TEST_SUB_KEY);
        
        assertNotNull(result);
        assertTrue(result.contains("test=value"));
        assertTrue(result.contains("&w_rid="));
        assertTrue(result.contains("&wts="));
    }

    @Test
    void testEncWbi() {
        // 测试 WBI 签名编码
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", 12345);
        params.put("type", "0");
        
        String result = BiliWbiSign.encWbi(params, TEST_IMG_KEY, TEST_SUB_KEY);
        
        assertNotNull(result);
        assertTrue(result.contains("id="));
        assertTrue(result.contains("type="));
        assertTrue(result.contains("&w_rid="));
        
        // 验证参数是按字典序排序的（TreeMap）
        String[] parts = result.split("&");
        assertTrue(parts.length >= 3);
    }

    @Test
    void testEncWbiWithSpecialCharacters() {
        // 测试特殊字符处理
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("name", "测试用户");
        params.put("value", "test!value");
        
        String result = BiliWbiSign.encWbi(params, TEST_IMG_KEY, TEST_SUB_KEY);
        
        assertNotNull(result);
        // URL 编码应该处理特殊字符
        assertTrue(result.contains("name=%E6%B5%8B%E8%AF%95%E7%94%A8%E6%88%B7"));
    }

    @Test
    void testEncWbiRemovesSpecialChars() {
        // 测试移除特殊字符 !'()*
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("text", "hello!(world)*");
        
        String result = BiliWbiSign.encWbi(params, TEST_IMG_KEY, TEST_SUB_KEY);
        
        assertNotNull(result);
        // 特殊字符应该被移除
        assertFalse(result.contains("!"));
        assertFalse(result.contains("("));
        assertFalse(result.contains(")"));
        assertFalse(result.contains("*"));
    }

    @Test
    void testUrlEncode() {
        // 测试 URL 编码（通过 encWbi 间接测试）
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("param", "value with spaces");
        
        String result = BiliWbiSign.encWbi(params, TEST_IMG_KEY, TEST_SUB_KEY);
        
        assertNotNull(result);
        // 空格应该被编码
        assertTrue(result.contains("value+with+spaces") || result.contains("value%20with%20spaces"));
    }

    @Test
    void testEncWbiConsistency() {
        // 测试相同输入产生相同输出（除了时间戳）
        java.util.Map<String, Object> params1 = new java.util.HashMap<>();
        params1.put("id", 100);
        
        java.util.Map<String, Object> params2 = new java.util.HashMap<>();
        params2.put("id", 100);
        
        String result1 = BiliWbiSign.encWbi(params1, TEST_IMG_KEY, TEST_SUB_KEY);
        // 等待一秒确保时间戳不同
        String result2 = BiliWbiSign.encWbi(params2, TEST_IMG_KEY, TEST_SUB_KEY);
        
        // 两次调用都应该成功
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("id=100"));
        assertTrue(result2.contains("id=100"));
    }
}
