package cn.liqing.bili.live.danmu;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Auth {
    private static final Gson GSON = new Gson();
    
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

    public static @NotNull Auth create(long roomid, String cookie) 
            throws IOException, InterruptedException {
        String key = getKey(roomid, cookie);
        String buvid = extractCookieValue("buvid3", cookie);
        long uid = Long.parseLong(Objects.requireNonNull(
            extractCookieValue("DedeUserID", cookie)));
        return new Auth(roomid, uid, buvid, key);
    }

    public static @NotNull Auth create(long roomid) 
            throws IOException, InterruptedException {
        String key = getKey(roomid, "");
        String buvid = generateUUID();
        return new Auth(roomid, 0, buvid, key);
    }

    private static String getKey(long roomId, String cookie) 
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        Map<String, Object> params = new HashMap<>();
        params.put("id", roomId);
        params.put("type", "0");

        String signedQuery;
        try {
            signedQuery = BiliWbiSign.wbiSign(params);
        } catch (Exception e) {
            throw new RuntimeException("签名失败", e);
        }
        
        String baseUrl = "https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo";
        String fullUrl = baseUrl + "?" + signedQuery;

        HttpRequest request = HttpRequest.newBuilder(URI.create(fullUrl))
                .header("Accept", "*/*")
                .header("Cookie", cookie)
                .build();

        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
        return root.getAsJsonObject("data")
                   .get("token")
                   .getAsString();
    }

    private static @Nullable String extractCookieValue(String cookieName, 
                                                       @NotNull String cookieString) {
        String[] cookies = cookieString.split(";");
        for (String cookie : cookies) {
            cookie = cookie.trim();
            if (cookie.startsWith(cookieName + "=")) {
                return cookie.substring(cookieName.length() + 1);
            }
        }
        return null;
    }

    private static @NotNull String generateUUID() {
        UUID uuid = UUID.randomUUID();
        long currentTimeMillis = System.currentTimeMillis();
        String timestampSuffix = String.valueOf(currentTimeMillis).substring(0, 5);
        return uuid.toString().replace("-", "") + timestampSuffix + "infoc";
    }
}
