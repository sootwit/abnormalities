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
    private static final int CELL_W = 200;
    private static final int CELL_H = 24;
    private static final int ROW_H = 30;
    private static final int VISIBLE_ROWS = 8;

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
                "Turns off nur block breaking, k3w undo, wind pillars, wind corruption, wind chunks",
                () -> {
                    AbnormalitiesConfig.NUR_BREAK_BLOCKS.set(false);
                    AbnormalitiesConfig.NUR_TOWER.set(false);
                    AbnormalitiesConfig.NUR_BRIDGE.set(false);
                    AbnormalitiesConfig.K3W_BREAK_BLOCKS.set(false);
                    AbnormalitiesConfig.K3W_PLACE_BLOCKS.set(false);
                    AbnormalitiesConfig.TW_PILLARS_ENABLED.set(false);
                    AbnormalitiesConfig.TW_CORRUPTION_ENABLED.set(false);
                    AbnormalitiesConfig.TW_DESTRUCTIVE_ENABLED.set(false);
                    AbnormalitiesConfig.TW_CHUNK_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Disable ALL hostile entities",
                "Turns off nur, k3w, him, skinwalkers, it",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(false);
                    AbnormalitiesConfig.K3W_ENABLED.set(false);
                    AbnormalitiesConfig.HIM_ENABLED.set(false);
                    AbnormalitiesConfig.SW_ENABLED.set(false);
                    AbnormalitiesConfig.IT_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Disable ALL events",
                "Turns off every horror event (vr9p, hush, lure, miner, etc)",
                () -> {
                    AbnormalitiesConfig.VR9P_ENABLED.set(false);
                    AbnormalitiesConfig.VR9P_STARGAZED_ENABLED.set(false);
                    AbnormalitiesConfig.HUSH_ENABLED.set(false);
                    AbnormalitiesConfig.LURE_ENABLED.set(false);
                    AbnormalitiesConfig.M1NER_ENABLED.set(false);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(false);
                    AbnormalitiesConfig.W4K3_ENABLED.set(false);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(false);
                    AbnormalitiesConfig.S1GN_ENABLED.set(false);
                    AbnormalitiesConfig.WR0NG_ENABLED.set(false);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(false);
                    AbnormalitiesConfig.H01D_ENABLED.set(false);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(false);
                    AbnormalitiesConfig.GONE_ENABLED.set(false);
                    AbnormalitiesConfig.B3D_ENABLED.set(false);
                    AbnormalitiesConfig.L3NS_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);
                    AbnormalitiesConfig.F4K3_ENABLED.set(false);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(false);
                    AbnormalitiesConfig.INSANITY_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Disable THE_WIND system",
                "Turns off all wind corruption, pillars, farlands, chunks, entity, border",
                () -> {
                    AbnormalitiesConfig.TW_ENABLED.set(false);
                    AbnormalitiesConfig.TW_CORRUPTION_ENABLED.set(false);
                    AbnormalitiesConfig.TW_DESTRUCTIVE_ENABLED.set(false);
                    AbnormalitiesConfig.TW_PILLARS_ENABLED.set(false);
                    AbnormalitiesConfig.TW_FURTHERLANDS_ENABLED.set(false);
                    AbnormalitiesConfig.TW_CHUNK_ENABLED.set(false);
                    AbnormalitiesConfig.TW_THEWIND_ENABLED.set(false);
                    AbnormalitiesConfig.TW_BORDER_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Safe mode (entities + events off)",
                "Disables all hostile entities and all events. Only xYz and Sister remain.",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(false);
                    AbnormalitiesConfig.K3W_ENABLED.set(false);
                    AbnormalitiesConfig.HIM_ENABLED.set(false);
                    AbnormalitiesConfig.SW_ENABLED.set(false);
                    AbnormalitiesConfig.IT_ENABLED.set(false);
                    AbnormalitiesConfig.VR9P_ENABLED.set(false);
                    AbnormalitiesConfig.VR9P_STARGAZED_ENABLED.set(false);
                    AbnormalitiesConfig.HUSH_ENABLED.set(false);
                    AbnormalitiesConfig.LURE_ENABLED.set(false);
                    AbnormalitiesConfig.M1NER_ENABLED.set(false);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(false);
                    AbnormalitiesConfig.W4K3_ENABLED.set(false);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(false);
                    AbnormalitiesConfig.S1GN_ENABLED.set(false);
                    AbnormalitiesConfig.WR0NG_ENABLED.set(false);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(false);
                    AbnormalitiesConfig.H01D_ENABLED.set(false);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(false);
                    AbnormalitiesConfig.GONE_ENABLED.set(false);
                    AbnormalitiesConfig.B3D_ENABLED.set(false);
                    AbnormalitiesConfig.TW_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);
                    AbnormalitiesConfig.INSANITY_ENABLED.set(false);
                    AbnormalitiesConfig.F4K3_ENABLED.set(false);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Nur only (keep nur, disable rest)",
                "Only nur spawns. Everything else off.",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(true);
                    AbnormalitiesConfig.K3W_ENABLED.set(false);
                    AbnormalitiesConfig.HIM_ENABLED.set(false);
                    AbnormalitiesConfig.SW_ENABLED.set(false);
                    AbnormalitiesConfig.IT_ENABLED.set(false);
                    AbnormalitiesConfig.XYZ_ENABLED.set(false);
                    AbnormalitiesConfig.VR9P_ENABLED.set(false);
                    AbnormalitiesConfig.VR9P_STARGAZED_ENABLED.set(false);
                    AbnormalitiesConfig.HUSH_ENABLED.set(false);
                    AbnormalitiesConfig.LURE_ENABLED.set(false);
                    AbnormalitiesConfig.M1NER_ENABLED.set(false);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(false);
                    AbnormalitiesConfig.W4K3_ENABLED.set(false);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(false);
                    AbnormalitiesConfig.S1GN_ENABLED.set(false);
                    AbnormalitiesConfig.WR0NG_ENABLED.set(false);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(false);
                    AbnormalitiesConfig.H01D_ENABLED.set(false);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(false);
                    AbnormalitiesConfig.GONE_ENABLED.set(false);
                    AbnormalitiesConfig.B3D_ENABLED.set(false);
                    AbnormalitiesConfig.TW_ENABLED.set(false);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(false);
                    AbnormalitiesConfig.INSANITY_ENABLED.set(false);
                    AbnormalitiesConfig.F4K3_ENABLED.set(false);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(false);
                    AbnormalitiesConfig.L3NS_ENABLED.set(false);
                    AbnormalitiesConfig.SPEC.save();
                }));

        presets.add(new PresetEntry("Everything ON",
                "Re-enable all entities and events (default)",
                () -> {
                    AbnormalitiesConfig.NUR_ENABLED.set(true);
                    AbnormalitiesConfig.K3W_ENABLED.set(true);
                    AbnormalitiesConfig.XYZ_ENABLED.set(true);
                    AbnormalitiesConfig.IT_ENABLED.set(true);
                    AbnormalitiesConfig.HIM_ENABLED.set(true);
                    AbnormalitiesConfig.SW_ENABLED.set(true);
                    AbnormalitiesConfig.SISTER_ENABLED.set(true);
                    AbnormalitiesConfig.VR9P_ENABLED.set(true);
                    AbnormalitiesConfig.VR9P_STARGAZED_ENABLED.set(true);
                    AbnormalitiesConfig.HUSH_ENABLED.set(true);
                    AbnormalitiesConfig.LURE_ENABLED.set(true);
                    AbnormalitiesConfig.M1NER_ENABLED.set(true);
                    AbnormalitiesConfig.V1S1T_ENABLED.set(true);
                    AbnormalitiesConfig.W4K3_ENABLED.set(true);
                    AbnormalitiesConfig.M1SL4Y_ENABLED.set(true);
                    AbnormalitiesConfig.S1GN_ENABLED.set(true);
                    AbnormalitiesConfig.WR0NG_ENABLED.set(true);
                    AbnormalitiesConfig.BR34TH_ENABLED.set(true);
                    AbnormalitiesConfig.H01D_ENABLED.set(true);
                    AbnormalitiesConfig.C1RCL_ENABLED.set(true);
                    AbnormalitiesConfig.GONE_ENABLED.set(true);
                    AbnormalitiesConfig.B3D_ENABLED.set(true);
                    AbnormalitiesConfig.TW_ENABLED.set(true);
                    AbnormalitiesConfig.B3DROCK_ENABLED.set(true);
                    AbnormalitiesConfig.F4K3_ENABLED.set(true);
                    AbnormalitiesConfig.ANG3R_ENABLED.set(true);
                    AbnormalitiesConfig.L3NS_ENABLED.set(true);
                    AbnormalitiesConfig.NUR_BREAK_BLOCKS.set(true);
                    AbnormalitiesConfig.NUR_TOWER.set(true);
                    AbnormalitiesConfig.NUR_BRIDGE.set(true);
                    AbnormalitiesConfig.K3W_BREAK_BLOCKS.set(true);
                    AbnormalitiesConfig.K3W_PLACE_BLOCKS.set(true);
                    AbnormalitiesConfig.TW_CORRUPTION_ENABLED.set(true);
                    AbnormalitiesConfig.TW_PILLARS_ENABLED.set(true);
                    AbnormalitiesConfig.TW_CHUNK_ENABLED.set(true);
                    AbnormalitiesConfig.TW_FURTHERLANDS_ENABLED.set(true);
                    AbnormalitiesConfig.TW_THEWIND_ENABLED.set(true);
                    AbnormalitiesConfig.TW_BORDER_ENABLED.set(true);
                    AbnormalitiesConfig.SPEC.save();
                }));

        addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> Minecraft.getInstance().setScreen(null)
        ).pos(this.width / 2 - 100, this.height - 30).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float partial) {
        renderBackground(gfx);
        super.render(gfx, mx, my, partial);

        int cx = width / 2;
        gfx.drawCenteredString(font, Component.literal("Abnormalities Config").withStyle(ChatFormatting.BOLD), cx, 8, 0xFFFFFF);
        gfx.drawCenteredString(font, Component.literal("Click a preset to apply it").withStyle(ChatFormatting.GRAY), cx, 20, 0xAAAAAA);

        int gridW = CELL_W;
        int startX = (width - gridW) / 2;
        int startY = 36;

        int maxScroll = Math.max(0, presets.size() - VISIBLE_ROWS);

        for (int i = scrollOffset; i < scrollOffset + VISIBLE_ROWS && i < presets.size(); i++) {
            var preset = presets.get(i);
            int y = startY + (i - scrollOffset) * ROW_H;

            boolean hover = mx >= startX && mx <= startX + gridW && my >= y && my <= y + CELL_H;
            int bgColor = hover ? 0x80444466 : 0x60222233;
            gfx.fill(startX, y, startX + gridW, y + CELL_H, bgColor);

            gfx.drawString(font, Component.literal(preset.label).withStyle(ChatFormatting.WHITE), startX + 6, y + 4, 0xFFFFFF);
            gfx.drawString(font, Component.literal(preset.description).withStyle(ChatFormatting.GRAY), startX + 6, y + 14, 0xAAAAAA);

            if (hover) {
                gfx.drawCenteredString(font, Component.literal("Click to apply").withStyle(ChatFormatting.GREEN), cx, height - 14, 0x55FF55);
            }
        }

        if (maxScroll > 0) {
            String scrollHint = "\u00a78\u25b2/\u25bc \u00a77scroll";
            gfx.drawCenteredString(font, Component.literal(scrollHint), cx, height - 10, 0x444444);
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
        int gridW = CELL_W;
        int startX = (width - gridW) / 2;
        int startY = 36;

        if (mx < startX || mx > startX + gridW) return super.mouseClicked(mx, my, btn);
        if (my < startY || my > startY + VISIBLE_ROWS * ROW_H) return super.mouseClicked(mx, my, btn);

        int row = (int) ((my - startY) / ROW_H) + scrollOffset;
        if (row >= 0 && row < presets.size()) {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 1.0f));
            presets.get(row).action.run();
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }
}
