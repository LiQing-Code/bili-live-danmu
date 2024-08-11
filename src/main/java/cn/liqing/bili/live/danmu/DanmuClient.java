package cn.liqing.bili.live.danmu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DanmuClient {
    static final Logger LOGGER = LoggerFactory.getLogger(DanmuClient.class);
    private final URI serverUri;
    private WebSocketClient wsClient;
    private ConnectionListener connectionListener;
    private final List<MessageHandler> handlers = new ArrayList<>();

    public DanmuClient() {
        this(URI.create("wss://broadcastlv.chat.bilibili.com:2245/sub"));
    }

    public DanmuClient(URI serverUri) {
        this.serverUri = serverUri;
    }

    public void connect(Auth auth) {
        if (wsClient != null) {
            disconnect();
        }

        wsClient = new WebSocketClient(serverUri) {
            private Timer heartbeatTimer;

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                if (connectionListener != null)
                    connectionListener.onOpen();

                try {
                    //发送认证包
                    byte[] body = new ObjectMapper().writeValueAsBytes(auth);
                    send(new Packet(Packet.Operation.AUTH, body).pack());
                } catch (Exception e) {
                    throw new RuntimeException("认证出错", e);
                }

                startHeartbeat();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (connectionListener != null)
                    connectionListener.onClose(code, reason, remote);
                stopHeartbeat();
            }

            @Override
            public void onError(Exception ex) {
                if (connectionListener != null)
                    connectionListener.onError(ex);
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                var packets = Packet.unPack(bytes);
                packets.forEach(packet -> onPacket(packet));
            }

            private void startHeartbeat() {
                heartbeatTimer = new Timer(true); // Daemon thread to avoid blocking JVM shutdown
                heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (wsClient != null && wsClient.isOpen()) {
                            send(new Packet(Packet.Operation.HEARTBEAT, new byte[0]).pack());
                        }
                    }
                }, 0, 30 * 1000); // 每30秒执行一次
            }

            private void stopHeartbeat() {
                if (heartbeatTimer != null) {
                    heartbeatTimer.cancel(); // 停止定时任务
                }
            }
        };

        wsClient.connect();
    }

    public void disconnect() {
        if (wsClient != null) {
            wsClient.close();
            wsClient = null;
        }
    }

    public boolean isOpen() {
        if (wsClient == null)
            return false;
        return wsClient.isOpen();
    }

    public void setListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void addHandler(MessageHandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(MessageHandler handler) {
        handlers.remove(handler);
    }

    private void onPacket(@NotNull Packet packet) {
        if (packet.operation == Packet.Operation.SEND_SMS_REPLY) {
            String bodyStr = new String(packet.body, StandardCharsets.UTF_8);
            LOGGER.debug(bodyStr);

            Message message;
            try {
                message = new ObjectMapper().readValue(bodyStr, Message.class);
            } catch (Exception ex) {
                throw new RuntimeException("解析消息出错", ex);
            }
            if (message.cmd == null) {
                throw new RuntimeException("消息包中没有cmd");
            }

            for (MessageHandler handler : handlers) {
                if (handler.canHandle(message)) {
                    handler.handle(message);
                }
            }
        }
    }

    // 生成随机数的方法
    public static @NotNull String generateRandomNumber() {
        Random random = new Random();
        return String.valueOf(random.nextInt(Integer.MAX_VALUE));
    }

    // 从 Cookie 中获取 CSRF Token 的值
    public static @Nullable String getCsrfTokenFromCookies(@NotNull String cookies) {
        for (String cookie : cookies.split(";")) {
            String[] pair = cookie.trim().split("=");
            if (pair.length == 2 && pair[0].equals("bili_jct")) {
                return pair[1];
            }
        }
        return null;
    }

    public static void send(String cookies, String room, String message) throws IOException {
        URL url = new URL("https://api.live.bilibili.com/msg/send");
        String csrf = getCsrfTokenFromCookies(cookies);

        // 打开连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置请求方法和请求头

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryp2ynm67gIeqyCK5D");
        connection.setRequestProperty("Cookie", cookies);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 Edg/127.0.0.0");

        // 请求体内容
        String boundary = "------WebKitFormBoundaryp2ynm67gIeqyCK5D";
        String data = boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"bubble\"\r\n\r\n" +
                "0\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"msg\"\r\n\r\n" +
                message + "\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"color\"\r\n\r\n" +
                "16777215\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"mode\"\r\n\r\n" +
                "1\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"room_type\"\r\n\r\n" +
                "0\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"jumpfrom\"\r\n\r\n" +
                "0\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"reply_mid\"\r\n\r\n" +
                "0\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"reply_attr\"\r\n\r\n" +
                "0\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"replay_dmid\"\r\n\r\n" +
                "\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"statistics\"\r\n\r\n" +
                "{\"appId\":100,\"platform\":5}\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"fontsize\"\r\n\r\n" +
                "25\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"rnd\"\r\n\r\n" +
                generateRandomNumber() + "\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"roomid\"\r\n\r\n" +
                room + "\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"csrf\"\r\n\r\n" +
                csrf + "\r\n" +
                boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"csrf_token\"\r\n\r\n" +
                csrf + "\r\n" +
                boundary + "--\r\n";

        // 发送数据
        try (OutputStream os = connection.getOutputStream()) {
            os.write(data.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // 获取响应
        int responseCode = connection.getResponseCode();
//      System.out.println("Response Code: " + responseCode);
//
//        // 读取响应内容
//        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//        String inputLine;
//        StringBuilder response = new StringBuilder();
//        while ((inputLine = in.readLine()) != null) {
//            response.append(inputLine);
//        }
//        in.close();
    }

}
