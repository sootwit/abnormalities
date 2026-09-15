package com.abnormalities.client;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private static final int MARGIN = 20;
    private static final int CELL_PAD = 6;
    private static final int TITLE_H = 14;
    private static final int DESC_H = 12;
    private static final int CELL_H = TITLE_H + DESC_H + CELL_PAD;
    private static final int ROW_H = CELL_H + 4;
    private static final int VISIBLE_ROWS = 7;

    private final List<PresetEntry> presets = new ArrayList<>();
    private int scrollOffset = 0;

    private static class PresetEntry {
        String label;
        String description;
        Runnable action;

        PresetEntry(String label, String description, Runnable action) {
            this.label = label;
            this.description = description;
            this.action = action;
        }
    }

    public ConfigScreen() {
        super(Component.literal("Config"));
    }

    @Override
    protected void init() {
        presets.clear();

        presets.add(new PresetEntry("Disable ALL block destruction",
                "Turns off nur block breaking, friend undo, 0x0000 pillars, corruption, chunks, border, miner, him tower/bridge",
                () -> {
                    AbnormalitiesConfig.NUR_BREAK_BLOCKS.set(false);
                    AbnormalitiesConfig.NUR_TOWER.set(false);
                    AbnormalitiesConfig.NUR_BRIDGE.set(false);
                    AbnormalitiesConfig.NUR_LIQUID.set(false);
                    AbnormalitiesConfig.FRIEND_BREAK_BLOCKS.set(false);
                    AbnormalitiesConfig.FRIEND_PLACE_BLOCKS.set(false);
                    AbnormalitiesConfig.HN_PILLARS_ENABLED.set(false);
                    AbnormalitiesConfig.HN_CHUNK_ENABLED.set(false);
                    AbnormalitiesConfig.HN_BORDER_ENABLED.set(false);
                    AbnormalitiesConfig.HN_FURTHERLANDS_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);
                    AbnormalitiesConfig.M1NER_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Disable ALL hostile entities",
                "Turns off nur, friend, him, skinwalkers",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(false);
                    AbnormalitiesConfig.FRIEND_ENABLED.set(false);
                    AbnormalitiesConfig.HIM_ENABLED.set(false);
                    AbnormalitiesConfig.SW_ENABLED.set(false);
                    AbnormalitiesConfig.APPARITION_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Disable ALL events",
                "Turns off every horror event (segfault, hush, miner, etc)",
                () -> {
                    AbnormalitiesConfig.SEGFAULT_ENABLED.set(false);
                    AbnormalitiesConfig.SEGFAULT_STARGAZED_ENABLED.set(false);
                    AbnormalitiesConfig.HUSH_ENABLED.set(false);
                    AbnormalitiesConfig.M1NER_ENABLED.set(false);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(false);
                    AbnormalitiesConfig.W4K3_ENABLED.set(false);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(false);
                    AbnormalitiesConfig.S1GN_ENABLED.set(false);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(false);
                    AbnormalitiesConfig.H01D_ENABLED.set(false);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(false);

                    AbnormalitiesConfig.B3D_ENABLED.set(false);
                    AbnormalitiesConfig.HN_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);
                    AbnormalitiesConfig.PEAK_DAY_ENABLED.set(false);
                    AbnormalitiesConfig.CURSED_HOUSE_ENABLED.set(false);
                    AbnormalitiesConfig.CURSED_BIOME_ENABLED.set(false);
                    AbnormalitiesConfig.APPARITION_ENABLED.set(false);

                    AbnormalitiesConfig.F4K3_ENABLED.set(false);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(false);
                    AbnormalitiesConfig.HN_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);
                    AbnormalitiesConfig.PEAK_DAY_ENABLED.set(false);
                    AbnormalitiesConfig.CURSED_HOUSE_ENABLED.set(false);
                    AbnormalitiesConfig.CURSED_BIOME_ENABLED.set(false);
                    AbnormalitiesConfig.APPARITION_ENABLED.set(false);

                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Disable 0x0000 system",
                "Turns off all corruption, pillars, farlands, chunks, border",
                () -> {
                    AbnormalitiesConfig.HN_ENABLED.set(false);
                    AbnormalitiesConfig.HN_PILLARS_ENABLED.set(false);
                    AbnormalitiesConfig.HN_FURTHERLANDS_ENABLED.set(false);
                    AbnormalitiesConfig.HN_CHUNK_ENABLED.set(false);
                    AbnormalitiesConfig.HN_BORDER_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Safe mode (entities + events off)",
                "Disables all hostile entities and all events. Only The Mother remains.",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(false);
                    AbnormalitiesConfig.FRIEND_ENABLED.set(false);
                    AbnormalitiesConfig.HIM_ENABLED.set(false);
                    AbnormalitiesConfig.SW_ENABLED.set(false);
                    AbnormalitiesConfig.SEGFAULT_ENABLED.set(false);
                    AbnormalitiesConfig.SEGFAULT_STARGAZED_ENABLED.set(false);
                    AbnormalitiesConfig.HUSH_ENABLED.set(false);
                    AbnormalitiesConfig.M1NER_ENABLED.set(false);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(false);
                    AbnormalitiesConfig.W4K3_ENABLED.set(false);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(false);
                    AbnormalitiesConfig.S1GN_ENABLED.set(false);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(false);
                    AbnormalitiesConfig.H01D_ENABLED.set(false);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(false);

                    AbnormalitiesConfig.B3D_ENABLED.set(false);
                    AbnormalitiesConfig.HN_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);

                    AbnormalitiesConfig.F4K3_ENABLED.set(false);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Nur only (keep nur, disable rest)",
                "Only nur spawns. Everything else off.",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(true);
                    AbnormalitiesConfig.FRIEND_ENABLED.set(false);
                    AbnormalitiesConfig.HIM_ENABLED.set(false);
                    AbnormalitiesConfig.SW_ENABLED.set(false);
                    AbnormalitiesConfig.THE_MOTHER_ENABLED.set(false);
                    AbnormalitiesConfig.SEGFAULT_ENABLED.set(false);
                    AbnormalitiesConfig.SEGFAULT_STARGAZED_ENABLED.set(false);
                    AbnormalitiesConfig.HUSH_ENABLED.set(false);
                    AbnormalitiesConfig.M1NER_ENABLED.set(false);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(false);
                    AbnormalitiesConfig.W4K3_ENABLED.set(false);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(false);
                    AbnormalitiesConfig.S1GN_ENABLED.set(false);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(false);
                    AbnormalitiesConfig.H01D_ENABLED.set(false);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(false);

                    AbnormalitiesConfig.B3D_ENABLED.set(false);
                    AbnormalitiesConfig.HN_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);

                    AbnormalitiesConfig.F4K3_ENABLED.set(false);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(false);
                    AbnormalitiesConfig.L3NS_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Everything ON",
                "Re-enable all entities and events (default)",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(true);
                    AbnormalitiesConfig.FRIEND_ENABLED.set(true);
                    AbnormalitiesConfig.THE_MOTHER_ENABLED.set(true);
                    AbnormalitiesConfig.HIM_ENABLED.set(true);
                    AbnormalitiesConfig.SW_ENABLED.set(true);
                    AbnormalitiesConfig.SEGFAULT_ENABLED.set(true);
                    AbnormalitiesConfig.SEGFAULT_STARGAZED_ENABLED.set(true);
                    AbnormalitiesConfig.HUSH_ENABLED.set(true);
                    AbnormalitiesConfig.M1NER_ENABLED.set(true);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(true);
                    AbnormalitiesConfig.W4K3_ENABLED.set(true);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(true);
                    AbnormalitiesConfig.S1GN_ENABLED.set(true);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(true);
                    AbnormalitiesConfig.H01D_ENABLED.set(true);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(true);
                    AbnormalitiesConfig.B3D_ENABLED.set(true);
                    AbnormalitiesConfig.HN_ENABLED.set(true);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(true);
                    AbnormalitiesConfig.F4K3_ENABLED.set(true);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(true);
                    AbnormalitiesConfig.L3NS_ENABLED.set(true);
                    AbnormalitiesConfig.NUR_BREAK_BLOCKS.set(true);
                    AbnormalitiesConfig.NUR_TOWER.set(true);
                    AbnormalitiesConfig.NUR_BRIDGE.set(true);
                    AbnormalitiesConfig.NUR_LIQUID.set(true);
                    AbnormalitiesConfig.FRIEND_BREAK_BLOCKS.set(true);
                    AbnormalitiesConfig.FRIEND_PLACE_BLOCKS.set(true);
                    AbnormalitiesConfig.HN_PILLARS_ENABLED.set(true);
                    AbnormalitiesConfig.HN_CHUNK_ENABLED.set(true);
                    AbnormalitiesConfig.HN_FURTHERLANDS_ENABLED.set(true);
                    AbnormalitiesConfig.HN_BORDER_ENABLED.set(true);
                    AbnormalitiesConfig.PEAK_DAY_ENABLED.set(true);
                    AbnormalitiesConfig.CURSED_HOUSE_ENABLED.set(true);
                    AbnormalitiesConfig.CURSED_BIOME_ENABLED.set(true);
                    AbnormalitiesConfig.APPARITION_ENABLED.set(true);
                    AbnormalitiesConfig.SPEC.save();
                }));

        int btnY = Math.min(36 + VISIBLE_ROWS * (CELL_H + ROW_H) + 10, this.height - 35);
        addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> Minecraft.getInstance().setScreen(null)
        ).pos(this.width / 2 - 100, btnY).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float partial) {
        renderBackground(gfx);
        super.render(gfx, mx, my, partial);

        int cx = width / 2;
        gfx.drawCenteredString(font, Component.literal("Abnormalities Config").withStyle(ChatFormatting.BOLD), cx, 8, 0xFFFFFF);
        gfx.drawCenteredString(font, Component.literal("Click a preset to apply it").withStyle(ChatFormatting.GRAY), cx, 20, 0xAAAAAA);

        int gridW = width - MARGIN * 2;
        int startX = MARGIN;
        int startY = 36;

        int maxScroll = Math.max(0, presets.size() - VISIBLE_ROWS);

        for (int i = scrollOffset; i < scrollOffset + VISIBLE_ROWS && i < presets.size(); i++) {
            var preset = presets.get(i);
            int y = startY + (i - scrollOffset) * (CELL_H + ROW_H);

            boolean hover = mx >= startX && mx <= startX + gridW && my >= y && my <= y + CELL_H;

            int bgColor = hover ? 0x80444466 : 0x60222233;
            gfx.fill(startX, y, startX + gridW, y + CELL_H, bgColor);

            gfx.drawString(font, Component.literal(preset.label).withStyle(ChatFormatting.WHITE), startX + 8, y + 3, hover ? 0xFFFF55 : 0xFFFFFF);

            gfx.drawString(font, Component.literal(preset.description).withStyle(ChatFormatting.DARK_GRAY), startX + 8, y + TITLE_H + 2, 0x888888);
        }

        if (maxScroll > 0) {
            String scrollHint = "\u00a78\u25b2/\u25bc \u00a77scroll";
            gfx.drawCenteredString(font, Component.literal(scrollHint), cx, this.height - 12, 0x444444);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxScroll = Math.max(0, presets.size() - VISIBLE_ROWS);
        if (maxScroll > 0) {
            scrollOffset = (int) Mth.clamp(scrollOffset - (int) Math.signum(delta), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int gridW = width - MARGIN * 2;
        int startX = MARGIN;
        int startY = 36;

        if (mx < startX || mx > startX + gridW) return super.mouseClicked(mx, my, btn);
        if (my < startY || my > startY + VISIBLE_ROWS * (CELL_H + ROW_H)) return super.mouseClicked(mx, my, btn);

        int row = (int) ((my - startY) / (CELL_H + ROW_H)) + scrollOffset;
        if (row >= 0 && row < presets.size()) {
            var preset = presets.get(row);
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 1.0f));
            preset.action.run();
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("Applied preset '" + preset.label + "'").withStyle(ChatFormatting.GREEN), false);
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }
}
