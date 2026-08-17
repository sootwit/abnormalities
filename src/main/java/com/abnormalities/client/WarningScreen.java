package com.abnormalities.client;

import com.abnormalities.AbnormalitiesMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class WarningScreen extends Screen {
    private Checkbox dontShowAgain;
    private final Component[] lines = new Component[]{
            Component.literal("Abnormalities"),
            Component.literal(""),
            Component.literal("This mod adds horror events that can:"),
            Component.literal("  - Destroy blocks in your world"),
            Component.literal("  - Spawn hostile entities"),
            Component.literal("  - Affect your inventory"),
            Component.literal("  - Modify your chat"),
            Component.literal(""),
            Component.literal("Some events are destructive by design."),
            Component.literal("If you don't want this, use the config command:"),
            Component.literal(""),
            Component.literal("/abnormalities config"),
            Component.literal(""),
            Component.literal("You can disable individual events there.")
    };

    public WarningScreen() {
        super(Component.literal("Abnormalities Warning"));
    }

    @Override
    protected void init() {
        dontShowAgain = new Checkbox(
                this.width / 2 - 100, this.height / 2 + 80, 200, 20,
                Component.literal("Don't show me again"), false);
        addRenderableWidget(dontShowAgain);
        addRenderableWidget(Button.builder(
                Component.literal("I understand"),
                button -> {
                    if (dontShowAgain.selected()) {
                        AbnormalitiesMod.CHANNEL.sendToServer(
                                new com.abnormalities.network.WarningAckPacket(true));
                    }
                    Minecraft.getInstance().setScreen(null);
                }).pos(this.width / 2 - 100, this.height / 2 + 110).size(200, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int y = this.height / 2 - 100;
        for (Component line : lines) {
            guiGraphics.drawCenteredString(this.font, line, this.width / 2, y, 0xFFFFFF);
            y += 12;
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
