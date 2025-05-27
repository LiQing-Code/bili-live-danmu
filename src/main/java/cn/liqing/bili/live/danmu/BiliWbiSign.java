package cn.liqing.bili.live.danmu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BiliWbiSign {
    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String getMixinKey(String orig) {
        StringBuilder sb = new StringBuilder();
        for (int index : MIXIN_KEY_ENC_TAB) {
            if (index < orig.length()) {
                sb.append(orig.charAt(index));
            }
        }
        return sb.substring(0, 32);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public static String encWbi(Map<String, Object> params, String imgKey, String subKey) {
        String mixinKey = getMixinKey(imgKey + subKey);
        long currTime = System.currentTimeMillis() / 1000;
        params.put("wts", currTime);

        Map<String, String> encodedParams = new TreeMap<>();
        params.forEach((k, v) -> {
            String value = v.toString().replaceAll("[!'()*]", "");
            encodedParams.put(
                    urlEncode(k),
                    urlEncode(value)
            );
        });

        String queryString = encodedParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        String sign = md5(queryString + mixinKey);
        return queryString + "&w_rid=" + sign;
    }

    public static Map<String, String> getWbiKeys() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.bilibili.com/x/web-interface/nav"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(response.body());
        JsonNode wbiImg = root.path("data").path("wbi_img");

        String imgUrl = wbiImg.path("img_url").asText();
        String subUrl = wbiImg.path("sub_url").asText();

        return Map.of(
                "img_key", extractKeyFromUrl(imgUrl),
                "sub_key", extractKeyFromUrl(subUrl)
        );
    }

    private static String extractKeyFromUrl(String url) {
        int start = url.lastIndexOf("/") + 1;
        int end = url.lastIndexOf(".");
        return url.substring(start, end);
    }


    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~")
                .replace("*", "%2A");
    }

    public static String wbiSign(Map<String, Object> params) throws Exception {
        Map<String, String> keys = getWbiKeys();
        return encWbi(params, keys.get("img_key"), keys.get("sub_key"));
    }
}
