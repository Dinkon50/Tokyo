package com.tokyoghoul.network;

import com.tokyoghoul.TokyoGhoulMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TokyoGhoulMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId = 0;

    private NetworkHandler() {}

    public static void register() {
        CHANNEL.messageBuilder(AbilityMessage.class, nextId++)
                .encoder(AbilityMessage::encode)
                .decoder(AbilityMessage::decode)
                .consumerMainThread(AbilityMessage::handle)
                .add();
    }
}
