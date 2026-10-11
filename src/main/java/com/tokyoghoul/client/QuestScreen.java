package com.tokyoghoul.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Initial quest journal screen; progress/rewards will be wired to server data next. */
public final class QuestScreen extends Screen {
    public QuestScreen() { super(Component.literal("Задания")); }

    @Override
    protected void init() {
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Закрыть"), button -> this.onClose())
                .bounds(this.width / 2 - 70, this.height / 2 + 48, 140, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int center = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, center, this.height / 2 - 62, 0xE8D7D7);
        graphics.drawCenteredString(this.font, "Охота: побеждайте противников и набирайте опыт.", center, this.height / 2 - 28, 0xD0D0D0);
        graphics.drawCenteredString(this.font, "Задания с прогрессом и наградами — следующий этап.", center, this.height / 2 - 10, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
