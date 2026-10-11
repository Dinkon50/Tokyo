package com.tokyoghoul.client;

import com.tokyoghoul.network.AbilityMessage;
import com.tokyoghoul.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact Russian-language first version of the skill tree. */
public final class SkillTreeScreen extends Screen {
    public SkillTreeScreen() { super(Component.literal("Дерево навыков")); }

    @Override
    protected void init() {
        int left = this.width / 2 - 110;
        int top = this.height / 2 - 70;
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Путь ярости: улучшить (1 очко)"),
                button -> NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(11)))
                .bounds(left, top, 220, 22).build());
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Закрыть"), button -> this.onClose())
                .bounds(left, top + 54, 220, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 94, 0xE8D7D7);
        graphics.drawCenteredString(this.font, "Улучшение увеличивает длительность и силу ярости.", this.width / 2, this.height / 2 - 35, 0xC7C7C7);
        graphics.drawCenteredString(this.font, "Очки навыков выдаются за каждые 5 убийств.", this.width / 2, this.height / 2 - 20, 0xC7C7C7);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
