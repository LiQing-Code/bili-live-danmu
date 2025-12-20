package cn.liqing.example;

import cn.liqing.bili.live.danmu.*;
import cn.liqing.bili.live.danmu.handler.*;
import cn.liqing.bili.live.danmu.model.*;

/**
 * 简单的直播间连接测试示例
 * 
 * 使用方法：
 * 1. 在项目根目录运行: ../../../gradlew publishToMavenLocal
 * 2. 在本目录运行: ./gradlew run
 * 3. 观察控制台输出的弹幕、礼物等消息
 */
public class SimpleLiveRoomTest {
    
    public static void main(String[] args) throws Exception {
        // 配置：修改这里的房间号来测试不同的直播间
        long roomId = 3;  // 官方直播间，可以改成任意正在直播的房间号
        
        System.out.println("========================================");
        System.out.println("  Bilibili 直播间弹幕监听测试");
        System.out.println("========================================");
        System.out.println("房间号: " + roomId);
        System.out.println("正在连接...\n");
        
        // 创建客户端
        DanmuClient client = new DanmuClient();
        
        // 设置连接监听器
        client.setListener(new ConnectionListener() {
            @Override
            public void onOpen() {
                System.out.println("✅ 连接成功！开始监听消息...\n");
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("\n❌ 连接已关闭");
                System.out.println("原因: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("❌ 发生错误: " + ex.getMessage());
            }
        });
        
        // 添加弹幕处理器
        client.addHandler(new DanmuHandler(danmu -> {
            StringBuilder sb = new StringBuilder();
            sb.append("💬 [弹幕] ");
            
            // 显示舰长等级
            if (danmu.user.guardLevel > 0) {
                String guardBadge = switch (danmu.user.guardLevel) {
                    case 1 -> "🔱总督";
                    case 2 -> "⚔️提督";
                    case 3 -> "🛡️舰长";
                    default -> "";
                };
                sb.append(guardBadge).append(" ");
            }
            
            // 显示用户名
            sb.append(danmu.user.name);
            
            // 显示粉丝勋章
            if (danmu.user.fansMedal != null) {
                sb.append(" [")
                  .append(danmu.user.fansMedal.name)
                  .append(" Lv.")
                  .append(danmu.user.fansMedal.level)
                  .append("]");
            }
            
            sb.append(": ").append(danmu.body);
            
            System.out.println(sb.toString());
        }));
        
        // 添加礼物处理器
        client.addHandler(new GiftHandler(gift -> {
            System.out.printf("🎁 [礼物] %s 送出 %s x%d", 
                gift.user.name, gift.name, gift.num);
            if (gift.price > 0) {
                System.out.printf(" (%.2f元)", gift.price);
            }
            System.out.println();
        }));
        
        // 添加醒目留言处理器
        client.addHandler(new SuperChatHandler(sc -> {
            System.out.printf("⭐ [醒目留言] %s (%d元): %s\n", 
                sc.user.name, sc.price, sc.body);
        }));
        
        // 添加舰长购买处理器
        client.addHandler(new GuardHandler(guard -> {
            System.out.printf("🛡️ [%s] %s 购买了 %s (%.2f元)\n", 
                guard.name, guard.user.name, guard.unit, guard.price);
        }));
        
        // 添加互动处理器
        client.addHandler(new InteractiveHandler(interactive -> {
            String action = switch (interactive.type) {
                case 1 -> "进入直播间";
                case 2 -> "关注了主播";
                case 3 -> "分享了直播间";
                default -> "互动";
            };
            System.out.printf("👋 [%s] %s\n", action, interactive.user.name);
        }));
        
        // 创建认证信息并连接（无需登录）
        Auth auth = Auth.create(roomId);
        client.connect(auth);
        
        // 运行 5 分钟后自动退出
        System.out.println("程序将运行 5 分钟，按 Ctrl+C 可提前退出\n");
        System.out.println("----------------------------------------");
        
        Thread.sleep(5 * 60 * 1000);
        
        // 断开连接
        System.out.println("\n----------------------------------------");
        System.out.println("测试时间到，正在断开连接...");
        client.disconnect();
        System.out.println("测试完成！");
    }
}
