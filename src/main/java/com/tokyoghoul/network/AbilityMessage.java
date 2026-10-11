package com.tokyoghoul.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;
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
            long now = player.level().getGameTime();
            // A barrage is a committed animation: don't allow its attacker to cancel it
            // into another ability. Always permit the release packet for hold-to-block.
            if (message.ability != 5
                    && player.getPersistentData().getBoolean("tokyoghoul_barrage_active")) {
                player.displayClientMessage(Component.literal("Во время шквала нельзя использовать другие способности"), true);
                return;
            }
            // Targets locked by a barrage cannot activate their own abilities.
            if (message.ability != 5
                    && player.getPersistentData().getLong("tokyoghoul_barrage_locked_until") > now) {
                player.displayClientMessage(Component.literal("Вы оглушены шквалом"), true);
                return;
            }
            switch (message.ability) {
                case 0 -> toggleKagune(player);
                case 1 -> dash(player);
                case 2 -> activateRage(player);
                case 3 -> bite(player);
                case 4 -> player.getPersistentData().putBoolean("tokyoghoul_blocking", true);
                case 5 -> player.getPersistentData().putBoolean("tokyoghoul_blocking", false);
                case 6 -> barrage(player);
                case 7 -> player.displayClientMessage(Component.literal("Дерево навыков: интерфейс ещё разрабатывается"), true);
                case 8 -> player.displayClientMessage(Component.literal("Задания: система ещё разрабатывается"), true);
                case 9 -> grapple(player);
                case 10 -> grabPull(player);
                case 11 -> upgradeRage(player);
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
        if (player.getPersistentData().getBoolean("tokyoghoul_rage_active")) {
            player.displayClientMessage(Component.literal("Ярость уже активна"), true);
            return;
        }
        if (player.getPersistentData().getLong("tokyoghoul_rage_ready") > now) {
            player.displayClientMessage(Component.literal("Ярость ещё не готова"), true);
            return;
        }
        int rage = player.getPersistentData().getInt("tokyoghoul_rage_points");
        if (rage < 100) {
            player.displayClientMessage(Component.literal("Для ярости нужно заполнить шкалу до 100"), true);
            return;
        }
        player.getPersistentData().putInt("tokyoghoul_rage_points", 0);
        player.getPersistentData().putBoolean("tokyoghoul_rage_active", true);
        int rank = player.getPersistentData().getInt("tokyoghoul_rage_rank");
        int durationTicks = 400 + rank * 100;
        player.getPersistentData().putLong("tokyoghoul_rage_end", now + durationTicks);
        player.getPersistentData().putLong("tokyoghoul_rage_ready", now + 1200);
        // Apply the requested base rage bonuses directly instead of potion effects
        // whose default percentages would overshoot the design values.
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            UUID modifierId = UUID.fromString("a5d3c0b1-5f82-4e1c-9a3d-7f5b2c6e8d10");
            speed.removeModifier(modifierId);
            speed.addTransientModifier(new AttributeModifier(
                    modifierId, "Tokyo Ghoul RPG rage speed", 0.05D + rank * 0.01875D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        player.displayClientMessage(Component.literal("Ярость активирована на " + (durationTicks / 20) + " секунд"), true);
    }




    private static void upgradeRage(ServerPlayer player) {
        int rank = player.getPersistentData().getInt("tokyoghoul_rage_rank");
        int points = player.getPersistentData().getInt("tokyoghoul_skill_points");
        if (rank >= 8) {
            player.displayClientMessage(Component.literal("Путь ярости уже полностью улучшен"), true);
            return;
        }
        if (points < 1) {
            player.displayClientMessage(Component.literal("Нужно очко навыка. Очко даётся за каждые 5 убийств."), true);
            return;
        }
        player.getPersistentData().putInt("tokyoghoul_skill_points", points - 1);
        player.getPersistentData().putInt("tokyoghoul_rage_rank", rank + 1);
        player.displayClientMessage(Component.literal("Путь ярости улучшен: ранг " + (rank + 1) + "/8. Осталось очков: " + (points - 1)), true);
    }

    private static LivingEntity targetInSight(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && !e.isSpectator())
            .stream()
            .filter(e -> {
                Vec3 delta = e.getBoundingBox().getCenter().subtract(eye);
                return delta.lengthSqr() <= range * range && delta.normalize().dot(look) > 0.80;
            })
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    private static void grapple(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("tokyoghoul_grapple_ready") > now) return;
        LivingEntity target = targetInSight(player, 10.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("Нет цели для крюка в пределах 10 блоков"), true);
            return;
        }
        Vec3 pull = target.getBoundingBox().getCenter().subtract(player.position()).normalize();
        player.setDeltaMovement(pull.scale(1.35).add(0, 0.18, 0));
        player.hurtMarked = true;
        player.getPersistentData().putLong("tokyoghoul_grapple_ready", now + 100);
    }

    private static void grabPull(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("tokyoghoul_grab_ready") > now) return;
        LivingEntity target = targetInSight(player, 10.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("Нет цели для притягивания в пределах 10 блоков"), true);
            return;
        }
        Vec3 pull = player.position().subtract(target.position()).normalize();
        target.setDeltaMovement(pull.scale(1.25).add(0, 0.15, 0));
        target.hurtMarked = true;
        player.getPersistentData().putLong("tokyoghoul_grab_ready", now + 200);
    }

    private static void barrage(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong("tokyoghoul_barrage_ready") > now) {
            player.displayClientMessage(Component.literal("Шквал ещё перезаряжается"), true);
            return;
        }
        if (player.getPersistentData().getBoolean("tokyoghoul_blocking")
                || player.getPersistentData().getBoolean("tokyoghoul_barrage_active")) {
            player.displayClientMessage(Component.literal("Сейчас нельзя начать шквал"), true);
            return;
        }
        LivingEntity target = targetInSight(player, 3.0);
        if (target == null) {
            player.displayClientMessage(Component.literal("Нет цели перед вами для шквала"), true);
            return;
        }
        if (target instanceof net.minecraft.world.entity.player.Player targetPlayer && targetPlayer.isBlocking()) {
            player.displayClientMessage(Component.literal("Цель блокирует — шквал не начался"), true);
            return;
        }
        player.getPersistentData().putBoolean("tokyoghoul_barrage_active", true);
        player.getPersistentData().putUUID("tokyoghoul_barrage_target", target.getUUID());
        player.getPersistentData().putLong("tokyoghoul_barrage_end", now + 90);
        target.getPersistentData().putLong("tokyoghoul_barrage_locked_until", now + 90);
        player.getPersistentData().putInt("tokyoghoul_barrage_hits", 0);
        player.getPersistentData().putLong("tokyoghoul_barrage_next_hit", now);
        player.getPersistentData().putLong("tokyoghoul_barrage_ready", now + 140);
        player.displayClientMessage(Component.literal("Шквал начат"), true);
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
