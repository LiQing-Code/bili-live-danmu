package cn.liqing.bili.live.danmu;

public interface ConnectionListener {
    void onOpen();
    void onClose(int code, String reason, boolean remote);
    void onError(Exception ex);
}
