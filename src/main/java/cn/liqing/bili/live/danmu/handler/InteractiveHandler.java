package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Interactive;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class InteractiveHandler implements MessageHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(InteractiveHandler.class);
    private final Consumer<Interactive> onInteractive;

    public InteractiveHandler(Consumer<Interactive> onInteractive) {
        this.onInteractive = onInteractive;
    }

    @Override
    public boolean canHandle(@NotNull Message message) {
        return Objects.equals(message.cmd, "INTERACT_WORD");
    }

    @Override
    public void handle(Message message) {
        try {
            var data = message.data;
            if (data == null) {
                LOGGER.error("互动包中没有data");
                return;
            }
            var interactive = new Interactive();
            interactive.user.uid = data.get("uid").asText();
            interactive.user.name = data.get("uname").asText();

            var fansMedal = data.get("fans_medal");
            if (fansMedal != null && !fansMedal.isNull()) {
                interactive.user.fansMedal = new User.FansMedal();
                interactive.user.fansMedal.name = fansMedal.get("medal_name").asText();
                interactive.user.fansMedal.level = fansMedal.get("medal_level").asInt();
                interactive.user.guardLevel = fansMedal.get("guard_level").asInt();
                if (interactive.user.fansMedal.name.isEmpty())
                    interactive.user.fansMedal = null;
            }

            interactive.type = data.get("msg_type").asInt();
            onInteractive.accept(interactive);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
