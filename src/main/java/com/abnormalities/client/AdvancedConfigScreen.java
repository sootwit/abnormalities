package com.abnormalities.client;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdvancedConfigScreen extends Screen {
    private static final int COLS = 1;
    private static final int CELL_W = 200;
    private static final int CELL_H = 22;
    private static final int COL_GAP = 0;
    private static final int ROW_H = 28;
    private static final int VISIBLE_ROWS = 12;

    private final List<ToggleEntry> entries = new ArrayList<>();
    private final Map<String, List<Integer>> sections = new LinkedHashMap<>();
    private int scrollOffset = 0;

    private static class ToggleEntry {
        String label;
        ForgeConfigSpec.BooleanValue config;
        String description;

        ToggleEntry(String label, ForgeConfigSpec.BooleanValue config, String description) {
            this.label = label;
            this.config = config;
            this.description = description;
        }
    }

    public AdvancedConfigScreen() {
        super(Component.literal("Advanced Config"));
    }

    @Override
    protected void init() {
        entries.clear();
        sections.clear();

        addSection("Entities");
        addToggle("Nur", AbnormalitiesConfig.NUR_ENABLED, "The billboard stalker that chases at night");
        addToggle("K3w", AbnormalitiesConfig.K3W_ENABLED, "Player clone that undoes your actions");
        addToggle("xYz", AbnormalitiesConfig.XYZ_ENABLED, "Giant supplier that demands items");
        addToggle("It", AbnormalitiesConfig.IT_ENABLED, "Flat shadow that steals when you look away");
        addToggle("Him", AbnormalitiesConfig.HIM_ENABLED, "Player-like survivor that bridges at you");
        addToggle("Skinwalkers", AbnormalitiesConfig.SW_ENABLED, "Disguised mobs that transform");
        addToggle("Sister", AbnormalitiesConfig.SISTER_ENABLED, "The warning spirit in tab list");
        addToggle("Sister Chat", AbnormalitiesConfig.SISTER_CHAT_ENABLED, "Sister responds to chat keywords");

        addSection("Events");
        addToggle("vr9p", AbnormalitiesConfig.VR9P_ENABLED, "STOP/CONTINUE overlay game");
        addToggle("vr9p Stargazed", AbnormalitiesConfig.VR9P_STARGAZED_ENABLED, "Stricter vr9p variant");
        addToggle("h1sh", AbnormalitiesConfig.HUSH_ENABLED, "Mobs freeze and stare at you");
        addToggle("1ull", AbnormalitiesConfig.LURE_ENABLED, "Music box that lures you in");
        addToggle("m1n3r", AbnormalitiesConfig.M1NER_ENABLED, "Tunnel digger underground");
        addToggle("v1s1t", AbnormalitiesConfig.V1S1T_ENABLED, "Home invasion gift giver");
        addToggle("w4k3", AbnormalitiesConfig.W4K3_ENABLED, "Sleep displacement teleport");
        addToggle("m1sl4y", AbnormalitiesConfig.M1SL4Y_ENABLED, "Inventory gaslighting");
        addToggle("s1gn", AbnormalitiesConfig.S1GN_ENABLED, "Creepy personalized signs");
        addToggle("wr0ng", AbnormalitiesConfig.WR0NG_ENABLED, "Cursed item crafting");
        addToggle("br34th", AbnormalitiesConfig.BR34TH_ENABLED, "Phantom drowning");
        addToggle("h01d", AbnormalitiesConfig.H01D_ENABLED, "Stillness punishment");
        addToggle("c1rcl", AbnormalitiesConfig.C1RCL_ENABLED, "Torch ring on wake");
        addToggle("g0n3", AbnormalitiesConfig.GONE_ENABLED, "Light thief");
        addToggle("b3d", AbnormalitiesConfig.B3D_ENABLED, "Bed memory hunt");
        addToggle("f4k3", AbnormalitiesConfig.F4K3_ENABLED, "Fake creepy chat");
        addToggle("ang3r", AbnormalitiesConfig.ANG3R_ENABLED, "Chat anger system");
        addToggle("l3ns", AbnormalitiesConfig.L3NS_ENABLED, "Screenshot figure");
        addToggle("insanity", AbnormalitiesConfig.INSANITY_ENABLED, "Insanity meter");
        addToggle("b3drock", AbnormalitiesConfig.B3DROCK_ENABLED, "Bedrock cube event");

        addSection("World");
        addToggle("Nur breaks blocks", AbnormalitiesConfig.NUR_BREAK_BLOCKS, "Nur destroys blocks in path");
        addToggle("Nur towers", AbnormalitiesConfig.NUR_TOWER, "Nur towers up with cobble");
        addToggle("Nur bridges", AbnormalitiesConfig.NUR_BRIDGE, "Nur bridges horizontally");
        addToggle("Nur liquids", AbnormalitiesConfig.NUR_LIQUID, "Nur walks on liquids");
        addToggle("K3w undo breaks", AbnormalitiesConfig.K3W_BREAK_BLOCKS, "K3w replaces blocks you broke");
        addToggle("K3w undo places", AbnormalitiesConfig.K3W_PLACE_BLOCKS, "K3w breaks blocks you placed");
        addToggle("K3w undo kills", AbnormalitiesConfig.K3W_KILL_MOBS, "K3w revives mobs you killed");
        addToggle("THE_WIND", AbnormalitiesConfig.TW_ENABLED, "Wind corruption system");
        addToggle("Wind corruption", AbnormalitiesConfig.TW_CORRUPTION_ENABLED, "Corruption blocks spread");
        addToggle("Wind pillars", AbnormalitiesConfig.TW_PILLARS_ENABLED, "Pillar terrain destruction");
        addToggle("Wind farlands", AbnormalitiesConfig.TW_FURTHERLANDS_ENABLED, "Farlands terrain generation");
        addToggle("Wind chunks", AbnormalitiesConfig.TW_CHUNK_ENABLED, "Vertical terrain removal");
        addToggle("Wind entity", AbnormalitiesConfig.TW_THEWIND_ENABLED, "Black humanoid figure");
        addToggle("Wind border", AbnormalitiesConfig.TW_BORDER_ENABLED, "Void ring around player");
    }

    private String currentSection = "";

    private void addSection(String name) {
        currentSection = name;
        sections.put(name, new ArrayList<>());
    }

    private void addToggle(String label, ForgeConfigSpec.BooleanValue config, String description) {
        sections.get(currentSection).add(entries.size());
        entries.add(new ToggleEntry(label, config, description));
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float partial) {
        renderBackground(gfx);
        super.render(gfx, mx, my, partial);

        int cx = width / 2;
        gfx.drawCenteredString(font, Component.literal("Abnormalities Advanced Config").withStyle(ChatFormatting.BOLD), cx, 8, 0xFFFFFF);

        int gridW = COLS * CELL_W + (COLS - 1) * COL_GAP;
        int startX = (width - gridW) / 2;
        int startY = 28;

        int maxScroll = Math.max(0, (entries.size() + sections.size()) - VISIBLE_ROWS);
        int visibleIdx = 0;

        for (var sectionEntry : sections.entrySet()) {
            if (visibleIdx >= scrollOffset && visibleIdx < scrollOffset + VISIBLE_ROWS) {
                int y = startY + (visibleIdx - scrollOffset) * ROW_H;
                gfx.drawString(font, Component.literal(sectionEntry.getKey()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), startX, y, 0xFFFFFF);
            }
            visibleIdx++;

            for (int idx : sectionEntry.getValue()) {
                if (visibleIdx >= scrollOffset && visibleIdx < scrollOffset + VISIBLE_ROWS) {
                    var entry = entries.get(idx);
                    int y = startY + (visibleIdx - scrollOffset) * ROW_H;

                    boolean hover = mx >= startX && mx <= startX + gridW && my >= y && my <= y + CELL_H;
                    boolean enabled = entry.config != null && entry.config.get();

                    int bgColor = hover ? 0x80333333 : 0x60111111;
                    if (enabled) bgColor = hover ? 0x80334D33 : 0x60113311;

                    gfx.fill(startX, y, startX + gridW, y + CELL_H, bgColor);

                    gfx.drawString(font, Component.literal(entry.label), startX + 4, y + 5, 0xFFFFFF);

                    String status = enabled ? "\u00a72ON" : "\u00a7cOFF";
                    int sw = font.width(status);
                    gfx.drawString(font, Component.literal(status), startX + gridW - sw - 4, y + 5, 0xFFFFFF);

                    if (hover && entry.description != null) {
                        gfx.drawCenteredString(font, Component.literal(entry.description).withStyle(ChatFormatting.GRAY), cx, height - 14, 0xAAAAAA);
                    }
                }
                visibleIdx++;
            }
        }

        if (maxScroll > 0) {
            String scrollHint = "\u00a78\u25b2/\u25bc \u00a77scroll";
            gfx.drawCenteredString(font, Component.literal(scrollHint), cx, height - 10, 0x444444);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxScroll = Math.max(0, (entries.size() + sections.size()) - VISIBLE_ROWS);
        if (maxScroll > 0) {
            scrollOffset = (int) Mth.clamp(scrollOffset - (int) Math.signum(delta), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int gridW = COLS * CELL_W + (COLS - 1) * COL_GAP;
        int startX = (width - gridW) / 2;
        int startY = 28;

        if (mx < startX || mx > startX + gridW) return super.mouseClicked(mx, my, btn);
        if (my < startY || my > startY + VISIBLE_ROWS * ROW_H) return super.mouseClicked(mx, my, btn);

        int row = (int) ((my - startY) / ROW_H) + scrollOffset;

        int visibleIdx = 0;
        for (var sectionEntry : sections.entrySet()) {
            if (visibleIdx == row) return super.mouseClicked(mx, my, btn);
            visibleIdx++;
            for (int idx : sectionEntry.getValue()) {
                if (visibleIdx == row) {
                    var entry = entries.get(idx);
                    if (entry.config != null) {
                        entry.config.set(!entry.config.get());
                        AbnormalitiesConfig.SPEC.save();
                        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 1.0f));
                    }
                    return true;
                }
                visibleIdx++;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }
}
