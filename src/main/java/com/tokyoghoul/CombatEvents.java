package com.tokyoghoul;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * First-pass defensive handling for the hold-to-block ability.
 * Special-attack break rules will be layered on top as combat abilities are implemented.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CombatEvents {
    private CombatEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.getPersistentData().getBoolean("tokyoghoul_blocking")) return;

        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker.getPersistentData().getBoolean("tokyoghoul_rage_active")) {
            long now = player.level().getGameTime();
            if (player.getPersistentData().getLong("tokyoghoul_block_break_ready") <= now) {
                player.getPersistentData().putBoolean("tokyoghoul_blocking", false);
                player.getPersistentData().putLong("tokyoghoul_block_break_ready", now + 200);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, true, true));
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Блок пробит!"), true);
            } else {
                event.setCanceled(true);
            }
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (event.player instanceof ServerPlayer player) {
            if (player.getPersistentData().getBoolean("tokyoghoul_blocking")) {
                player.setSprinting(false);
            }

            if (player.getPersistentData().getBoolean("tokyoghoul_barrage_active")) {
                long now = player.level().getGameTime();
                long start = player.getPersistentData().getLong("tokyoghoul_barrage_end") - 90;
                long elapsed = now - start;
                int hits = player.getPersistentData().getInt("tokyoghoul_barrage_hits");
                int expectedHits = (int)Math.min(32, (elapsed * 32L) / 90L + 1L);
                if (expectedHits > hits) {
                    try {
                        java.util.UUID targetId = player.getPersistentData().getUUID("tokyoghoul_barrage_target");
                        net.minecraft.world.entity.Entity entity = player.level().getEntity(
                                player.level().getEntities().get(0).getId());
                        entity = player.level().getPlayerByUUID(targetId);
                        if (entity == null) {
                            entity = player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                                    player.getBoundingBox().inflate(4.0), e -> e.getUUID().equals(targetId))
                                    .stream().findFirst().orElse(null);
                        }
                        if (entity instanceof net.minecraft.world.entity.LivingEntity target && target.isAlive()) {
                            if (target instanceof net.minecraft.world.entity.player.Player targetPlayer && targetPlayer.isBlocking()) {
                                player.getPersistentData().putBoolean("tokyoghoul_barrage_active", false);
                            } else {
                                target.hurt(player.damageSources().playerAttack(player), 1.0F);
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, true, true));
                                player.getPersistentData().putInt("tokyoghoul_barrage_hits", hits + 1);
                            }
                        } else {
                            player.getPersistentData().putBoolean("tokyoghoul_barrage_active", false);
                        }
                    } catch (RuntimeException ignored) {
                        player.getPersistentData().putBoolean("tokyoghoul_barrage_active", false);
                    }
                }
                if (now >= player.getPersistentData().getLong("tokyoghoul_barrage_end")
                        || player.getPersistentData().getInt("tokyoghoul_barrage_hits") >= 32) {
                    player.getPersistentData().putBoolean("tokyoghoul_barrage_active", false);
                    player.getPersistentData().putInt("tokyoghoul_barrage_hits", 0);
                }
            }
                        if (player.getPersistentData().getBoolean("tokyoghoul_rage_active")
                    && player.level().getGameTime() >= player.getPersistentData().getLong("tokyoghoul_rage_end")) {
                player.getPersistentData().putBoolean("tokyoghoul_rage_active", false);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Ярость закончилась"), true);
            }
        }
    }
}
