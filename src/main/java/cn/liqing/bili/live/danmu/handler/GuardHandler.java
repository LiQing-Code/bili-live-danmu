package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.model.Guard;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class GuardHandler implements MessageHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(GuardHandler.class);
    private final Consumer<Guard> onGuard;

    public GuardHandler(Consumer<Guard> onGuard) {
        this.onGuard = onGuard;
    }

    @Override
    public boolean canHandle(@NotNull Message message) {
        return Objects.equals(message.cmd, "USER_TOAST_MSG");
    }

    @Override
    public void handle(Message message) {
        try {
            var data = message.data;
            if (data == null) {
                LOGGER.error("舰长包中没有data");
                return;
            }
            var guard = new Guard();
            guard.user.uid = data.get("uid").asText();
            guard.user.name = data.get("username").asText();
            guard.user.guardLevel = data.get("guard_level").asInt();
            guard.id = data.get("gift_id").asInt();
            guard.name = data.get("role_name").asText();
            guard.price = data.get("price").asInt() / 1000f;
            guard.num = data.get("num").asInt();
            guard.level = guard.user.guardLevel;
            guard.unit = data.get("unit").asText();
            onGuard.accept(guard);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
