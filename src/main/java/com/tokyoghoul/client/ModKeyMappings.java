package com.tokyoghoul.client;

import com.tokyoghoul.TokyoGhoulMod;
import com.tokyoghoul.network.AbilityMessage;
import com.tokyoghoul.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    public static final Lazy<KeyMapping> BLOCK = key("key.tokyoghoul.block", GLFW.GLFW_KEY_L);
    public static final Lazy<KeyMapping> BARRAGE = key("key.tokyoghoul.barrage", GLFW.GLFW_KEY_K);
    public static final Lazy<KeyMapping> SKILL_TREE = key("key.tokyoghoul.skill_tree", GLFW.GLFW_KEY_B);
    public static final Lazy<KeyMapping> QUESTS = key("key.tokyoghoul.quests", GLFW.GLFW_KEY_N);
    // One shared, rebindable modifier for both mouse-combo abilities.
    public static final Lazy<KeyMapping> COMBO_MODIFIER = key("key.tokyoghoul.combo_modifier", GLFW.GLFW_KEY_LEFT_SHIFT);

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
        event.register(BLOCK.get());
        event.register(BARRAGE.get());
        event.register(SKILL_TREE.get());
        event.register(QUESTS.get());
        event.register(COMBO_MODIFIER.get());
    }

    @Mod.EventBusSubscriber(modid = TokyoGhoulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientEvents {
        private static boolean wasBlocking = false;
        private ClientEvents() {}

        @SubscribeEvent
        public static void onMouseButton(InputEvent.MouseButton.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null || event.getAction() != GLFW.GLFW_PRESS) return;
            if (!COMBO_MODIFIER.get().isDown()) return;
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(9));
                event.setCanceled(true);
            } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(10));
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (Minecraft.getInstance().player == null) { wasBlocking = false; return; }
            while (KAGUNE.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(0));
            while (DASH.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(1));
            while (RAGE.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(2));
            while (BITE.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(3));
            while (BARRAGE.get().consumeClick()) NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(6));
            while (SKILL_TREE.get().consumeClick()) Minecraft.getInstance().setScreen(new SkillTreeScreen());
            while (QUESTS.get().consumeClick()) Minecraft.getInstance().setScreen(new QuestScreen());
            boolean blocking = BLOCK.get().isDown();
            if (blocking != wasBlocking) {
                NetworkHandler.CHANNEL.sendToServer(new AbilityMessage(blocking ? 4 : 5));
                wasBlocking = blocking;
            }
        }
    }
}
