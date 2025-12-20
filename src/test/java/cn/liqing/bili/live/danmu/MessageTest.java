package cn.liqing.bili.live.danmu;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Message 类的 Gson 序列化和反序列化
 */
class MessageTest {
    
    private final Gson gson = new Gson();

    @Test
    void testMessageDeserialization() {
        // 测试从 JSON 字符串反序列化 Message
        String json = "{\"cmd\":\"DANMU_MSG\",\"data\":{\"uid\":\"12345\",\"uname\":\"测试用户\"}}";
        
        Message message = gson.fromJson(json, Message.class);
        
        assertNotNull(message);
        assertEquals("DANMU_MSG", message.cmd);
        assertNotNull(message.data);
        assertTrue(message.data.isJsonObject());
        assertEquals("12345", message.data.getAsJsonObject().get("uid").getAsString());
        assertEquals("测试用户", message.data.getAsJsonObject().get("uname").getAsString());
    }

    @Test
    void testMessageWithInfo() {
        // 测试包含 info 数组的消息
        String json = "{\"cmd\":\"DANMU_MSG\",\"info\":[[0,1,25,16777215,1234567890,1234567890,0,\"abc123\",0,0,0,\"\",0,\"\",\"\"],\"测试弹幕\",[123456,\"用户名\",0,0,0,10000,1],[]]}";
        
        Message message = gson.fromJson(json, Message.class);
        
        assertNotNull(message);
        assertEquals("DANMU_MSG", message.cmd);
        assertNotNull(message.info);
        assertTrue(message.info.isJsonArray());
        assertEquals("测试弹幕", message.info.getAsJsonArray().get(1).getAsString());
    }

    @Test
    void testMessageSerialization() {
        // 测试序列化 Message 为 JSON
        Message message = new Message();
        message.cmd = "TEST_CMD";
        
        JsonObject data = new JsonObject();
        data.addProperty("key", "value");
        message.data = data;
        
        String json = gson.toJson(message);
        
        assertNotNull(json);
        assertTrue(json.contains("\"cmd\":\"TEST_CMD\""));
        assertTrue(json.contains("\"key\":\"value\""));
    }

    @Test
    void testMessageWithNullFields() {
        // 测试空字段
        String json = "{\"cmd\":\"SIMPLE_MSG\"}";
        
        Message message = gson.fromJson(json, Message.class);
        
        assertNotNull(message);
        assertEquals("SIMPLE_MSG", message.cmd);
        assertNull(message.data);
        assertNull(message.info);
    }

    @Test
    void testMessageJsonElementTypes() {
        // 测试 JsonElement 可以是不同类型
        Message message = new Message();
        message.cmd = "TEST";
        
        // data 作为对象
        JsonObject obj = new JsonObject();
        obj.addProperty("test", "value");
        message.data = obj;
        
        assertTrue(message.data.isJsonObject());
        
        // 序列化并反序列化
        String json = gson.toJson(message);
        Message deserialized = gson.fromJson(json, Message.class);
        
        assertEquals("TEST", deserialized.cmd);
        assertTrue(deserialized.data.isJsonObject());
        assertEquals("value", deserialized.data.getAsJsonObject().get("test").getAsString());
    }
}
