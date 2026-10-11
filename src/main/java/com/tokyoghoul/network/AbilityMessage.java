package com.tokyoghoul.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.function.Supplier;

public record AbilityMessage(int ability) {
    public static void encode(AbilityMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.ability);
    }

    public static AbilityMessage decode(FriendlyByteBuf buffer) {
        return new AbilityMessage(buffer.readVarInt());
    }

    public static void handle(AbilityMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isSpectator()) return;
            switch (message.ability) {
                case 0 -> toggleKagune(player);
                case 1 -> dash(player);
                case 2 -> activateRage(player);
                case 3 -> bite(player);
                default -> { }
            }
        });
        context.setPacketHandled(true);
    }

    private static void toggleKagune(ServerPlayer player) {
        boolean active = !player.getPersistentData().getBoolean("tokyoghoul_kagune_active");
        player.getPersistentData().putBoolean("tokyoghoul_kagune_active", active);
        player.displayClientMessage(Component.literal(active ? "Кагуне раскрыто" : "Кагуне убрано"), true);
    }

    private static void dash(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("tokyoghoul_dash_ready") > now) return;
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.x * 1.25, Math.max(player.getDeltaMovement().y, 0.12), look.z * 1.25);
        player.hurtMarked = true;
        player.getPersistentData().putLong("tokyoghoul_dash_ready", now + 30);
    }

    private static void activateRage(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("tokyoghoul_rage_ready") > now) {
            player.displayClientMessage(Component.literal("Ярость ещё не готова"), true);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0, false, true, true));
        player.getPersistentData().putLong("tokyoghoul_rage_ready", now + 1200);
        player.displayClientMessage(Component.literal("Ярость активирована на 20 секунд"), true);
    }

    private static void bite(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("tokyoghoul_bite_ready") > now) {
            player.displayClientMessage(Component.literal("Укус ещё перезаряжается"), true);
            return;
        }
        if (player.isBlocking()) {
            player.displayClientMessage(Component.literal("Нельзя кусать во время блока"), true);
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity target = player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(3.0),
                        e -> e != player && e.isAlive() && !e.isSpectator())
                .stream()
                .filter(e -> {
                    Vec3 delta = e.getEyePosition().subtract(eye);
                    return delta.lengthSqr() <= 9.0 && delta.normalize().dot(look) > 0.72;
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        player.getPersistentData().putLong("tokyoghoul_bite_ready", now + 200);
        if (target == null) {
            player.displayClientMessage(Component.literal("Нет цели для укуса"), true);
            return;
        }
        if (target instanceof net.minecraft.world.entity.player.Player targetPlayer && targetPlayer.isBlocking()) {
            player.displayClientMessage(Component.literal("Цель блокирует — укус не прошёл"), true);
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10, false, true, true));
        player.getFoodData().setFoodLevel(20);
        player.heal(2.0F);
        if (player.getPersistentData().getBoolean("tokyoghoul_rage_active")) {
            player.displayClientMessage(Component.literal("Укус успешен"), true);
        } else {
            int rage = player.getPersistentData().getInt("tokyoghoul_rage_points");
            player.getPersistentData().putInt("tokyoghoul_rage_points", Math.min(100, rage + 5));
            player.displayClientMessage(Component.literal("Укус успешен: голод восстановлен"), true);
        }
    }
}
