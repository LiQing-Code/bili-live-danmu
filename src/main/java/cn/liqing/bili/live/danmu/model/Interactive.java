package cn.liqing.bili.live.danmu.model;

import cn.liqing.bili.live.danmu.User;

public class Interactive {
    public static final int ENTER = 1;
    public static final int FOLLOW = 2;
    public static final int SHARE = 3;
    public static final int SPECIAL_FOLLOW = 4;
    public static final int MUTUAL_FOLLOW = 5;

    public User user = new User();
    public int type;

    public static String typeName(int type) {
        return switch (type) {
            case ENTER -> "ENTER";
            case FOLLOW -> "FOLLOW";
            case SHARE -> "SHARE";
            case SPECIAL_FOLLOW -> "SPECIAL_FOLLOW";
            case MUTUAL_FOLLOW -> "MUTUAL_FOLLOW";
            default -> "UNKNOWN";
        };
    }
}
