package cn.myflycat.mat.inventory;

import org.graalvm.polyglot.Value;

public final class ItemQuery {
    public final String id;
    public final String tag;
    public final Integer minCount;
    public final String container;

    private ItemQuery(String id, String tag, Integer minCount, String container) {
        this.id = id;
        this.tag = tag;
        this.minCount = minCount;
        this.container = container;
    }

    public boolean isEmpty() {
        return id == null && tag == null && minCount == null && container == null;
    }

    public static ItemQuery any() {
        return new ItemQuery(null, null, null, null);
    }

    public static ItemQuery byId(String id) {
        return new ItemQuery(id, null, null, null);
    }

    public static ItemQuery fromValue(Value v) {
        if (v == null || v.isNull()) return any();
        if (v.isString()) return byId(v.asString());
        if (!v.hasMembers()) return any();

        String id = optString(v, "id");
        String tag = optString(v, "tag");
        Integer minCount = optInt(v, "minCount");
        String container = optString(v, "container");
        return new ItemQuery(id, tag, minCount, container);
    }

    private static String optString(Value v, String key) {
        if (!v.hasMember(key)) return null;
        Value m = v.getMember(key);
        return (m == null || m.isNull()) ? null : m.asString();
    }

    private static Integer optInt(Value v, String key) {
        if (!v.hasMember(key)) return null;
        Value m = v.getMember(key);
        return (m == null || m.isNull()) ? null : m.asInt();
    }
}
