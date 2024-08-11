package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Gift;
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
            var data = message.data;
            if (data == null) {
                LOGGER.error("礼物包中没有data");
                return;
            }
            var gift = new Gift();
            gift.user.uid = data.get("uid").asText();
            gift.user.name = data.get("uname").asText();
            gift.user.guardLevel = data.get("guard_level").asInt();

            var fansMedal = data.get("medal_info");
            if (fansMedal != null && !fansMedal.isNull()) {
                gift.user.fansMedal = new User.FansMedal();
                gift.user.fansMedal.name = fansMedal.get("medal_name").asText();
                gift.user.fansMedal.level = fansMedal.get("medal_level").asInt();
                if (gift.user.name.isEmpty())
                    gift.user.fansMedal = null;
            }

            gift.id = data.get("giftId").asInt();
            gift.name = data.get("giftName").asText();
            if (Objects.equals(data.get("coin_type").asText(), "gold"))
                gift.price = data.get("total_coin").asInt() / 1000f;
            else
                gift.price = 0;
            gift.num = data.get("num").asInt();
            onGift.accept(gift);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
