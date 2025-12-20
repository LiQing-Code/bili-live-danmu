package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Interactive;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
            JsonElement data = message.data;
            if (data == null) {
                LOGGER.error("互动包中没有data");
                return;
            }
            JsonObject dataObj = data.getAsJsonObject();
            
            var interactive = new Interactive();
            interactive.user.uid = dataObj.get("uid").getAsString();
            interactive.user.name = dataObj.get("uname").getAsString();

            JsonElement fansMedal = dataObj.get("fans_medal");
            if (fansMedal != null && !fansMedal.isJsonNull()) {
                JsonObject medalObj = fansMedal.getAsJsonObject();
                interactive.user.fansMedal = new User.FansMedal();
                interactive.user.fansMedal.name = medalObj.get("medal_name").getAsString();
                interactive.user.fansMedal.level = medalObj.get("medal_level").getAsInt();
                interactive.user.guardLevel = medalObj.get("guard_level").getAsInt();
                if (interactive.user.fansMedal.name.isEmpty())
                    interactive.user.fansMedal = null;
            }

            interactive.type = dataObj.get("msg_type").getAsInt();
            onInteractive.accept(interactive);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
