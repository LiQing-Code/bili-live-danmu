package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Danmu;
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
        return message.info.at("/0/12").asInt() != 1;
    }

    @Override
    public void handle(Message message) {
        try {
            var info = message.info;
            if (info == null) {
                LOGGER.error("弹幕包中没有info");
                return;
            }
            //解析用户
            var danmu = new Danmu();
            danmu.user.uid = info.at("/2/0").asText();
            danmu.user.name = info.at("/2/1").asText();
            danmu.user.guardLevel = info.get(7).asInt();

            //解析粉丝团
            var fansMedal = info.get(3);
            if (fansMedal != null && fansMedal.size() >= 2) {
                danmu.user.fansMedal = new User.FansMedal();
                danmu.user.fansMedal.level = fansMedal.get(0).asInt();
                danmu.user.fansMedal.name = fansMedal.get(1).asText();
            }

            //解析内容
            danmu.body = info.get(1).asText();
            onDanmu.accept(danmu);
        } catch (Exception ex) {
            LOGGER.error("解析弹幕包出错", ex);
        }
    }
}
