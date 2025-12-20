package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.model.Guard;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
            JsonElement data = message.data;
            if (data == null) {
                LOGGER.error("舰长包中没有data");
                return;
            }
            JsonObject dataObj = data.getAsJsonObject();
            
            var guard = new Guard();
            guard.user.uid = dataObj.get("uid").getAsString();
            guard.user.name = dataObj.get("username").getAsString();
            guard.user.guardLevel = dataObj.get("guard_level").getAsInt();
            guard.id = dataObj.get("gift_id").getAsInt();
            guard.name = dataObj.get("role_name").getAsString();
            guard.price = dataObj.get("price").getAsInt() / 1000f;
            guard.num = dataObj.get("num").getAsInt();
            guard.level = guard.user.guardLevel;
            guard.unit = dataObj.get("unit").getAsString();
            onGuard.accept(guard);
        } catch (Exception ex) {
            LOGGER.error("解析消息出错", ex);
        }
    }
}
