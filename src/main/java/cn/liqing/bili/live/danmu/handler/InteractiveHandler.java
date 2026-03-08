package cn.liqing.bili.live.danmu.handler;

import cn.liqing.bili.live.danmu.Message;
import cn.liqing.bili.live.danmu.MessageHandler;
import cn.liqing.bili.live.danmu.User;
import cn.liqing.bili.live.danmu.model.Interactive;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

public class InteractiveHandler implements MessageHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(InteractiveHandler.class);
    private final Consumer<Interactive> onInteractive;

    public InteractiveHandler(Consumer<Interactive> onInteractive) {
        this.onInteractive = onInteractive;
    }

    @Override
    public boolean canHandle(@NotNull Message message) {
        return message.cmd != null && message.cmd.startsWith("INTERACT_WORD");
    }

    @Override
    public void handle(Message message) {
        try {
            JsonElement data = message.data;
            if (data == null || !data.isJsonObject()) {
                LOGGER.error("interactive payload has no json data");
                return;
            }
            JsonObject dataObj = data.getAsJsonObject();

            var interactive = new Interactive();
            interactive.user.uid = getString(dataObj, "uid", getNestedString(dataObj, "uinfo", "uid"));
            interactive.user.name = getString(dataObj, "uname", getNestedString(dataObj, "uinfo", "base", "name"));

            JsonElement fansMedal = dataObj.get("fans_medal");
            if (fansMedal != null && fansMedal.isJsonObject()) {
                JsonObject medalObj = fansMedal.getAsJsonObject();
                interactive.user.fansMedal = new User.FansMedal();
                interactive.user.fansMedal.name = getString(medalObj, "medal_name", "");
                interactive.user.fansMedal.level = getInt(medalObj, "medal_level", 0);
                interactive.user.guardLevel = getInt(medalObj, "guard_level", 0);
                if (interactive.user.fansMedal.name.isEmpty()) {
                    interactive.user.fansMedal = null;
                }
            }

            interactive.type = getInt(dataObj, "msg_type", getInt(dataObj, "trigger_type", 0));

            if ((isBlank(interactive.user.uid) || isBlank(interactive.user.name) || interactive.type == 0)
                    && dataObj.has("pb")
                    && dataObj.get("pb") != null
                    && !dataObj.get("pb").isJsonNull()) {
                applyPbFallback(interactive, dataObj.get("pb").getAsString());
            }

            onInteractive.accept(interactive);
        } catch (Exception ex) {
            LOGGER.error("failed to parse interactive message", ex);
        }
    }

    private static void applyPbFallback(Interactive interactive, String pbBase64) {
        if (isBlank(pbBase64)) {
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(pbBase64);
            int i = 0;
            while (i < bytes.length) {
                long key = readVarint(bytes, i, bytes.length);
                int keyLen = varintLength(bytes, i, bytes.length);
                if (keyLen <= 0) {
                    return;
                }
                i += keyLen;

                int field = (int) (key >>> 3);
                int wire = (int) (key & 0x07);

                if (wire == 0) {
                    long value = readVarint(bytes, i, bytes.length);
                    int valueLen = varintLength(bytes, i, bytes.length);
                    if (valueLen <= 0) {
                        return;
                    }
                    i += valueLen;

                    if (field == 1 && isBlank(interactive.user.uid)) {
                        interactive.user.uid = Long.toUnsignedString(value);
                    } else if (field == 5 && interactive.type == 0) {
                        interactive.type = (int) value;
                    }
                } else if (wire == 2) {
                    long lenLong = readVarint(bytes, i, bytes.length);
                    int lenLen = varintLength(bytes, i, bytes.length);
                    if (lenLen <= 0) {
                        return;
                    }
                    i += lenLen;
                    int len = (int) lenLong;
                    if (len < 0 || i + len > bytes.length) {
                        return;
                    }

                    if (field == 2 && isBlank(interactive.user.name)) {
                        interactive.user.name = new String(bytes, i, len, StandardCharsets.UTF_8);
                    }

                    i += len;
                } else if (wire == 1) {
                    i += 8;
                } else if (wire == 5) {
                    i += 4;
                } else {
                    return;
                }

                if (i > bytes.length) {
                    return;
                }
            }
        } catch (Exception ignore) {
        }
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsString();
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsInt();
    }

    private static String getNestedString(JsonObject obj, String... path) {
        JsonObject current = obj;
        for (int i = 0; i < path.length - 1; i++) {
            JsonElement next = current.get(path[i]);
            if (next == null || !next.isJsonObject()) {
                return "";
            }
            current = next.getAsJsonObject();
        }
        JsonElement target = current.get(path[path.length - 1]);
        if (target == null || target.isJsonNull()) {
            return "";
        }
        return target.getAsString();
    }

    private static int varintLength(byte[] data, int offset, int end) {
        for (int i = offset; i < end && i < offset + 10; i++) {
            if ((data[i] & 0x80) == 0) {
                return i - offset + 1;
            }
        }
        return -1;
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
