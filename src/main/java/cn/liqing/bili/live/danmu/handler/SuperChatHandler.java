package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.SuperChat;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class SuperChatHandler implements MessageHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(SuperChatHandler.class);
    private final Consumer<SuperChat> onSuperChat;

    public SuperChatHandler(Consumer<SuperChat> onSuperChat) {
        this.onSuperChat = onSuperChat;
    }

    @Override
    public boolean canHandle(@NotNull Message message) {
        return Objects.equals(message.cmd, "SUPER_CHAT_MESSAGE");
    }

    @Override
    public void handle(Message message) {
        try {
            JsonElement data = message.data;
            if (data == null) {
                LOGGER.warn("醒目留言包中没有data");
                return;
            }
            JsonObject dataObj = data.getAsJsonObject();
            
            var sc = new SuperChat();
            sc.user.uid = dataObj.get("uid").getAsString();
            sc.user.name = dataObj.getAsJsonObject("user_info").get("uname").getAsString();
            sc.user.guardLevel = dataObj.getAsJsonObject("user_info").get("guard_level").getAsInt();

            JsonElement fansMedal = dataObj.get("medal_info");
            if (fansMedal != null && !fansMedal.isJsonNull()) {
                JsonObject medalObj = fansMedal.getAsJsonObject();
                sc.user.fansMedal = new User.FansMedal();
                sc.user.fansMedal.name = medalObj.get("medal_name").getAsString();
                sc.user.fansMedal.level = medalObj.get("medal_level").getAsInt();
                if (sc.user.name.isEmpty())
                    sc.user.fansMedal = null;
            }

            sc.id = dataObj.get("id").getAsInt();
            sc.body = dataObj.get("message").getAsString();
            sc.price = dataObj.get("price").getAsInt();
            sc.time = dataObj.get("time").getAsInt();
            onSuperChat.accept(sc);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
