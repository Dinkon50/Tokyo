package com.tokyoghoul.client;

import com.tokyoghoul.TokyoGhoulMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Compact dark HUD for the first playable pass. */
@Mod.EventBusSubscriber(modid = TokyoGhoulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ModHud {
    private ModHud() {}

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = 8;
        int y = 8;
        drawBar(g, x, y, 118, 8, mc.player.getHealth() / mc.player.getMaxHealth(), 0xFF8D202B);
        g.drawString(mc.font, "HP " + (int)mc.player.getHealth() + "/" + (int)mc.player.getMaxHealth(), x + 4, y, 0xFFFFFFFF, false);
        y += 12;
        drawBar(g, x, y, 118, 8, mc.player.getFoodData().getFoodLevel() / 20.0F, 0xFFB07A35);
        g.drawString(mc.font, "Голод " + mc.player.getFoodData().getFoodLevel() + "/20", x + 4, y, 0xFFFFFFFF, false);
        y += 12;
        drawBar(g, x, y, 118, 8, mc.player.experienceProgress, 0xFF4B9C58);
        g.drawString(mc.font, "Уровень " + mc.player.experienceLevel, x + 4, y, 0xFFFFFFFF, false);
        g.drawString(mc.font, "Tokyo Ghoul RPG", x, y + 13, 0xFFE2D4D4, false);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, int height, float amount, int color) {
        g.fill(x, y, x + width, y + height, 0xAA17151A);
        int filled = Math.max(0, Math.min(width, Math.round(width * amount)));
        if (filled > 0) g.fill(x, y, x + filled, y + height, color);
        g.fill(x, y, x + width, y + 1, 0x55FFFFFF);
    }
}
