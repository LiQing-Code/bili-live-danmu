package cn.liqing.bili.live.danmu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    public @Nullable String cmd;
    public @Nullable JsonNode data;
    public @Nullable JsonNode info;
}
