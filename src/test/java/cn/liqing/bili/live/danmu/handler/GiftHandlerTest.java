package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.model.Gift;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 GiftHandler 的 Gson 集成
 */
class GiftHandlerTest {

    private final Gson gson = new Gson();
    private List<Gift> receivedGifts;
    private GiftHandler handler;

    @BeforeEach
    void setUp() {
        receivedGifts = new ArrayList<>();
        handler = new GiftHandler(receivedGifts::add);
    }

    @Test
    void testCanHandleGiftMessage() {
        String json = "{\"cmd\":\"SEND_GIFT\",\"data\":{}}";
        
        Message message = gson.fromJson(json, Message.class);
        
        assertTrue(handler.canHandle(message));
    }

    @Test
    void testHandleGoldGift() {
        // 测试处理金瓜子礼物
        String json = "{\"cmd\":\"SEND_GIFT\",\"data\":{\"uid\":12345,\"uname\":\"送礼用户\",\"giftId\":1,\"giftName\":\"辣条\",\"num\":10,\"total_coin\":10000,\"coin_type\":\"gold\",\"guard_level\":0,\"medal_info\":{\"medal_name\":\"测试勋章\",\"medal_level\":5}}}";
        
        Message message = gson.fromJson(json, Message.class);
        handler.handle(message);
        
        assertEquals(1, receivedGifts.size());
        Gift gift = receivedGifts.get(0);
        
        assertEquals("12345", gift.user.uid);
        assertEquals("送礼用户", gift.user.name);
        assertEquals(1, gift.id);
        assertEquals("辣条", gift.name);
        assertEquals(10, gift.num);
        assertEquals(10.0f, gift.price, 0.001f); // 10000 / 1000 = 10
        assertNotNull(gift.user.fansMedal);
        assertEquals(5, gift.user.fansMedal.level);
        assertEquals("测试勋章", gift.user.fansMedal.name);
    }

    @Test
    void testHandleSilverGift() {
        // 测试处理银瓜子礼物（免费礼物）
        String json = "{\"cmd\":\"SEND_GIFT\",\"data\":{\"uid\":67890,\"uname\":\"用户B\",\"giftId\":2,\"giftName\":\"小心心\",\"num\":5,\"total_coin\":0,\"coin_type\":\"silver\",\"guard_level\":0,\"medal_info\":null}}";
        
        Message message = gson.fromJson(json, Message.class);
        handler.handle(message);
        
        assertEquals(1, receivedGifts.size());
        Gift gift = receivedGifts.get(0);
        
        assertEquals("67890", gift.user.uid);
        assertEquals("用户B", gift.user.name);
        assertEquals(2, gift.id);
        assertEquals("小心心", gift.name);
        assertEquals(5, gift.num);
        assertEquals(0.0f, gift.price, 0.001f); // 银瓜子礼物价格为 0
    }

    @Test
    void testHandleGuardGift() {
        // 测试处理舰长送的礼物
        String json = "{\"cmd\":\"SEND_GIFT\",\"data\":{\"uid\":11111,\"uname\":\"舰长用户\",\"giftId\":3,\"giftName\":\"B坷垃\",\"num\":1,\"total_coin\":9994,\"coin_type\":\"gold\",\"guard_level\":3,\"medal_info\":{\"medal_name\":\"舰队\",\"medal_level\":20}}}";
        
        Message message = gson.fromJson(json, Message.class);
        handler.handle(message);
        
        assertEquals(1, receivedGifts.size());
        Gift gift = receivedGifts.get(0);
        
        assertEquals("舰长用户", gift.user.name);
        assertEquals(3, gift.user.guardLevel);
        assertEquals(9.994f, gift.price, 0.001f);
        assertEquals(20, gift.user.fansMedal.level);
    }

    @Test
    void testHandleGiftWithoutMedal() {
        // 测试处理没有粉丝勋章的礼物
        String json = "{\"cmd\":\"SEND_GIFT\",\"data\":{\"uid\":99999,\"uname\":\"新用户\",\"giftId\":1,\"giftName\":\"辣条\",\"num\":1,\"total_coin\":1000,\"coin_type\":\"gold\",\"guard_level\":0,\"medal_info\":null}}";
        
        Message message = gson.fromJson(json, Message.class);
        handler.handle(message);
        
        assertEquals(1, receivedGifts.size());
        Gift gift = receivedGifts.get(0);
        
        assertEquals("新用户", gift.user.name);
        assertNull(gift.user.fansMedal);
    }

    @Test
    void testCannotHandleOtherMessages() {
        String json = "{\"cmd\":\"DANMU_MSG\",\"info\":[]}";
        
        Message message = gson.fromJson(json, Message.class);
        
        assertFalse(handler.canHandle(message));
    }
}
