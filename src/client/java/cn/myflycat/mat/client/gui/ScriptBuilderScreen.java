package cn.myflycat.mat.client.gui;

import cn.myflycat.mat.editor.ActionStep;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

public class ScriptBuilderScreen extends Screen {
    private static final int PALETTE_WIDTH = 150;
    private static final int SEQUENCE_WIDTH = 250;
    private static final int STEP_HEIGHT = 24;
    private static final int PALETTE_ITEM_HEIGHT = 18;
    private static final int PANEL_TOP = 24;
    private static final int HEADER_H = 14;

    private static final int BG = 0xC0101010;
    private static final int PANEL_BG = 0xCC2C2C2C;
    private static final int BORDER = 0xFF555555;
    private static final int STEP_BG = 0xFF3C3C3C;
    private static final int STEP_SEL = 0xFF5C8C3C;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HOVER_BG = 0xFF5C5C5C;
    private static final int CAT_COLOR = 0xFFAAAAAA;

    private record ParamDef(String key, String label, boolean numeric) {}
    private record PalEntry(String text, boolean header) {}

    private static final Map<String, List<ParamDef>> PARAM_DEFS = new LinkedHashMap<>();

    static {
        PARAM_DEFS.put(ActionStep.TYPE_CHAT_SEND, List.of(new ParamDef("message", "Message", false)));
        PARAM_DEFS.put(ActionStep.TYPE_CHAT_LOG, List.of(new ParamDef("message", "Message", false)));
        PARAM_DEFS.put(ActionStep.TYPE_SLEEP, List.of(new ParamDef("ms", "MS", true)));
        PARAM_DEFS.put(ActionStep.TYPE_BARITONE_GOTO_BLOCK, List.of(
                new ParamDef("x", "X", true), new ParamDef("y", "Y", true), new ParamDef("z", "Z", true)));
        PARAM_DEFS.put(ActionStep.TYPE_BARITONE_GOTO_XZ, List.of(
                new ParamDef("x", "X", true), new ParamDef("z", "Z", true)));
        PARAM_DEFS.put(ActionStep.TYPE_BARITONE_GOTO_NEAR, List.of(
                new ParamDef("x", "X", true), new ParamDef("y", "Y", true),
                new ParamDef("z", "Z", true), new ParamDef("radius", "Radius", true)));
        PARAM_DEFS.put(ActionStep.TYPE_BARITONE_MINE, List.of(
                new ParamDef("count", "Count", true), new ParamDef("blocks", "Blocks", false)));
        PARAM_DEFS.put(ActionStep.TYPE_BARITONE_CANCEL, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_BARITONE_IS_PATHING, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_INVENTORY_SLOTS, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_INVENTORY_FIND, List.of(new ParamDef("query", "Query", false)));
        PARAM_DEFS.put(ActionStep.TYPE_INVENTORY_SELECT_HOTBAR, List.of(new ParamDef("slot", "Slot", true)));
        PARAM_DEFS.put(ActionStep.TYPE_INVENTORY_DROP, List.of(new ParamDef("ref", "Ref", false)));
        PARAM_DEFS.put(ActionStep.TYPE_INVENTORY_QUICK_MOVE, List.of(new ParamDef("ref", "Ref", false)));
        PARAM_DEFS.put(ActionStep.TYPE_INVENTORY_SWAP, List.of(
                new ParamDef("hotbarSlot", "Slot", true), new ParamDef("ref", "Ref", false)));
        PARAM_DEFS.put(ActionStep.TYPE_INVENTORY_CLOSE_CONTAINER, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_PLAYER_POS, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_PLAYER_YAW, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_RECORDER_START, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_RECORDER_STOP, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_RECORDER_SAVE, List.of(new ParamDef("name", "Name", false)));
        PARAM_DEFS.put(ActionStep.TYPE_RECORDER_IS_RECORDING, List.of());
        PARAM_DEFS.put(ActionStep.TYPE_RECORD_BREAK_BLOCK, List.of(
                new ParamDef("x", "X", true), new ParamDef("y", "Y", true), new ParamDef("z", "Z", true)));
        PARAM_DEFS.put(ActionStep.TYPE_RECORD_PLACE_BLOCK, List.of(
                new ParamDef("x", "X", true), new ParamDef("y", "Y", true), new ParamDef("z", "Z", true)));
        PARAM_DEFS.put(ActionStep.TYPE_MOVE_TO, List.of(
                new ParamDef("x", "X", true), new ParamDef("y", "Y", true), new ParamDef("z", "Z", true)));
    }

    private final String scriptName;
    private final List<ActionStep> steps = new ArrayList<>();
    private final List<PalEntry> palette = new ArrayList<>();
    private final List<TextFieldWidget> editFields = new ArrayList<>();

    private int editingIndex = -1;
    private int palScroll = 0;
    private int seqScroll = 0;

    private boolean dragging = false;
    private int dragIndex = -1;

    // Cached during render for mouse-event use
    private int seqPanelX;
    private int previewPanelX;
    private int doneX, doneY = -1, doneW = 60, doneH = 16;

    public ScriptBuilderScreen(String scriptName) {
        super(Text.literal("Script Builder — " + scriptName));
        this.scriptName = scriptName;
        buildPalette();
    }

    // ---- palette entries ---------------------------------------------------

    private void buildPalette() {
        cat("Chat",    ActionStep.TYPE_CHAT_SEND, ActionStep.TYPE_CHAT_LOG);
        cat("Control", ActionStep.TYPE_SLEEP);
        cat("Movement", ActionStep.TYPE_BARITONE_GOTO_BLOCK,
                ActionStep.TYPE_BARITONE_GOTO_XZ, ActionStep.TYPE_BARITONE_GOTO_NEAR,
                ActionStep.TYPE_MOVE_TO);
        cat("Mining",  ActionStep.TYPE_BARITONE_MINE, ActionStep.TYPE_BARITONE_CANCEL,
                ActionStep.TYPE_BARITONE_IS_PATHING,
                ActionStep.TYPE_RECORD_BREAK_BLOCK, ActionStep.TYPE_RECORD_PLACE_BLOCK);
        cat("Inventory", ActionStep.TYPE_INVENTORY_SLOTS, ActionStep.TYPE_INVENTORY_FIND,
                ActionStep.TYPE_INVENTORY_SELECT_HOTBAR, ActionStep.TYPE_INVENTORY_DROP,
                ActionStep.TYPE_INVENTORY_QUICK_MOVE, ActionStep.TYPE_INVENTORY_SWAP,
                ActionStep.TYPE_INVENTORY_CLOSE_CONTAINER);
        cat("Player",  ActionStep.TYPE_PLAYER_POS, ActionStep.TYPE_PLAYER_YAW);
        cat("Recorder", ActionStep.TYPE_RECORDER_START, ActionStep.TYPE_RECORDER_STOP,
                ActionStep.TYPE_RECORDER_SAVE, ActionStep.TYPE_RECORDER_IS_RECORDING);
    }

    private void cat(String name, String... types) {
        palette.add(new PalEntry(name, true));
        for (String t : types) palette.add(new PalEntry(t, false));
    }

    // ---- init ---------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Exit"), b -> {
            saveScript();
            close();
        }).dimensions(width - 180, 2, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> close())
                .dimensions(width - 22, 2, 20, 20).build());
    }

    // ---- render -------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        seqPanelX = 4 + PALETTE_WIDTH + 4;
        previewPanelX = seqPanelX + SEQUENCE_WIDTH + 4;
        int panelRight = width - 4;
        int panelH = height - PANEL_TOP - 4;

        // Full background
        ctx.fill(0, 0, width, height, BG);

        // Panel backgrounds and separators
        ctx.fill(4, PANEL_TOP, 4 + PALETTE_WIDTH, PANEL_TOP + panelH, PANEL_BG);
        ctx.fill(seqPanelX, PANEL_TOP, seqPanelX + SEQUENCE_WIDTH, PANEL_TOP + panelH, PANEL_BG);
        ctx.fill(previewPanelX, PANEL_TOP, previewPanelX + (panelRight - previewPanelX), PANEL_TOP + panelH, PANEL_BG);

        // Vertical separator lines
        int sep = PANEL_TOP;
        int sepEnd = PANEL_TOP + panelH;
        ctx.fill(4 + PALETTE_WIDTH, sep, 4 + PALETTE_WIDTH + 1, sepEnd, BORDER);
        ctx.fill(seqPanelX + SEQUENCE_WIDTH, sep, seqPanelX + SEQUENCE_WIDTH + 1, sepEnd, BORDER);
        ctx.fill(previewPanelX, sep, previewPanelX + 1, sepEnd, BORDER);

        // Title
        ctx.drawText(textRenderer, Text.literal("Script Builder — " + scriptName + ".js"),
                5, 6, TEXT_COLOR, true);

        renderPalette(ctx, mx, my, delta, 4, PANEL_TOP, PALETTE_WIDTH, panelH);
        renderSequence(ctx, mx, my, delta, seqPanelX, PANEL_TOP, SEQUENCE_WIDTH, panelH);
        renderPreview(ctx, previewPanelX, PANEL_TOP, panelRight - previewPanelX, panelH);

        // Drag ghost
        if (dragging && dragIndex >= 0 && dragIndex < steps.size()) {
            int ghostX = seqPanelX + 4;
            int ghostY = my - STEP_HEIGHT / 2;
            ctx.fill(ghostX, ghostY, ghostX + SEQUENCE_WIDTH - 8, ghostY + STEP_HEIGHT, 0xAA5C8C3C);
            String label = (dragIndex + 1) + ". " + steps.get(dragIndex).displayName();
            ctx.drawText(textRenderer, "≡ " + label, ghostX + 4, ghostY + 4, TEXT_COLOR, true);
        }
    }

    // ---- palette ------------------------------------------------------------

    private void renderPalette(DrawContext ctx, int mx, int my, float delta,
                               int px, int py, int pw, int ph) {
        ctx.enableScissor(px, py, pw, ph);
        ctx.drawText(textRenderer, "Actions", px + 4, py + 2, CAT_COLOR, true);

        int y = py + HEADER_H - palScroll;
        for (PalEntry entry : palette) {
            if (y + PALETTE_ITEM_HEIGHT < py) { y += PALETTE_ITEM_HEIGHT; continue; }
            if (y > py + ph) break;

            if (entry.header()) {
                ctx.drawText(textRenderer, entry.text(), px + 4, y + 2, CAT_COLOR, true);
            } else {
                if (mx >= px && mx <= px + pw && my >= y && my <= y + PALETTE_ITEM_HEIGHT) {
                    ctx.fill(px + 2, y, px + pw - 2, y + PALETTE_ITEM_HEIGHT, HOVER_BG);
                }
                ctx.drawText(textRenderer, entry.text(), px + 8, y + 3, TEXT_COLOR, true);
            }
            y += PALETTE_ITEM_HEIGHT;
        }
        ctx.disableScissor();
    }

    // ---- sequence -----------------------------------------------------------

    private void renderSequence(DrawContext ctx, int mx, int my, float delta,
                                int sx, int sy, int sw, int ph) {
        ctx.enableScissor(sx, sy, sw, ph);
        ctx.drawText(textRenderer, "Sequence", sx + 4, sy + 2, CAT_COLOR, true);

        int y = sy + HEADER_H - seqScroll;
        for (int i = 0; i < steps.size(); i++) {
            int stepH = getStepHeight(i);
            if (y + stepH < sy) { y += stepH; continue; }
            if (y > sy + ph) break;

            ActionStep step = steps.get(i);
            boolean editing = i == editingIndex;
            boolean hovered = mx >= sx && mx <= sx + sw && my >= y && my <= y + STEP_HEIGHT;

            // Step background
            int bg;
            if (dragging && i == dragIndex) {
                bg = 0x661C1C1C; // dimmed while dragging
            } else if (editing) {
                bg = STEP_SEL;
            } else if (hovered) {
                bg = HOVER_BG;
            } else {
                bg = STEP_BG;
            }
            ctx.fill(sx + 2, y, sx + sw - 2, y + STEP_HEIGHT, bg);

            // Drag handle
            ctx.drawText(textRenderer, "≡", sx + 4, y + 4, 0xFF888888, true);

            // Label + summary
            StringBuilder sb = new StringBuilder();
            sb.append(i + 1).append(". ").append(step.displayName());
            String summary = step.paramsSummary();
            if (!summary.isEmpty()) sb.append(" (").append(summary).append(")");
            ctx.drawText(textRenderer, sb.toString(), sx + 20, y + 5, TEXT_COLOR, true);

            y += STEP_HEIGHT;

            // Editing area
            if (editing) {
                renderEditArea(ctx, mx, my, delta, sx, y, sw, i);
                y += getEditHeight(i);
            }
        }
        ctx.disableScissor();
    }

    private void renderEditArea(DrawContext ctx, int mx, int my, float delta,
                                int sx, int y, int sw, int index) {
        ActionStep step = steps.get(index);
        List<ParamDef> defs = PARAM_DEFS.getOrDefault(step.type(), List.of());
        if (defs.isEmpty()) return;

        // Compute max label width
        int maxW = 0;
        for (ParamDef def : defs) {
            maxW = Math.max(maxW, textRenderer.getWidth(def.label() + ": "));
        }

        int fx = sx + 4;
        int fy = y + 2;

        for (int i = 0; i < defs.size(); i++) {
            ParamDef def = defs.get(i);
            ctx.drawText(textRenderer, def.label() + ":", fx, fy + 2, CAT_COLOR, true);
            if (i < editFields.size()) {
                TextFieldWidget f = editFields.get(i);
                f.setX(fx + maxW);
                f.setY(fy);
                f.setWidth(sw - maxW - 10);
                f.render(ctx, mx, my, delta);
            }
            fy += 18;
        }

        // Done button
        doneX = fx;
        doneY = fy + 3;
        int dr = doneX + doneW;
        int db = doneY + doneH;
        boolean overDone = mx >= doneX && mx <= dr && my >= doneY && my <= db;
        ctx.fill(doneX, doneY, dr, db, overDone ? 0xFF777777 : 0xFF555555);
        ctx.drawText(textRenderer, "Done", doneX + 16, doneY + 3, TEXT_COLOR, true);
    }

    // ---- preview -----------------------------------------------------------

    private void renderPreview(DrawContext ctx, int px, int py, int pw, int ph) {
        ctx.enableScissor(px, py, pw, ph);
        ctx.drawText(textRenderer, "Preview", px + 4, py + 2, CAT_COLOR, true);

        String code = ScriptCodegen.generate(scriptName, steps);
        if (!code.isEmpty()) {
            String[] lines = code.split("\n", -1);
            int y = py + HEADER_H + 2;
            for (String line : lines) {
                if (y + textRenderer.fontHeight > py + ph) break;
                ctx.drawText(textRenderer, line, px + 4, y, 0xFF88FF88, true);
                y += textRenderer.fontHeight + 1;
            }
        }
        ctx.disableScissor();
    }

    // ---- mouse events -------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;

        // Editing: text fields and Done button
        if (editingIndex >= 0) {
            for (TextFieldWidget f : editFields) {
                if (f.mouseClicked(mx, my, button)) return true;
            }
            if (button == 0 && doneY >= 0
                    && mx >= doneX && mx <= doneX + doneW
                    && my >= doneY && my <= doneY + doneH) {
                commitEdit();
                return true;
            }
        }

        // Palette panel
        if (mx >= 4 && mx <= 4 + PALETTE_WIDTH && my >= PANEL_TOP) {
            PalEntry entry = getPalEntryAt(my);
            if (entry != null && !entry.header()) {
                if (editingIndex >= 0) commitEdit();
                steps.add(new ActionStep(entry.text()));
                return true;
            }
            return false;
        }

        // Sequence panel
        if (mx >= seqPanelX && mx <= seqPanelX + SEQUENCE_WIDTH && my >= PANEL_TOP) {
            int idx = getSeqIndexAt(my);
            if (idx >= 0 && idx < steps.size()) {
                if (button == 1) {
                    // Right-click: delete
                    steps.remove(idx);
                    if (editingIndex == idx) cancelEdit();
                    else if (editingIndex > idx) editingIndex--;
                    return true;
                }
                // Left click
                if (mx - seqPanelX < 20) {
                    // Drag handle
                    dragging = true;
                    dragIndex = idx;
                } else {
                    // Select for editing
                    startEdit(idx);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && button == 0 && dragIndex >= 0 && dragIndex < steps.size()) {
            int dropIdx = getSeqIndexAt(my);
            if (dropIdx >= 0 && dropIdx != dragIndex && dropIdx != dragIndex + 1) {
                if (editingIndex >= 0) commitEdit();
                ActionStep step = steps.remove(dragIndex);
                if (dropIdx > dragIndex) dropIdx--;
                steps.add(dropIdx, step);
            }
            dragging = false;
            dragIndex = -1;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
        if (mx >= 4 && mx <= 4 + PALETTE_WIDTH && my >= PANEL_TOP) {
            palScroll = clampScroll(palScroll - (int) (verticalAmount * 12), getMaxPalScroll());
            return true;
        }
        if (mx >= seqPanelX && mx <= seqPanelX + SEQUENCE_WIDTH && my >= PANEL_TOP) {
            seqScroll = clampScroll(seqScroll - (int) (verticalAmount * 12), getMaxSeqScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount);
    }

    // ---- keyboard -----------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingIndex >= 0) {
            if (keyCode == GLFW_KEY_ENTER || keyCode == GLFW_KEY_KP_ENTER) {
                commitEdit();
                return true;
            }
            for (TextFieldWidget f : editFields) {
                if (f.isFocused()) {
                    return f.keyPressed(keyCode, scanCode, modifiers);
                }
            }
            if (keyCode == GLFW_KEY_DELETE) {
                steps.remove(editingIndex);
                cancelEdit();
                return true;
            }
            if (keyCode == GLFW_KEY_ESCAPE) {
                cancelEdit();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingIndex >= 0) {
            for (TextFieldWidget f : editFields) {
                if (f.isFocused()) {
                    if (f.charTyped(chr, modifiers)) return true;
                    break;
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }

    // ---- tick ---------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (editingIndex >= 0) updateFieldPos();
    }

    // ---- save ---------------------------------------------------------------

    private void saveScript() {
        String code = ScriptCodegen.generate(scriptName, steps);
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("mat").resolve("scripts");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(scriptName + ".js"), code);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§aSaved script: " + scriptName + ".js"));
            }
        } catch (IOException e) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§cFailed to save: " + e.getMessage()));
            }
        }
    }

    // ---- editing helpers ----------------------------------------------------

    private void startEdit(int index) {
        if (index < 0 || index >= steps.size()) return;
        commitEdit();
        editingIndex = index;
        editFields.clear();
        ActionStep step = steps.get(index);
        for (ParamDef def : PARAM_DEFS.getOrDefault(step.type(), List.of())) {
            TextFieldWidget f = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal(def.label()));
            f.setText(step.param(def.key()));
            f.setMaxLength(100);
            editFields.add(f);
        }
    }

    private void commitEdit() {
        if (editingIndex < 0) return;
        ActionStep step = steps.get(editingIndex);
        List<ParamDef> defs = PARAM_DEFS.getOrDefault(step.type(), List.of());
        for (int i = 0; i < defs.size() && i < editFields.size(); i++) {
            step.setParam(defs.get(i).key(), editFields.get(i).getText());
        }
        cancelEdit();
    }

    private void cancelEdit() {
        editingIndex = -1;
        editFields.clear();
        doneY = -1;
    }

    private void updateFieldPos() {
        if (editingIndex < 0) return;
        int contentY = PANEL_TOP + HEADER_H - seqScroll;
        for (int i = 0; i < editingIndex; i++) contentY += getStepHeight(i);
        int fy = contentY + STEP_HEIGHT + 2;

        ActionStep step = steps.get(editingIndex);
        List<ParamDef> defs = PARAM_DEFS.getOrDefault(step.type(), List.of());
        int maxW = 0;
        for (ParamDef d : defs) maxW = Math.max(maxW, textRenderer.getWidth(d.label() + ": "));

        int fx = seqPanelX + 4;
        for (int i = 0; i < defs.size() && i < editFields.size(); i++) {
            TextFieldWidget f = editFields.get(i);
            f.setX(fx + maxW);
            f.setY(fy);
            f.setWidth(SEQUENCE_WIDTH - maxW - 14);
            fy += 18;
        }
        doneX = fx;
        doneY = fy + 3;
    }

    // ---- spatial helpers ----------------------------------------------------

    private int getStepHeight(int idx) {
        return STEP_HEIGHT + getEditHeight(idx);
    }

    private int getEditHeight(int idx) {
        if (idx != editingIndex) return 0;
        List<ParamDef> defs = PARAM_DEFS.getOrDefault(steps.get(idx).type(), List.of());
        if (defs.isEmpty()) return 0;
        return 2 + defs.size() * 18 + 4 + doneH + 2;
    }

    private PalEntry getPalEntryAt(double my) {
        int y = PANEL_TOP + HEADER_H - palScroll;
        for (PalEntry entry : palette) {
            if (my >= y && my < y + PALETTE_ITEM_HEIGHT) {
                return entry.header() ? null : entry;
            }
            y += PALETTE_ITEM_HEIGHT;
        }
        return null;
    }

    private int getSeqIndexAt(double my) {
        int y = PANEL_TOP + HEADER_H - seqScroll;
        for (int i = 0; i < steps.size(); i++) {
            int h = getStepHeight(i);
            if (my >= y && my < y + h) return i;
            y += h;
        }
        return steps.size();
    }

    private int getMaxPalScroll() {
        int total = palette.size() * PALETTE_ITEM_HEIGHT;
        int vis = height - PANEL_TOP - 4 - HEADER_H - 2;
        return Math.max(0, total - vis);
    }

    private int getMaxSeqScroll() {
        int total = 0;
        for (int i = 0; i < steps.size(); i++) total += getStepHeight(i);
        int vis = height - PANEL_TOP - 4 - HEADER_H - 2;
        return Math.max(0, total - vis);
    }

    private static int clampScroll(int scroll, int max) {
        if (scroll < 0) return 0;
        if (scroll > max) return max;
        return scroll;
    }
}
