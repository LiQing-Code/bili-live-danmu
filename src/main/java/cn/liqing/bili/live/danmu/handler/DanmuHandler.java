package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Danmu;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class DanmuHandler implements MessageHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(DanmuHandler.class);
    private final Consumer<Danmu> onDanmu;

    public DanmuHandler(Consumer<Danmu> onDanmu) {
        this.onDanmu = onDanmu;
    }

    @Override
    public boolean canHandle(Message message) {
        if (!Objects.equals(message.cmd, "DANMU_MSG"))
            return false;
        if (message.info == null)
            return false;
        JsonArray info = message.info.getAsJsonArray();
        return info.get(0).getAsJsonArray().get(12).getAsInt() != 1;
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
            var danmu = new Danmu();
            danmu.user.uid = infoArray.get(2).getAsJsonArray().get(0).getAsString();
            danmu.user.name = infoArray.get(2).getAsJsonArray().get(1).getAsString();
            danmu.user.guardLevel = infoArray.get(7).getAsInt();

            //解析粉丝团
            JsonElement fansMedal = infoArray.get(3);
            if (fansMedal != null && fansMedal.isJsonArray() && fansMedal.getAsJsonArray().size() >= 2) {
                danmu.user.fansMedal = new User.FansMedal();
                danmu.user.fansMedal.level = fansMedal.getAsJsonArray().get(0).getAsInt();
                danmu.user.fansMedal.name = fansMedal.getAsJsonArray().get(1).getAsString();
            }

            //解析内容
            danmu.body = infoArray.get(1).getAsString();
            onDanmu.accept(danmu);
        } catch (Exception ex) {
            LOGGER.error("解析弹幕包出错", ex);
        }
    }
}
