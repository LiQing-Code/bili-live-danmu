package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.SuperChat;
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
            var data = message.data;
            if (data == null) {
                LOGGER.warn("醒目留言包中没有data");
                return;
            }
            var sc = new SuperChat();
            sc.user.uid = data.get("uid").asText();
            sc.user.name = data.at("/user_info/uname").asText();
            sc.user.guardLevel = data.at("/user_info/guard_level").asInt();

            var fansMedal = data.get("medal_info");
            if (fansMedal != null && !fansMedal.isNull()) {
                sc.user.fansMedal = new User.FansMedal();
                sc.user.fansMedal.name = fansMedal.get("medal_name").asText();
                sc.user.fansMedal.level = fansMedal.get("medal_level").asInt();
                if (sc.user.name.isEmpty())
                    sc.user.fansMedal = null;
            }

            sc.id = data.get("id").asInt();
            sc.body = data.get("message").asText();
            sc.price = data.get("price").asInt();
            sc.time = data.get("time").asInt();
            onSuperChat.accept(sc);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
