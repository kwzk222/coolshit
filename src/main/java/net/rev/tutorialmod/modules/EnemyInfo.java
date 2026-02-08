package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;

import java.util.Optional;
import java.util.function.Predicate;

public class EnemyInfo {

    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 5; // 20 ticks per second / 4 updates per second = 5 ticks
    private String lastEnemyInfo = null;
    private int lingerTicks = 0;

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
            lingerTicks = 0;
            return;
        }

        double maxDistance = TutorialMod.CONFIG.doubleEnemyInfoRange ? 100.0 : 50.0;
        PlayerEntity target = getPlayerLookingAt(client, maxDistance);
        if (target != null) {
            lastEnemyInfo = formatEnemyInfo(target);
            lingerTicks = 10; // Linger for 10 ticks (0.5s) after target is lost
        } else {
            if (lingerTicks > 0) {
                lingerTicks--;
            } else {
                lastEnemyInfo = null;
            }
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

        // Line 3: Blast Protection Count
        if (TutorialMod.CONFIG.showBlastProtectionCount) {
            int bpCount = getBlastProtectionCount(player);
            if (bpCount > 0) {
                sb.append("\\n").append(bpCount).append(" BP");
            }
        }

        return sb.toString();
    }

    private int getBlastProtectionCount(PlayerEntity player) {
        int count = 0;
        var world = MinecraftClient.getInstance().world;
        if (world == null) return 0;
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> bpEntry = registry.getEntry(Enchantments.BLAST_PROTECTION.getValue());

        if (bpEntry.isPresent()) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getEquippedStack(slot);
                if (EnchantmentHelper.getLevel(bpEntry.get(), stack) > 0) {
                    count++;
                }
            }
        }
        return count;
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
