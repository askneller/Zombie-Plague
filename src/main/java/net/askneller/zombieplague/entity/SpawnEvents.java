package net.askneller.zombieplague.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.ZombieEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import static net.askneller.zombieplague.ZombiePlague.DOMESTIC_ANIMAL_FRACTION;
import static net.askneller.zombieplague.entity.ModEntities.SUN_PROOF_ZOMBIE;

public class SpawnEvents {

    private static final Logger logger = LogUtils.getLogger();

    // Change mob spawning characteristics
    // This only seems to be called for ModSpawnTypes NATURAL, SPAWNER, and CHUNK_GENERATION
    @SubscribeEvent
    public static void onCheckEntityPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        // Only spawn our zombies
        if (event.getEntityType().getCategory() == MobCategory.MONSTER &&
                !event.getEntityType().equals(SUN_PROOF_ZOMBIE)) {
            event.setResult(Event.Result.DENY);
        }

        // Reduce spawn rate of domestic animals (cows, sheep, pigs, chickens)
        EntityType<?> type = event.getEntityType();
        if (type.getCategory() == MobCategory.CREATURE) {
            if (type.equals(EntityType.COW) || type.equals(EntityType.PIG)
                    || type.equals(EntityType.CHICKEN) || type.equals(EntityType.SHEEP)) {
                double random = Math.random();
                if (random > DOMESTIC_ANIMAL_FRACTION) {
                    event.setResult(Event.Result.DENY);
                }
            }
        }
    }

    // Stop village entity spawns
    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Villagers and Iron Golems are spawned by type STRUCTURE, which doesn't trigger a SpawnPlacementCheck
        if (event.getEntity().getType().equals(EntityType.VILLAGER) ||
                event.getEntity().getType().equals(EntityType.IRON_GOLEM)) {
            event.setSpawnCancelled(true);
        }
    }

    // Disallow zombies from summoning aid
    @SubscribeEvent
    public static void onSummonAid(ZombieEvent.SummonAidEvent event) {
        event.setResult(Event.Result.DENY);
    }
}
