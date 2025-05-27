package cn.liqing.bili.live.danmu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Auth {
    public final long uid;
    public final long roomid;
    @SuppressWarnings("unused")
    public final int protover = 2;
    public final String buvid;
    @SuppressWarnings("unused")
    public final String platform = "web";
    @SuppressWarnings("unused")
    public final int type = 2;
    public final String key;

    public Auth(long roomid, long uid, String buvid, String key) {
        this.roomid = roomid;
        this.uid = uid;
        this.buvid = buvid;
        this.key = key;
    }

    public static @NotNull Auth create(long roomid, String cookie) throws IOException, URISyntaxException, InterruptedException {
        String key = GetKey(roomid, cookie);
        String buvid = extractCookieValue("buvid3", cookie);
        long uid = Long.parseLong(Objects.requireNonNull(extractCookieValue("DedeUserID", cookie)));
        return new Auth(roomid, uid, buvid, key);
    }

    public static @NotNull Auth create(long roomid) throws IOException, URISyntaxException, InterruptedException {
        String key = GetKey(roomid, "");
        String buvid = GeneratedUUID();
        return new Auth(roomid, 0, buvid, key);
    }

    private static String GetKey(long roomId, String cookie) throws IOException, InterruptedException, URISyntaxException {
        HttpResponse<String> response;
        HttpClient client = HttpClient.newHttpClient();

        Map<String, Object> params = new HashMap<>();
        params.put("id", roomId);
        params.put("type", "0");

        String signedQuery;
        try {
            signedQuery = BiliWbiSign.wbiSign(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String baseUrl = "https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo";
        String fullUrl = baseUrl + "?" + signedQuery;

        HttpRequest request = HttpRequest.newBuilder(URI.create(fullUrl))
                .header("Accept", "*/*")
                .header("Cookie", cookie).build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode rootNode = new ObjectMapper().readTree(response.body());
        return rootNode.path("data").path("token").asText();
    }

    private static @Nullable String extractCookieValue(String cookieName, @NotNull String cookieString) {
        String[] cookies = cookieString.split(";");

        for (String cookie : cookies) {
            cookie = cookie.trim(); // 去除首尾空格

            if (cookie.startsWith(cookieName + "=")) {
                // 找到对应的 Cookie，提取值
                return cookie.substring(cookieName.length() + 1); // 加上等号长度
            }
        }

        // 没有找到对应的 Cookie
        return null;
    }

    // 生成随机的UUID
    private static @NotNull String GeneratedUUID() {
        UUID uuid = UUID.randomUUID();

        // 获取当前时间的毫秒数，用来生成后缀
        long currentTimeMillis = System.currentTimeMillis();
        String suffix = String.format("%05d", currentTimeMillis % 100000); // 取后五位，不足五位补零

        // 拼接结果
        return String.format("%s-%s-%s-%s-%s%sinfoc",
                uuid.toString().substring(0, 8),
                uuid.toString().substring(9, 13),
                uuid.toString().substring(14, 18),
                uuid.toString().substring(19, 23),
                uuid.toString().substring(24, 36),
                suffix);
    }
}
