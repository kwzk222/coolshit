package net.rev.tutorialmod.modules.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.rev.tutorialmod.TutorialMod;

public class TargetFilters {

    public static boolean isValidTarget(Entity entity) {
        if (entity == null) {
            return false;
        }

        // Player Checks
        if (entity instanceof PlayerEntity) {
            if (!TutorialMod.CONFIG.triggerBotIncludePlayers) {
                return false;
            }
            if (TutorialMod.CONFIG.triggerBotExcludeTeammates && TutorialMod.CONFIG.teamManager.isTeammate(entity.getName().getString())) {
                return false;
            }
            return true;
        }

        // End Crystal Checks
        if (entity instanceof EndCrystalEntity) {
            return TutorialMod.CONFIG.triggerBotIncludeCrystals;
        }

        SpawnGroup spawnGroup = entity.getType().getSpawnGroup();

        // Hostile Mob Checks
        if (spawnGroup == SpawnGroup.MONSTER) {
            return TutorialMod.CONFIG.triggerBotIncludeHostiles;
        }

        // Passive Mob Checks
        if (spawnGroup == SpawnGroup.CREATURE || spawnGroup == SpawnGroup.AMBIENT || spawnGroup == SpawnGroup.WATER_CREATURE) {
            if (!TutorialMod.CONFIG.triggerBotIncludePassives) {
                return false;
            }
            if (entity instanceof VillagerEntity && TutorialMod.CONFIG.triggerBotExcludeVillagers) {
                return false;
            }
            return true;
        }

        return false;
    }
}
