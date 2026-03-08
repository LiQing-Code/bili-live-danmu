package cn.liqing.bili.live.danmu;

import cn.liqing.bili.live.danmu.handler.DanmuHandler;
import cn.liqing.bili.live.danmu.handler.EomjiHandler;
import cn.liqing.bili.live.danmu.handler.GiftHandler;
import cn.liqing.bili.live.danmu.handler.GuardHandler;
import cn.liqing.bili.live.danmu.handler.InteractiveHandler;
import cn.liqing.bili.live.danmu.handler.SuperChatHandler;
import cn.liqing.bili.live.danmu.model.Interactive;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveRoomIntegrationTest {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Test
    void shouldConnectRealRoomAndPrintEvents() throws Exception {
        String roomIdText = System.getenv("BILI_TEST_ROOM_ID");
        String cookie = System.getenv("BILI_TEST_COOKIE");
        Assumptions.assumeTrue(roomIdText != null && !roomIdText.isBlank(),
                "skip: set BILI_TEST_ROOM_ID to run live integration test");

        long roomId = Long.parseLong(roomIdText);
        int durationSeconds = Integer.parseInt(System.getenv().getOrDefault("BILI_TEST_DURATION_SECONDS", "90"));
        String rawMode = System.getenv().getOrDefault("BILI_TEST_RAW_MODE", "interactive");

        DanmuClient client = new DanmuClient();
        CountDownLatch opened = new CountDownLatch(1);

        client.setListener(new ConnectionListener() {
            @Override
            public void onOpen() {
                System.out.println("[OPEN] connected to room: " + roomId);
                opened.countDown();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("[CLOSE] code=" + code + ", reason=" + reason + ", remote=" + remote);
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("[ERROR] " + ex.getMessage());
                ex.printStackTrace(System.out);
            }
        });

        client.addHandler(new DanmuHandler(danmu ->
                System.out.println("[DANMU] " + danmu.user.name + ": " + danmu.body)));
        client.addHandler(new EomjiHandler(emoji ->
                System.out.println("[EMOJI] " + emoji.user.name + ": " + emoji.body + " (" + emoji.uri + ")")));
        client.addHandler(new GiftHandler(gift ->
                System.out.println("[GIFT] " + gift.user.name + " -> " + gift.name + " x" + gift.num +
                        ", price=" + gift.price)));
        client.addHandler(new GuardHandler(guard ->
                System.out.println("[GUARD] " + guard.user.name + " -> " + guard.name + " x" + guard.num +
                        ", price=" + guard.price)));
        client.addHandler(new InteractiveHandler(interactive ->
                System.out.println("[INTERACT] type=" + interactive.type
                        + "(" + Interactive.typeName(interactive.type) + ")"
                        + ", user=" + interactive.user.name)));
        client.addHandler(new SuperChatHandler(sc ->
                System.out.println("[SC] " + sc.user.name + ": " + sc.body +
                        " (price=" + sc.price + ", time=" + sc.time + "s)")));
        client.addHandler(new MessageHandler() {
            @Override
            public boolean canHandle(Message message) {
                return shouldPrintRaw(rawMode, message.cmd);
            }

            @Override
            public void handle(Message message) {
                System.out.println("[RAW] cmd=" + message.cmd);
                printJson("RAW-DATA", message.data);
                printJson("RAW-INFO", message.info);
                if (message.cmd != null && message.cmd.startsWith("INTERACT_WORD")) {
                    printPbSummary(message.data);
                    printPbDebug(message.data);
                }
            }
        });

        try {
            Auth auth;
            if (cookie != null && !cookie.isBlank()) {
                System.out.println("[AUTH] using cookie mode");
                auth = Auth.create(roomId, cookie);
            } else {
                System.out.println("[AUTH] using anonymous mode (set BILI_TEST_COOKIE for login mode)");
                auth = Auth.create(roomId);
            }
            client.connect(auth);

            boolean connected = opened.await(15, TimeUnit.SECONDS);
            assertTrue(connected, "websocket connect timeout");

            System.out.println("[RUNNING] listening for " + durationSeconds + " seconds...");
            Thread.sleep(durationSeconds * 1000L);
            System.out.println("[DONE] integration test finished");
        } finally {
            client.disconnect();
        }
    }

    private static boolean shouldPrintRaw(String rawMode, String cmd) {
        if ("off".equalsIgnoreCase(rawMode)) {
            return false;
        }
        if ("all".equalsIgnoreCase(rawMode)) {
            return true;
        }
        return cmd != null && cmd.startsWith("INTERACT_WORD");
    }

    private static void printJson(String title, JsonElement element) {
        if (element == null) {
            return;
        }
        System.out.println("[" + title + "] " + GSON.toJson(element));
    }

    private static void printPbDebug(JsonElement dataElement) {
        if (dataElement == null || !dataElement.isJsonObject()) {
            return;
        }
        JsonObject data = dataElement.getAsJsonObject();
        JsonElement pbElement = data.get("pb");
        if (pbElement == null || pbElement.isJsonNull()) {
            return;
        }
        String pb = pbElement.getAsString();
        if (pb.isBlank()) {
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(pb);
            System.out.println("[RAW-PB] bytes=" + bytes.length);
            StringBuilder sb = new StringBuilder();
            dumpProto(bytes, 0, bytes.length, 0, sb);
            System.out.print(sb);
        } catch (Exception ex) {
            System.out.println("[RAW-PB] decode-failed: " + ex.getMessage());
        }
    }

    private static void printPbSummary(JsonElement dataElement) {
        if (dataElement == null || !dataElement.isJsonObject()) {
            return;
        }
        JsonObject data = dataElement.getAsJsonObject();
        JsonElement pbElement = data.get("pb");
        if (pbElement == null || pbElement.isJsonNull()) {
            return;
        }
        String pb = pbElement.getAsString();
        if (pb.isBlank()) {
            return;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(pb);
            Long uid = null;
            String uname = null;
            Integer msgType = null;
            int i = 0;
            while (i < bytes.length) {
                long key = readVarint(bytes, i, bytes.length);
                int keyLen = varintLength(bytes, i, bytes.length);
                if (keyLen <= 0) {
                    break;
                }
                i += keyLen;
                int field = (int) (key >>> 3);
                int wire = (int) (key & 0x07);
                if (wire == 0) {
                    long v = readVarint(bytes, i, bytes.length);
                    int len = varintLength(bytes, i, bytes.length);
                    if (len <= 0) {
                        break;
                    }
                    i += len;
                    if (field == 1 && uid == null) {
                        uid = v;
                    } else if (field == 5 && msgType == null) {
                        msgType = (int) v;
                    }
                } else if (wire == 2) {
                    long lenLong = readVarint(bytes, i, bytes.length);
                    int lenLen = varintLength(bytes, i, bytes.length);
                    if (lenLen <= 0) {
                        break;
                    }
                    i += lenLen;
                    int len = (int) lenLong;
                    if (len < 0 || i + len > bytes.length) {
                        break;
                    }
                    if (field == 2 && uname == null) {
                        uname = new String(bytes, i, len, StandardCharsets.UTF_8);
                    }
                    i += len;
                } else if (wire == 1) {
                    i += 8;
                } else if (wire == 5) {
                    i += 4;
                } else {
                    break;
                }
            }
            if (uid != null || uname != null || msgType != null) {
                System.out.println("[PB-SUMMARY] uid=" + (uid == null ? "" : uid)
                        + ", uname=" + (uname == null ? "" : uname)
                        + ", msgType=" + (msgType == null ? "" : msgType));
            }
        } catch (Exception ignore) {
        }
    }

    private static void dumpProto(byte[] data, int start, int end, int depth, StringBuilder out) {
        int i = start;
        while (i < end) {
            long key = readVarint(data, i, end);
            int keyLen = varintLength(data, i, end);
            if (keyLen <= 0) {
                out.append(indent(depth)).append("! invalid key at ").append(i).append('\n');
                return;
            }
            i += keyLen;
            int field = (int) (key >>> 3);
            int wire = (int) (key & 0x07);
            out.append(indent(depth)).append("field=").append(field).append(", wire=").append(wire);

            switch (wire) {
                case 0 -> {
                    long v = readVarint(data, i, end);
                    int len = varintLength(data, i, end);
                    if (len <= 0) {
                        out.append(", ! invalid varint").append('\n');
                        return;
                    }
                    i += len;
                    out.append(", varint=").append(v).append('\n');
                }
                case 1 -> {
                    if (i + 8 > end) {
                        out.append(", ! invalid fixed64").append('\n');
                        return;
                    }
                    out.append(", fixed64=0x");
                    for (int p = i + 7; p >= i; p--) {
                        out.append(String.format("%02x", data[p] & 0xff));
                    }
                    out.append('\n');
                    i += 8;
                }
                case 2 -> {
                    long lenLong = readVarint(data, i, end);
                    int lenLen = varintLength(data, i, end);
                    if (lenLen <= 0) {
                        out.append(", ! invalid length").append('\n');
                        return;
                    }
                    i += lenLen;
                    int len = (int) lenLong;
                    if (len < 0 || i + len > end) {
                        out.append(", ! length overflow=").append(len).append('\n');
                        return;
                    }
                    out.append(", len=").append(len);
                    byte[] slice = new byte[len];
                    System.arraycopy(data, i, slice, 0, len);
                    String asText = new String(slice, StandardCharsets.UTF_8);
                    if (looksReadable(asText)) {
                        out.append(", text=").append(asText.replace('\n', ' '));
                    } else {
                        String b64 = Base64.getEncoder().encodeToString(slice);
                        if (b64.length() > 96) {
                            b64 = b64.substring(0, 96) + "...";
                        }
                        out.append(", b64=").append(b64);
                    }
                    out.append('\n');

                    if (len > 2) {
                        try {
                            dumpProto(slice, 0, slice.length, depth + 1, out);
                        } catch (Exception ignore) {
                        }
                    }
                    i += len;
                }
                case 5 -> {
                    if (i + 4 > end) {
                        out.append(", ! invalid fixed32").append('\n');
                        return;
                    }
                    int v = (data[i] & 0xff)
                            | ((data[i + 1] & 0xff) << 8)
                            | ((data[i + 2] & 0xff) << 16)
                            | ((data[i + 3] & 0xff) << 24);
                    i += 4;
                    out.append(", fixed32=").append(v).append('\n');
                }
                default -> {
                    out.append(", ! unsupported wire=").append(wire).append('\n');
                    return;
                }
            }
        }
    }

    private static long readVarint(byte[] data, int offset, int end) {
        long result = 0L;
        int shift = 0;
        for (int i = offset; i < end && shift < 64; i++) {
            int b = data[i] & 0xff;
            result |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IllegalArgumentException("invalid varint");
    }

    private static int varintLength(byte[] data, int offset, int end) {
        for (int i = offset; i < end && i < offset + 10; i++) {
            if ((data[i] & 0x80) == 0) {
                return i - offset + 1;
            }
        }
        return -1;
    }

    private static boolean looksReadable(String text) {
        if (text.isBlank()) {
            return false;
        }
        int bad = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                bad++;
            }
        }
        return bad <= Math.max(1, text.length() / 10);
    }

    private static String indent(int depth) {
        return "  ".repeat(Math.max(0, depth));
    }
}

