package cn.myflycat.mat.recorder;

import java.util.Collections;
import java.util.Map;

public final class ActionRecord {
    private final ActionType type;
    private final long timestamp;
    private final Map<String, Object> params;

    public ActionRecord(ActionType type, long timestamp, Map<String, Object> params) {
        this.type = type;
        this.timestamp = timestamp;
        this.params = Map.copyOf(params);
    }

    public ActionType type() { return type; }
    public long timestamp() { return timestamp; }
    public Map<String, Object> params() { return params; }

    public Object param(String key) { return params.get(key); }

    @SuppressWarnings("unchecked")
    public <T> T param(String key, T fallback) {
        Object v = params.get(key);
        return v != null ? (T) v : fallback;
    }

    @Override
    public String toString() {
        return type + " @" + timestamp + "ms " + params;
    }
}
