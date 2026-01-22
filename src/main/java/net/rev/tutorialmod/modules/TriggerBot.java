package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.MaceItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.rev.tutorialmod.TutorialMod;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class TriggerBot {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    private long lastAttackTime = 0;
    private Entity lastTarget = null;
    private int currentReactionDelay = -1;
    private int reactionTicks = 0;
    private boolean reactionGatePassed = false;

    private static final float MIN_ATTACK_CHARGE = 0.88f;
    private static final float MAX_ATTACK_CHARGE = 0.98f;

    public void onTick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.triggerBotEnabled) {
            reset();
            return;
        }

        if (!TutorialMod.CONFIG.triggerBotToggledOn) {
            reset();
            return;
        }

        // Check hotkey
        if (!TutorialMod.CONFIG.triggerBotHotkey.equals("key.keyboard.unknown")) {
            int keyCode = getKeyCode(TutorialMod.CONFIG.triggerBotHotkey);
            if (keyCode != -1 && !InputUtil.isKeyPressed(mc.getWindow(), keyCode)) {
                reset();
                return;
            }
        }

        if (mc.currentScreen != null && !TutorialMod.CONFIG.triggerBotActiveInInventory) {
            reset();
            return;
        }

        HitResult hitResult = mc.crosshairTarget;
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            if (shouldAttack(entity)) {
                if (entity != lastTarget) {
                    lastTarget = entity;
                    reactionTicks = 0;
                    reactionGatePassed = false;
                    currentReactionDelay = getRandomDelay(TutorialMod.CONFIG.triggerBotReactionMinDelay, TutorialMod.CONFIG.triggerBotReactionMaxDelay);
                }

                if (!reactionGatePassed) {
                    reactionTicks++;
                    if (reactionTicks >= currentReactionDelay) {
                        reactionGatePassed = true;
                    }
                }

                if (reactionGatePassed) {
                    if (canAttack()) {
                        attack(entity);
                    }
                }
            } else {
                reset();
            }
        } else {
            reset();
        }
    }

    private void reset() {
        lastTarget = null;
        reactionTicks = 0;
        reactionGatePassed = false;
        currentReactionDelay = -1;
    }

    private int getRandomDelay(int min, int max) {
        if (min >= max) return min;
        return min + random.nextInt(max - min + 1);
    }

    private boolean shouldAttack(Entity entity) {
        if (!(entity instanceof LivingEntity) && !(entity instanceof EndCrystalEntity)) return false;
        if (entity instanceof LivingEntity living && !living.isAlive()) return false;
        if (entity == mc.player) return false;

        if (entity instanceof PlayerEntity player) {
            if (!TutorialMod.CONFIG.triggerBotIncludePlayers) return false;
            if (TutorialMod.CONFIG.triggerBotExcludeTeammates && TutorialMod.CONFIG.teammates.contains(player.getName().getString())) return false;
        } else if (entity instanceof VillagerEntity) {
            if (TutorialMod.CONFIG.triggerBotExcludeVillagers) return false;
            if (!TutorialMod.CONFIG.triggerBotIncludePassives) return false;
        } else if (entity instanceof EndCrystalEntity) {
            if (!TutorialMod.CONFIG.triggerBotIncludeCrystals) return false;
        } else if (entity instanceof LivingEntity) {
            // Check if hostile or passive
            boolean isHostile = isHostile(entity);
            if (isHostile && !TutorialMod.CONFIG.triggerBotIncludeHostiles) return false;
            if (!isHostile && !TutorialMod.CONFIG.triggerBotIncludePassives) return false;
        }

        return true;
    }

    private boolean isHostile(Entity entity) {
        String name = entity.getType().getTranslationKey();
        return name.contains("zombie") || name.contains("skeleton") || name.contains("spider") ||
               name.contains("creeper") || name.contains("slime") || name.contains("enderman") ||
               name.contains("blaze") || name.contains("ghast") || name.contains("witch") ||
               name.contains("guardian") || name.contains("hoglin") || name.contains("piglin") ||
               name.contains("pillager") || name.contains("ravager") || name.contains("vex") ||
               name.contains("vindicator") || name.contains("warden") || name.contains("wither");
    }

    private boolean canAttack() {
        if (mc.player == null) return false;

        // Weapon check
        if (TutorialMod.CONFIG.triggerBotWeaponOnly) {
            ItemStack stack = mc.player.getMainHandStack();
            if (!stack.isIn(ItemTags.SWORDS) && !stack.isIn(ItemTags.AXES) && !(stack.getItem() instanceof MaceItem)) {
                return false;
            }
        }

        // Crit check
        if (TutorialMod.CONFIG.attackOnCrit && !mc.player.isOnGround()) {
            if (!isCrit()) return false;
        }

        float cooldown = mc.player.getAttackCooldownProgress(0.0f);

        if (cooldown < MIN_ATTACK_CHARGE) {
            return false; // too early -> prevents spam
        }

        if (cooldown > MAX_ATTACK_CHARGE) {
            return true; // fully charged, always ok
        }

        // Between min & max -> add randomness
        return random.nextFloat() < 0.4f;
    }

    private boolean isCrit() {
        if (mc.player == null) return false;
        // Vanilla crit requirements:
        // 1. Not on ground
        // 2. Falling (velocity.y < 0 and fallDistance > 0)
        // 3. Not climbing (ladder/vines)
        // 4. Not in water
        // 5. Not blind
        // 6. Not riding
        return !mc.player.isOnGround() &&
               mc.player.getVelocity().y < -0.01 &&
               mc.player.fallDistance > 0.0f &&
               !mc.player.isClimbing() &&
               !mc.player.isTouchingWater() &&
               !mc.player.hasVehicle() &&
               !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
    }

    private void attack(Entity entity) {
        if (mc.interactionManager == null || mc.player == null) return;

        if (mc.player.getAttackCooldownProgress(0.0f) < 0.85f) return;

        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();
    }

    private int getKeyCode(String translationKey) {
        if (translationKey == null || translationKey.isEmpty() || translationKey.equals("key.keyboard.unknown")) return -1;
        try {
            if (translationKey.startsWith("key.keyboard.")) {
                String keyName = translationKey.substring("key.keyboard.".length()).toUpperCase();
                return GLFW.class.getField("GLFW_KEY_" + keyName).getInt(null);
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }
}
