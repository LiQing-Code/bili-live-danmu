package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Emoji;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class EomjiHandler implements MessageHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(EomjiHandler.class);
    private final Consumer<Emoji> onEmoji;

    public EomjiHandler(Consumer<Emoji> onEmoji) {
        this.onEmoji = onEmoji;
    }

    @Override
    public boolean canHandle(Message message) {
        if (!Objects.equals(message.cmd, "DANMU_MSG"))
            return false;
        if (message.info == null)
            return false;
        JsonArray info = message.info.getAsJsonArray();
        return info.get(0).getAsJsonArray().get(12).getAsInt() == 1;
    }

    @Override
    public void handle(Message message) {
        try {
            JsonElement info = message.info;
            if (info == null) {
                LOGGER.error("弹幕包中没有info");
                return;
            }
            JsonArray infoArray = info.getAsJsonArray();
            
            //解析用户
            var emoji = new Emoji();
            emoji.user.uid = infoArray.get(2).getAsJsonArray().get(0).getAsString();
            emoji.user.name = infoArray.get(2).getAsJsonArray().get(1).getAsString();
            emoji.user.guardLevel = infoArray.get(7).getAsInt();

            //解析粉丝团
            JsonElement fansMedal = infoArray.get(3);
            if (fansMedal != null && fansMedal.isJsonArray() && fansMedal.getAsJsonArray().size() >= 2) {
                emoji.user.fansMedal = new User.FansMedal();
                emoji.user.fansMedal.level = fansMedal.getAsJsonArray().get(0).getAsInt();
                emoji.user.fansMedal.name = fansMedal.getAsJsonArray().get(1).getAsString();
            }

            //解析内容
            emoji.body = infoArray.get(1).getAsString();
            emoji.uri = infoArray.get(0).getAsJsonArray().get(13).getAsJsonObject().get("url").getAsString();
            onEmoji.accept(emoji);
        } catch (Exception ex) {
            LOGGER.error("解析弹幕包出错", ex);
        }
    }
}
