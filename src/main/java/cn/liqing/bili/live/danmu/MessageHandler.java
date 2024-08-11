package cn.liqing.bili.live.danmu;

public interface MessageHandler {
    boolean canHandle(Message message);
    void handle(Message message);
}
