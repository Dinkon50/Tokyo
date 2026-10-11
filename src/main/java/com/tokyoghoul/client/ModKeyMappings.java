package com.tokyoghoul.client;

import com.tokyoghoul.TokyoGhoulMod;
import com.tokyoghoul.network.AbilityMessage;
import com.tokyoghoul.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TokyoGhoulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final Lazy<KeyMapping> KAGUNE = key("key.tokyoghoul.kagune", GLFW.GLFW_KEY_G);
    public static final Lazy<KeyMapping> DASH = key("key.tokyoghoul.dash", GLFW.GLFW_KEY_H);
    public static final Lazy<KeyMapping> RAGE = key("key.tokyoghoul.rage", GLFW.GLFW_KEY_J);
    public static final Lazy<KeyMapping> BITE = key("key.tokyoghoul.bite", GLFW.GLFW_KEY_V);

    private static Lazy<KeyMapping> key(String name, int key) {
        return Lazy.of(() -> new KeyMapping(name, KeyConflictContext.IN_GAME, key, "key.categories.tokyoghoul"));
    }

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KAGUNE.get());
        event.register(DASH.get());
        event.register(RAGE.get());
        event.register(BITE.get());
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
            while (BITE.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(3));
        }
    }
}
