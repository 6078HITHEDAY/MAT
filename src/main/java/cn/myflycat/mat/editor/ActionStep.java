package cn.myflycat.mat.editor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ActionStep {
    public static final String TYPE_CHAT_SEND = "chat.send";
    public static final String TYPE_CHAT_LOG = "chat.log";
    public static final String TYPE_SLEEP = "sleep";
    public static final String TYPE_BARITONE_GOTO_BLOCK = "baritone.gotoBlock";
    public static final String TYPE_BARITONE_GOTO_XZ = "baritone.gotoXZ";
    public static final String TYPE_BARITONE_GOTO_NEAR = "baritone.gotoNear";
    public static final String TYPE_BARITONE_MINE = "baritone.mine";
    public static final String TYPE_BARITONE_CANCEL = "baritone.cancel";
    public static final String TYPE_BARITONE_IS_PATHING = "baritone.isPathing";
    public static final String TYPE_INVENTORY_SLOTS = "inventory.slots";
    public static final String TYPE_INVENTORY_FIND = "inventory.find";
    public static final String TYPE_INVENTORY_SELECT_HOTBAR = "inventory.selectHotbar";
    public static final String TYPE_INVENTORY_DROP = "inventory.drop";
    public static final String TYPE_INVENTORY_QUICK_MOVE = "inventory.quickMove";
    public static final String TYPE_INVENTORY_SWAP = "inventory.swap";
    public static final String TYPE_INVENTORY_CLOSE_CONTAINER = "inventory.closeContainer";
    public static final String TYPE_PLAYER_POS = "player.pos";
    public static final String TYPE_PLAYER_YAW = "player.yaw";
    public static final String TYPE_RECORDER_START = "recorder.start";
    public static final String TYPE_RECORDER_STOP = "recorder.stop";
    public static final String TYPE_RECORDER_SAVE = "recorder.save";
    public static final String TYPE_RECORDER_IS_RECORDING = "recorder.isRecording";
    public static final String TYPE_RECORD_BREAK_BLOCK = "record.breakBlock";
    public static final String TYPE_RECORD_PLACE_BLOCK = "record.placeBlock";
    public static final String TYPE_MOVE_TO = "move.to";

    private final String type;
    private final Map<String, String> params;
    private String comment;

    public ActionStep(String type) {
        this.type = Objects.requireNonNull(type);
        this.params = new LinkedHashMap<>();
        this.comment = "";
    }

    public String type() { return type; }
    public Map<String, String> params() { return params; }
    public String comment() { return comment; }
    public void setComment(String comment) { this.comment = comment != null ? comment : ""; }

    public void setParam(String key, String value) {
        params.put(key, value != null ? value : "");
    }

    public String param(String key) {
        return params.getOrDefault(key, "");
    }

    public String displayName() {
        return type;
    }

    public String paramsSummary() {
        if (params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (var entry : params.entrySet()) {
            String v = entry.getValue();
            if (v.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(v);
        }
        return sb.toString();
    }

    public String toJavaScript() {
        StringBuilder sb = new StringBuilder();
        if (!comment.isEmpty()) {
            sb.append("// ").append(comment).append("\n");
        }
        switch (type) {
            case TYPE_CHAT_SEND:
                sb.append("mat.chat.send(").append(quote(param("message"))).append(");");
                break;
            case TYPE_CHAT_LOG:
                sb.append("mat.chat.log(").append(quote(param("message"))).append(");");
                break;
            case TYPE_SLEEP:
                sb.append("mat.sleep(").append(num(param("ms"), "1000")).append(");");
                break;
            case TYPE_BARITONE_GOTO_BLOCK:
                sb.append("mat.baritone.gotoBlock(")
                        .append(num(param("x"), "0")).append(", ")
                        .append(num(param("y"), "64")).append(", ")
                        .append(num(param("z"), "0")).append(");");
                break;
            case TYPE_BARITONE_GOTO_XZ:
                sb.append("mat.baritone.gotoXZ(")
                        .append(num(param("x"), "0")).append(", ")
                        .append(num(param("z"), "0")).append(");");
                break;
            case TYPE_BARITONE_GOTO_NEAR:
                sb.append("mat.baritone.gotoNear(")
                        .append(num(param("x"), "0")).append(", ")
                        .append(num(param("y"), "64")).append(", ")
                        .append(num(param("z"), "0")).append(", ")
                        .append(num(param("radius"), "5")).append(");");
                break;
            case TYPE_BARITONE_MINE:
                sb.append("mat.baritone.mine(")
                        .append(num(param("count"), "1"));
                String blocks = param("blocks");
                if (!blocks.isEmpty()) {
                    for (String b : blocks.split(",")) {
                        sb.append(", ").append(quote(b.trim()));
                    }
                }
                sb.append(");");
                break;
            case TYPE_BARITONE_CANCEL:
                sb.append("mat.baritone.cancel();");
                break;
            case TYPE_BARITONE_IS_PATHING:
                sb.append("var pathing = mat.baritone.isPathing();");
                break;
            case TYPE_INVENTORY_SLOTS:
                sb.append("var slots = mat.inventory.slots();");
                break;
            case TYPE_INVENTORY_FIND:
                sb.append("mat.inventory.find(").append(quote(param("query"))).append(");");
                break;
            case TYPE_INVENTORY_SELECT_HOTBAR:
                sb.append("mat.inventory.selectHotbar(").append(num(param("slot"), "0")).append(");");
                break;
            case TYPE_INVENTORY_DROP:
                sb.append("mat.inventory.drop(").append(quote(param("ref"))).append(");");
                break;
            case TYPE_INVENTORY_QUICK_MOVE:
                sb.append("mat.inventory.quickMove(").append(quote(param("ref"))).append(");");
                break;
            case TYPE_INVENTORY_SWAP:
                sb.append("mat.inventory.swap(")
                        .append(num(param("hotbarSlot"), "0")).append(", ")
                        .append(quote(param("ref"))).append(");");
                break;
            case TYPE_INVENTORY_CLOSE_CONTAINER:
                sb.append("mat.inventory.closeContainer();");
                break;
            case TYPE_PLAYER_POS:
                sb.append("var pos = mat.player.pos();");
                break;
            case TYPE_PLAYER_YAW:
                sb.append("var yaw = mat.player.yaw();");
                break;
            case TYPE_RECORDER_START:
                sb.append("mat.recorder.start();");
                break;
            case TYPE_RECORDER_STOP:
                sb.append("mat.recorder.stop();");
                break;
            case TYPE_RECORDER_SAVE:
                sb.append("mat.recorder.save(").append(quote(param("name"))).append(");");
                break;
            case TYPE_RECORDER_IS_RECORDING:
                sb.append("var recording = mat.recorder.isRecording();");
                break;
            case TYPE_RECORD_BREAK_BLOCK:
                sb.append("// break block at (").append(blank(param("x"), "?"))
                        .append(", ").append(blank(param("y"), "?"))
                        .append(", ").append(blank(param("z"), "?")).append(")\n");
                sb.append("mat.baritone.gotoBlock(")
                        .append(num(param("x"), "0")).append(", ")
                        .append(num(param("y"), "0")).append(", ")
                        .append(num(param("z"), "0")).append(");\n");
                sb.append("// TODO: break block after arrival");
                break;
            case TYPE_RECORD_PLACE_BLOCK:
                sb.append("// place block at (").append(blank(param("x"), "?"))
                        .append(", ").append(blank(param("y"), "?"))
                        .append(", ").append(blank(param("z"), "?")).append(")\n");
                sb.append("mat.baritone.gotoBlock(")
                        .append(num(param("x"), "0")).append(", ")
                        .append(num(param("y"), "0")).append(", ")
                        .append(num(param("z"), "0")).append(");\n");
                sb.append("// TODO: place block after arrival");
                break;
            case TYPE_MOVE_TO:
                sb.append("mat.baritone.gotoBlock(")
                        .append(num(param("x"), "0")).append(", ")
                        .append(num(param("y"), "64")).append(", ")
                        .append(num(param("z"), "0")).append(");");
                break;
            default:
                sb.append("// unknown action: ").append(type);
                break;
        }
        return sb.toString();
    }

    private static String num(String val, String fallback) {
        if (val == null || val.isEmpty()) return fallback;
        try {
            Integer.parseInt(val);
            return val;
        } catch (NumberFormatException e) {
            try {
                double d = Double.parseDouble(val);
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((int) d);
                }
                return val;
            } catch (NumberFormatException e2) {
                return fallback;
            }
        }
    }

    private static String blank(String val, String fallback) {
        return (val == null || val.isEmpty()) ? fallback : val;
    }

    private static String quote(String s) {
        if (s == null || s.isEmpty()) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
