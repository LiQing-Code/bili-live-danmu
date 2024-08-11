package cn.liqing.bili.live.danmu.model;

import cn.liqing.bili.live.danmu.User;

public class SuperChat {
    public int id;
    public User user = new User();
    public String body;

    /**
     * 持续秒数
     */
    public int time;

    /**
     * 金额 单位元
     */
    public float price;
}
