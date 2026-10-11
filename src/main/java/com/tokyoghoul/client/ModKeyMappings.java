package com.tokyoghoul.client;

import com.tokyoghoul.TokyoGhoulMod;
import com.tokyoghoul.network.AbilityMessage;
import com.tokyoghoul.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TokyoGhoulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final Lazy<KeyMapping> KAGUNE = Lazy.of(() -> new KeyMapping(
            "key.tokyoghoul.kagune", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_G,
            "key.categories.tokyoghoul"));
    public static final Lazy<KeyMapping> DASH = Lazy.of(() -> new KeyMapping(
            "key.tokyoghoul.dash", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_H,
            "key.categories.tokyoghoul"));
    public static final Lazy<KeyMapping> RAGE = Lazy.of(() -> new KeyMapping(
            "key.tokyoghoul.rage", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_J,
            "key.categories.tokyoghoul"));

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KAGUNE.get());
        event.register(DASH.get());
        event.register(RAGE.get());
    }

    @Mod.EventBusSubscriber(modid = TokyoGhoulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) return;
            while (KAGUNE.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(0));
            while (DASH.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(1));
            while (RAGE.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(2));
        }
    }
}
