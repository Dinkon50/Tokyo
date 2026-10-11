package com.tokyoghoul;

import com.tokyoghoul.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;

@Mod(TokyoGhoulMod.MOD_ID)
public final class TokyoGhoulMod {
    public static final String MOD_ID = "tokyoghoul";

    public TokyoGhoulMod() {
        NetworkHandler.register();
    }
}
