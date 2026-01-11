package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.rev.tutorialmod.TutorialMod;

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

        PlayerEntity target = getPlayerLookingAt(client, 50.0);
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
        int health = (int) Math.ceil(player.getHealth());
        sb.append(health).append("HP ");
        sb.append(getArmorDurability(player));
        sb.append("\\n");

        // Line 3: Potion Effects
        String effects = player.getStatusEffects().stream()
                .map(effect -> {
                    Text effectName = effect.getEffectType().getName();
                    int amplifier = effect.getAmplifier();
                    return effectName.getString() + (amplifier > 0 ? " " + (amplifier + 1) : "");
                })
                .collect(Collectors.joining(", "));
        if (!effects.isEmpty()) {
            sb.append(effects);
        }

        return sb.toString();
    }

    private String getArmorDurability(PlayerEntity player) {
        float lowestDurability = 1.0f;
        for (ItemStack armorPiece : player.getArmorItems()) {
            if (!armorPiece.isEmpty()) {
                float durability = (float) (armorPiece.getMaxDamage() - armorPiece.getDamage()) / armorPiece.getMaxDamage();
                if (durability < lowestDurability) {
                    lowestDurability = durability;
                }
            }
        }

        if (lowestDurability >= 0.75f) {
            return "High";
        } else if (lowestDurability >= 0.40f) {
            return "Medium";
        } else if (lowestDurability >= 0.15f) {
            return "Low";
        } else {
            return "Really Low";
        }
    }

    private PlayerEntity getPlayerLookingAt(MinecraftClient client, double maxDistance) {
        if (client.targetedEntity instanceof PlayerEntity && client.player.distanceTo(client.targetedEntity) <= maxDistance) {
            return (PlayerEntity) client.targetedEntity;
        }
        return null;
    }
}
