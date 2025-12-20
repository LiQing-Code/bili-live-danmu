package cn.liqing.bili.live.danmu;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DanmuClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanmuClient.class);
    private static final Gson GSON = new Gson();
    
    private final URI serverUri;
    private WebSocketClient wsClient;
    private ConnectionListener connectionListener;
    private final List<MessageHandler> handlers = new ArrayList<>();
    private ScheduledExecutorService heartbeatScheduler;
    private ScheduledFuture<?> heartbeatTask;

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
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                if (connectionListener != null) {
                    connectionListener.onOpen();
                }

                try {
                    byte[] body = GSON.toJson(auth).getBytes(StandardCharsets.UTF_8);
                    send(new Packet(Packet.Operation.AUTH, body).pack());
                } catch (Exception e) {
                    throw new RuntimeException("认证出错", e);
                }

                startHeartbeat();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (connectionListener != null) {
                    connectionListener.onClose(code, reason, remote);
                }
                stopHeartbeat();
            }

            @Override
            public void onError(Exception ex) {
                if (connectionListener != null) {
                    connectionListener.onError(ex);
                }
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                List<Packet> packets = Packet.unPack(bytes);
                packets.forEach(DanmuClient.this::onPacket);
            }
        };

        wsClient.connect();
    }

    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "BiliDanmu-Heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        
        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.send(new Packet(Packet.Operation.HEARTBEAT, new byte[0]).pack());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
            heartbeatScheduler = null;
        }
    }

    public void disconnect() {
        stopHeartbeat();
        if (wsClient != null) {
            wsClient.close();
            wsClient = null;
        }
    }

    public boolean isOpen() {
        return wsClient != null && wsClient.isOpen();
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
                message = GSON.fromJson(bodyStr, Message.class);
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

    public static @NotNull String generateRandomNumber() {
        return String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    }

    public static void send(String cookie, String roomId, String message) throws IOException {
        String url = "https://api.live.bilibili.com/msg/send";
        String csrf = extractCookieValue("bili_jct", cookie);
        String postData = String.format(
                "color=16777215&fontsize=25&mode=1&msg=%s&rnd=%d&roomid=%s&bubble=0&csrf_token=%s&csrf=%s",
                java.net.URLEncoder.encode(message, StandardCharsets.UTF_8),
                System.currentTimeMillis() / 1000,
                roomId,
                csrf,
                csrf
        );

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Cookie", cookie);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("发送弹幕失败，HTTP 状态码: " + responseCode);
        }
    }

    private static String extractCookieValue(String cookieName, String cookieString) {
        String[] cookies = cookieString.split(";");
        for (String cookie : cookies) {
            cookie = cookie.trim();
            if (cookie.startsWith(cookieName + "=")) {
                return cookie.substring(cookieName.length() + 1);
            }
        }
        return null;
    }
}
