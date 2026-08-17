package com.abnormalities.client;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final List<ToggleEntry> toggles = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 30;

    private static class ToggleEntry {
        String label;
        ForgeConfigSpec.BooleanValue config;
        Checkbox checkbox;

        ToggleEntry(String label, ForgeConfigSpec.BooleanValue config) {
            this.label = label;
            this.config = config;
        }
    }

    public ConfigScreen() {
        super(Component.literal("Abnormalities Config"));
    }

    @Override
    protected void init() {
        toggles.clear();
        addCategory("=== ENTITIES ===");
        addToggle("Nur (The Stalker)", AbnormalitiesConfig.NUR_ENABLED);
        addToggle("K3w (The Clone)", AbnormalitiesConfig.K3W_ENABLED);
        addToggle("xYz (The Supplier)", AbnormalitiesConfig.XYZ_ENABLED);
        addToggle("It (The Condition)", AbnormalitiesConfig.IT_ENABLED);
        addToggle("Him (The Survivor)", AbnormalitiesConfig.HIM_ENABLED);
        addToggle("Skinwalkers", AbnormalitiesConfig.SW_ENABLED);
        addToggle("Sister (The Watcher)", AbnormalitiesConfig.SISTER_ENABLED);

        addCategory("=== EVENTS ===");
        addToggle("vr9p (STOP/CONTINUE)", AbnormalitiesConfig.VR9P_ENABLED);
        addToggle("vr9p Stargazed", AbnormalitiesConfig.VR9P_STARGAZED_ENABLED);
        addToggle("h1sh (Mob Freeze)", AbnormalitiesConfig.HUSH_ENABLED);
        addToggle("1ull (Music Lure)", AbnormalitiesConfig.LURE_ENABLED);
        addToggle("m1n3r (Tunnel Digger)", AbnormalitiesConfig.M1NER_ENABLED);
        addToggle("v1s1t (Home Invasion)", AbnormalitiesConfig.V1S1T_ENABLED);
        addToggle("w4k3 (Sleep Displacement)", AbnormalitiesConfig.W4K3_ENABLED);
        addToggle("m1sl4y (Inventory Gaslight)", AbnormalitiesConfig.M1SL4Y_ENABLED);
        addToggle("s1gn (Creepy Signs)", AbnormalitiesConfig.S1GN_ENABLED);
        addToggle("wr0ng (Cursed Crafts)", AbnormalitiesConfig.WR0NG_ENABLED);
        addToggle("br34th (Phantom Drown)", AbnormalitiesConfig.BR34TH_ENABLED);
        addToggle("h01d (Stillness Punish)", AbnormalitiesConfig.H01D_ENABLED);
        addToggle("c1rcl (Torch Ring)", AbnormalitiesConfig.C1RCL_ENABLED);
        addToggle("g0n3 (Light Thief)", AbnormalitiesConfig.GONE_ENABLED);
        addToggle("b3d (Bed Hunt)", AbnormalitiesConfig.B3D_ENABLED);
        addToggle("f4k3 (Fake Chat)", AbnormalitiesConfig.F4K3_ENABLED);
        addToggle("ang3r (Chat Anger)", AbnormalitiesConfig.ANG3R_ENABLED);
        addToggle("l3ns (Screenshot Figure)", AbnormalitiesConfig.L3NS_ENABLED);
        addToggle("insanity", AbnormalitiesConfig.INSANITY_ENABLED);
        addToggle("b3drock (Bedrock Cube)", AbnormalitiesConfig.B3DROCK_ENABLED);

        addCategory("=== WORLD SAFETY ===");
        addToggle("Nur breaks blocks", AbnormalitiesConfig.NUR_BREAK_BLOCKS);
        addToggle("Nur towers up", AbnormalitiesConfig.NUR_TOWER);
        addToggle("Nur bridges", AbnormalitiesConfig.NUR_BRIDGE);
        addToggle("Nur walks on liquids", AbnormalitiesConfig.NUR_LIQUID);
        addToggle("K3w undoes block breaks", AbnormalitiesConfig.K3W_BREAK_BLOCKS);
        addToggle("K3w undoes block places", AbnormalitiesConfig.K3W_PLACE_BLOCKS);
        addToggle("K3w undoes mob kills", AbnormalitiesConfig.K3W_KILL_MOBS);
        addToggle("THE_WIND enabled", AbnormalitiesConfig.TW_ENABLED);
        addToggle("Wind corruption", AbnormalitiesConfig.TW_CORRUPTION_ENABLED);
        addToggle("Wind destructive corruption", AbnormalitiesConfig.TW_DESTRUCTIVE_ENABLED);
        addToggle("Wind pillars", AbnormalitiesConfig.TW_PILLARS_ENABLED);
        addToggle("Wind farlands", AbnormalitiesConfig.TW_FURTHERLANDS_ENABLED);
        addToggle("Wind chunk removal", AbnormalitiesConfig.TW_CHUNK_ENABLED);
        addToggle("Wind entity", AbnormalitiesConfig.TW_THEWIND_ENABLED);
        addToggle("Wind border", AbnormalitiesConfig.TW_BORDER_ENABLED);

        addCategory("=== SISTER ===");
        addToggle("Sister chat responses", AbnormalitiesConfig.SISTER_CHAT_ENABLED);

        int y = HEADER_HEIGHT + 10;
        for (ToggleEntry entry : toggles) {
            if (entry.config == null) continue;
            entry.checkbox = new Checkbox(
                    this.width / 2 - 150, y, 300, LINE_HEIGHT - 4,
                    Component.literal(entry.label), entry.config.get());
            addRenderableWidget(entry.checkbox);
            y += LINE_HEIGHT;
        }

        addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> {
                    applyChanges();
                    Minecraft.getInstance().setScreen(null);
                }).pos(this.width / 2 - 100, this.height - 30).size(200, 20).build());
    }

    private void addCategory(String name) {
        toggles.add(new ToggleEntry(name, null));
    }

    private void addToggle(String label, ForgeConfigSpec.BooleanValue config) {
        toggles.add(new ToggleEntry(label, config));
    }

    private void applyChanges() {
        for (ToggleEntry entry : toggles) {
            if (entry.config == null || entry.checkbox == null) continue;
            entry.config.set(entry.checkbox.selected());
        }
        AbnormalitiesConfig.SPEC.save();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        gui.drawCenteredString(this.font, Component.literal("Abnormalities Config").withStyle(ChatFormatting.BOLD),
                this.width / 2, 8, 0xFFFFFF);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        applyChanges();
        return true;
    }
}
