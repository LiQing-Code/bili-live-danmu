package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.model.Danmu;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 DanmuHandler 的 Gson 集成
 */
class DanmuHandlerTest {

    private final Gson gson = new Gson();
    private List<Danmu> receivedDanmus;
    private DanmuHandler handler;

    @BeforeEach
    void setUp() {
        receivedDanmus = new ArrayList<>();
        handler = new DanmuHandler(receivedDanmus::add);
    }

    @Test
    void testCanHandleDanmuMessage() {
        // 测试能够处理弹幕消息
        String json = "{\"cmd\":\"DANMU_MSG\",\"info\":[[0,1,25,16777215,1234567890,1234567890,0,\"abc\",0,0,0,\"\",0],\"测试弹幕\",[123456,\"用户名\",0,0,0,10000,1],[],0,0,1,0,0,\"\"]}";
        
        Message message = gson.fromJson(json, Message.class);
        
        assertTrue(handler.canHandle(message));
    }

    @Test
    void testCannotHandleOtherMessages() {
        // 测试不能处理其他类型的消息
        String json = "{\"cmd\":\"SEND_GIFT\",\"data\":{}}";
        
        Message message = gson.fromJson(json, Message.class);
        
        assertFalse(handler.canHandle(message));
    }

    @Test
    void testHandleDanmuMessage() {
        // 测试处理弹幕消息
        String json = "{\"cmd\":\"DANMU_MSG\",\"info\":[[0,1,25,16777215,1234567890,1234567890,0,\"abc\",0,0,0,\"\",0],\"测试内容\",[123456,\"测试用户\",0,0,0,10000,1],[1,\"粉丝团\",\"主播名\",123,10426609,\"\",0,7706705,7706705,16752445,\"\",\"\"],0,0,1,0,0,\"\"]}";
        
        Message message = gson.fromJson(json, Message.class);
        handler.handle(message);
        
        assertEquals(1, receivedDanmus.size());
        Danmu danmu = receivedDanmus.get(0);
        
        assertEquals("测试内容", danmu.body);
        assertEquals("123456", danmu.user.uid);
        assertEquals("测试用户", danmu.user.name);
        assertNotNull(danmu.user.fansMedal);
        assertEquals(1, danmu.user.fansMedal.level);
        assertEquals("粉丝团", danmu.user.fansMedal.name);
    }

    @Test
    void testHandleDanmuWithoutMedal() {
        // 测试处理没有粉丝团的弹幕
        String json = "{\"cmd\":\"DANMU_MSG\",\"info\":[[0,1,25,16777215,1234567890,1234567890,0,\"abc\",0,0,0,\"\",0],\"普通弹幕\",[789,\"普通用户\",0,0,0,10000,1],[],0,0,1,0,0,\"\"]}";
        
        Message message = gson.fromJson(json, Message.class);
        handler.handle(message);
        
        assertEquals(1, receivedDanmus.size());
        Danmu danmu = receivedDanmus.get(0);
        
        assertEquals("普通弹幕", danmu.body);
        assertEquals("789", danmu.user.uid);
        assertEquals("普通用户", danmu.user.name);
        // 空的粉丝团数组不应该创建 fansMedal 对象
    }

    @Test
    void testHandleGuardLevelDanmu() {
        // 测试处理带舰长等级的弹幕
        // info[7] 是 guardLevel
        String json = "{\"cmd\":\"DANMU_MSG\",\"info\":[[0,1,25,16777215,1234567890,1234567890,0,\"abc\",0,0,0,\"\",0],\"舰长弹幕\",[999,\"舰长用户\",0,0,0,10000,1],[],0,0,1,3,0,\"\"]}";
        
        Message message = gson.fromJson(json, Message.class);
        handler.handle(message);
        
        assertEquals(1, receivedDanmus.size());
        Danmu danmu = receivedDanmus.get(0);
        
        assertEquals("舰长弹幕", danmu.body);
        assertEquals(3, danmu.user.guardLevel);
    }

    @Test
    void testCannotHandleEmojiDanmu() {
        // 测试不处理表情弹幕（info[0][12] == 1）
        String json = "{\"cmd\":\"DANMU_MSG\",\"info\":[[0,1,25,16777215,1234567890,1234567890,0,\"abc\",0,0,0,\"\",1],\"表情\",[456,\"用户\",0,0,0,10000,1],[],0,0,1,0,0,\"\"]}";
        
        Message message = gson.fromJson(json, Message.class);
        
        // 表情弹幕应该不被这个 handler 处理
        assertFalse(handler.canHandle(message));
    }
}
