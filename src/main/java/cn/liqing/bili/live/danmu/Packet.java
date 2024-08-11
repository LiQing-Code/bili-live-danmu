package cn.liqing.bili.live.danmu;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

public class Packet {
    static final Logger LOGGER = LoggerFactory.getLogger(Packet.class);
    public static final short HEADER_LENGTH = 16;
    public static final int SEQUENCE_ID = 0;
    public final byte[] body;
    public final Operation operation;

    /**
     * 如果Version=0，Body中就是实际发送的数据。
     * 如果Version=2，Body中是经过压缩后的数据，请使用zlib解压，然后解析。
     */
    public final short version = 0;

    public int length() {
        return body.length + HEADER_LENGTH;
    }

    public Packet(Operation operation, byte[] body) {
        this.operation = operation;
        this.body = body;
    }

    public ByteBuffer pack() {
        int length = length();
        var buffer = ByteBuffer.allocate(length);

        buffer.putInt(length);
        buffer.putShort(HEADER_LENGTH);
        buffer.putShort(version);
        buffer.putInt(operation.code);
        buffer.putInt(SEQUENCE_ID);
        buffer.put(body);

        buffer.position(0);
        return buffer;
    }

    public static @NotNull ArrayList<Packet> unPack(@NotNull ByteBuffer buffer) {
        var packs = new ArrayList<Packet>();
        int offset = 0;
        int len = buffer.limit();
        while (offset + HEADER_LENGTH <= len) {
            int bodyLen = buffer.getInt(offset) - HEADER_LENGTH;
            try {
                short ver = buffer.getShort(offset + 6);
                Operation op = Operation.parse(buffer.getInt(8));
                byte[] body = new byte[bodyLen];
                buffer.get(offset + HEADER_LENGTH, body, 0, bodyLen);

                if (ver == 2) {
                    try {
                        body = decompress(body);
                        packs.addAll(unPack(ByteBuffer.wrap(body)));
                    } catch (IOException e) {
                        LOGGER.error("解压失败", e);
                    }
                    continue;
                }

                packs.add(new Packet(op, body));
            } finally {
                offset += bodyLen + HEADER_LENGTH;
            }
        }
        return packs;
    }

    public static byte @NotNull [] decompress(byte @NotNull [] data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        InflaterInputStream iis = new InflaterInputStream(inputStream);
        return iis.readAllBytes();
    }

    public enum Operation {
        /**
         * 客户端发送的心跳包(30秒发送一次)
         */
        HEARTBEAT(2),
        /**
         * 服务器收到心跳包的回复
         */
        HEARTBEAT_REPLY(3),
        /**
         * 服务器推送的弹幕消息包
         */
        SEND_SMS_REPLY(5),
        /**
         * 客户端发送的鉴权包(客户端发送的第一个包)
         */
        AUTH(7),
        /**
         * 服务器收到鉴权包后的回复
         */
        AUTH_REPLY(8);
        public final int code;

        Operation(int code) {
            this.code = code;
        }

        public static Operation parse(int code) {
            return switch (code) {
                case 2 -> Operation.HEARTBEAT;
                case 3 -> Operation.HEARTBEAT_REPLY;
                case 5 -> Operation.SEND_SMS_REPLY;
                case 7 -> Operation.AUTH;
                case 8 -> Operation.AUTH_REPLY;
                default -> throw new IllegalStateException("Unexpected value: " + code);
            };
        }
    }
}
