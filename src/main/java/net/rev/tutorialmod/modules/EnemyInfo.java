package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;

import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EnemyInfo {

    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 5; // 20 ticks per second / 4 updates per second = 5 ticks
    private String lastEnemyInfo = null;

    public void onTick(MinecraftClient client) {
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updateEnemyInfo(client);
        }
    }

    private void updateEnemyInfo(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            lastEnemyInfo = null;
            return;
        }

        double maxDistance = TutorialMod.CONFIG.doubleEnemyInfoRange ? 100.0 : 50.0;
        PlayerEntity target = getPlayerLookingAt(client, maxDistance);
        if (target != null) {
            lastEnemyInfo = formatEnemyInfo(target);
        } else {
            lastEnemyInfo = null;
        }
    }

    public String getFormattedEnemyInfo() {
        return lastEnemyInfo;
    }

    private String formatEnemyInfo(PlayerEntity player) {
        StringBuilder sb = new StringBuilder();

        // Line 1: Username and Team Status
        String username = player.getName().getString();
        boolean isTeammate = TutorialMod.CONFIG.teamManager.isTeammate(username);
        sb.append(username).append(isTeammate ? " T" : " E").append("\\n");

        // Line 2: Health and Armor Durability
        if (TutorialMod.CONFIG.showHpDecimals) {
            sb.append(String.format("%.1f", player.getHealth())).append("HP ");
        } else {
            sb.append((int) Math.ceil(player.getHealth())).append("HP ");
        }
        sb.append(getArmorDurability(player));

        // Line 3: Potion Effects
        String effects = player.getStatusEffects().stream()
                .map(effect -> {
                    Text effectName = effect.getEffectType().value().getName();
                    int amplifier = effect.getAmplifier();
                    return effectName.getString() + (amplifier > 0 ? " " + (amplifier + 1) : "");
                })
                .collect(Collectors.joining(", "));
        if (!effects.isEmpty()) {
            sb.append("\\n").append(effects);
        }

        return sb.toString();
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private String getArmorDurability(PlayerEntity player) {
        float lowestDurability = 1.0f;
        EquipmentSlot lowestSlot = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armorPiece = player.getEquippedStack(slot);
            if (!armorPiece.isEmpty()) {
                float durability = (float) (armorPiece.getMaxDamage() - armorPiece.getDamage()) / armorPiece.getMaxDamage();
                if (durability < lowestDurability) {
                    lowestDurability = durability;
                    lowestSlot = slot;
                }
            }
        }

        String durabilityString;
        if (lowestDurability >= 0.75f) {
            durabilityString = "High";
        } else if (lowestDurability >= 0.40f) {
            durabilityString = "Medium";
        } else if (lowestDurability >= 0.15f) {
            durabilityString = "Low";
        } else {
            durabilityString = "Really Low";
        }

        if (TutorialMod.CONFIG.showLowestArmorPiece && lowestSlot != null) {
            char initial = ' ';
            switch (lowestSlot) {
                case HEAD: initial = 'H'; break;
                case CHEST: initial = 'C'; break;
                case LEGS: initial = 'L'; break;
                case FEET: initial = 'B'; break;
            }
            return durabilityString + " " + initial;
        }

        return durabilityString;
    }

    private PlayerEntity getPlayerLookingAt(MinecraftClient client, double maxDistance) {
        Entity camera = client.getCameraEntity();
        if (camera == null || client.world == null) {
            return null;
        }

        Vec3d cameraPos = camera.getEyePos();
        Vec3d lookVec = camera.getRotationVector();
        Vec3d endVec = cameraPos.add(lookVec.multiply(maxDistance));
        Box searchBox = camera.getBoundingBox().stretch(lookVec.multiply(maxDistance)).expand(1.0, 1.0, 1.0);

        Predicate<Entity> filter = e -> !e.isSpectator() && e.isAlive() && e instanceof PlayerEntity && e != client.player;

        EntityHitResult hitResult = ProjectileUtil.raycast(
                camera,
                cameraPos,
                endVec,
                searchBox,
                filter,
                maxDistance * maxDistance
        );

        if (hitResult != null && hitResult.getEntity() instanceof PlayerEntity) {
            return (PlayerEntity) hitResult.getEntity();
        }

        return null;
    }
}
