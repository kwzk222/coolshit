package net.rev.tutorialmod.modules.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.rev.tutorialmod.TutorialMod;

public class TargetFilters {

    public static boolean isValidTarget(Entity entity) {
        return isValidTarget(entity, false);
    }

    public static boolean isValidTarget(Entity entity, boolean forAimAssist) {
        if (entity == null) {
            return false;
        }

        boolean includePlayers = forAimAssist ? TutorialMod.CONFIG.aimAssistIncludePlayers : TutorialMod.CONFIG.triggerBotIncludePlayers;
        boolean excludeTeammates = forAimAssist ? TutorialMod.CONFIG.aimAssistExcludeTeammates : TutorialMod.CONFIG.triggerBotExcludeTeammates;
        boolean includeHostiles = forAimAssist ? TutorialMod.CONFIG.aimAssistIncludeHostiles : TutorialMod.CONFIG.triggerBotIncludeHostiles;
        boolean includePassives = forAimAssist ? TutorialMod.CONFIG.aimAssistIncludePassives : TutorialMod.CONFIG.triggerBotIncludePassives;
        boolean excludeVillagers = forAimAssist ? TutorialMod.CONFIG.aimAssistExcludeVillagers : TutorialMod.CONFIG.triggerBotExcludeVillagers;
        boolean includeCrystals = forAimAssist ? false : TutorialMod.CONFIG.triggerBotIncludeCrystals; // crystals not usually for aim assist

        // Player Checks
        if (entity instanceof PlayerEntity) {
            if (!includePlayers) {
                return false;
            }
            if (excludeTeammates && TutorialMod.CONFIG.teamManager.isTeammate(entity.getName().getString())) {
                return false;
            }
            return true;
        }

        // End Crystal Checks
        if (entity instanceof EndCrystalEntity) {
            return includeCrystals;
        }

        SpawnGroup spawnGroup = entity.getType().getSpawnGroup();

        // Hostile Mob Checks
        if (spawnGroup == SpawnGroup.MONSTER) {
            return includeHostiles;
        }

        // Passive Mob Checks
        if (spawnGroup == SpawnGroup.CREATURE || spawnGroup == SpawnGroup.AMBIENT || spawnGroup == SpawnGroup.WATER_CREATURE) {
            if (!includePassives) {
                return false;
            }
            if (entity instanceof VillagerEntity && excludeVillagers) {
                return false;
            }
            return true;
        }

        return false;
    }
}
