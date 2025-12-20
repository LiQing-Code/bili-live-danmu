package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Gift;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class GiftHandler implements MessageHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(GiftHandler.class);
    private final Consumer<Gift> onGift;

    public GiftHandler(Consumer<Gift> onGift) {
        this.onGift = onGift;
    }

    @Override
    public boolean canHandle(Message message) {
        return Objects.equals(message.cmd, "SEND_GIFT");
    }

    @Override
    public void handle(Message message) {
        try {
            JsonElement data = message.data;
            if (data == null) {
                LOGGER.error("礼物包中没有data");
                return;
            }
            JsonObject dataObj = data.getAsJsonObject();
            
            var gift = new Gift();
            gift.user.uid = dataObj.get("uid").getAsString();
            gift.user.name = dataObj.get("uname").getAsString();
            gift.user.guardLevel = dataObj.get("guard_level").getAsInt();

            JsonElement fansMedal = dataObj.get("medal_info");
            if (fansMedal != null && !fansMedal.isJsonNull()) {
                JsonObject medalObj = fansMedal.getAsJsonObject();
                gift.user.fansMedal = new User.FansMedal();
                gift.user.fansMedal.name = medalObj.get("medal_name").getAsString();
                gift.user.fansMedal.level = medalObj.get("medal_level").getAsInt();
                if (gift.user.name.isEmpty())
                    gift.user.fansMedal = null;
            }

            gift.id = dataObj.get("giftId").getAsInt();
            gift.name = dataObj.get("giftName").getAsString();
            if (Objects.equals(dataObj.get("coin_type").getAsString(), "gold"))
                gift.price = dataObj.get("total_coin").getAsInt() / 1000f;
            else
                gift.price = 0;
            gift.num = dataObj.get("num").getAsInt();
            onGift.accept(gift);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
