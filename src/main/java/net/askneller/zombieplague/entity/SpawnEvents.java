package net.askneller.zombieplague.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.ZombieEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.askneller.zombieplague.ZombiePlague.DOMESTIC_ANIMAL_FRACTION;
import static net.askneller.zombieplague.entity.ModEntities.SUN_PROOF_ZOMBIE;

public class SpawnEvents {

    private static int attemptedTotal = 0;
    private static int attemptedTotalMonsters = 0;
    private static int attemptedTotalCreatures = 0;
    private static int spawnedTotal = 0;
    private static int spawnedTotalMonsters = 0;
    private static int spawnedTotalCreatures = 0;
    private static Map<EntityType<?>, Integer> attemptedMonsters = new HashMap<>();
    private static Map<EntityType<?>, Integer> attemptedCreatures = new HashMap<>();
    private static Map<EntityType<?>, Integer> spawnedMonsters = new HashMap<>();
    private static Map<EntityType<?>, Integer> spawnedCreatures = new HashMap<>();

    private static final Logger logger = LogUtils.getLogger();

    public static int zombiesRemoved = 0;

    // Change mob spawning characteristics
    // This only seems to be called for ModSpawnTypes NATURAL, SPAWNER, and CHUNK_GENERATION
    @SubscribeEvent
    public static void onCheckEntityPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        /*
        attemptedTotal++;
        if (event.getEntityType().getCategory() == MobCategory.CREATURE) {
            attemptedTotalCreatures++;
            attemptedCreatures.computeIfPresent(event.getEntityType(), ((entityType, integer) -> integer + 1));
            attemptedCreatures.computeIfAbsent(event.getEntityType(), (entityType -> 1));
        } else if (event.getEntityType().getCategory() == MobCategory.MONSTER) {
            attemptedTotalMonsters++;
            attemptedMonsters.computeIfPresent(event.getEntityType(), ((entityType, integer) -> integer + 1));
            attemptedMonsters.computeIfAbsent(event.getEntityType(), (entityType -> 1));
        }

        if (attemptedTotalCreatures % 300 == 0) {
            printStatsAttempted();
        }
        */

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

        /*
        if (event.getEntityType().equals(SUN_PROOF_ZOMBIE)) {
            BlockPos pos = event.getPos();
            BlockPos heightmapPos = event.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos);
            logger.info("Checking spawn for Zombie at {}, (height = {}, isSurface {})",
                    pos, heightmapPos.getY(), pos.getY() >= heightmapPos.getY());
        }
        */
    }

    /*
    // DEBUG: SunProofZombies were not spawning even though a lot of placements were being checked.
    // DEBUG: This test here was failing. event.getEntity().checkSpawnRules was returning false. Because the light
    // DEBUG: level dependant walk value was too high for monsters (Monster.getWalkTargetValue).
    // DEBUG: SunProofZombie.getWalkTargetValue implements the same check as Animal.java, which returns a high value
    // DEBUG: if the block underneath is grass.
    @SubscribeEvent
    public static void onCheckPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity().getType().equals(SUN_PROOF_ZOMBIE)) {
            BlockPos pos = new BlockPos((int) event.getX(), (int) event.getY(), (int) event.getZ());
            logger.info("onCheckPosition for Zombie at {}, result = {} ({}, {})", pos, event.getResult(),
                    event.getEntity().checkSpawnRules(event.getLevel(), event.getSpawnType()),
                    event.getEntity().checkSpawnObstruction(event.getLevel()));
        }
    }
    */

    // Stop village entity spawns
    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        /*
        spawnedTotal++;
        if (event.getEntity().getType().getCategory() == MobCategory.CREATURE) {
            spawnedTotalCreatures++;
            spawnedCreatures.computeIfPresent(event.getEntity().getType(), ((entityType, integer) -> integer + 1));
            spawnedCreatures.computeIfAbsent(event.getEntity().getType(), (entityType -> 1));
        } else if (event.getEntity().getType().getCategory() == MobCategory.MONSTER) {
            spawnedTotalMonsters++;
            spawnedMonsters.computeIfPresent(event.getEntity().getType(), ((entityType, integer) -> integer + 1));
            spawnedMonsters.computeIfAbsent(event.getEntity().getType(), (entityType -> 1));
        }
        */

        /*
        if (event.getEntity().getType().equals(EntityType.VILLAGER) && event.getSpawnType() == MobSpawnType.STRUCTURE) {
            logger.info("Here");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            System.out.println("\nTrace");
            int len = 25;
            if (stackTrace.length < 25)
                len = stackTrace.length;
            for (int i = 0; i < len; i++) {
                System.out.println(stackTrace[i]);
            }
            System.out.println("\n");
        }
        */

        // Villagers and Iron Golems are spawned by type STRUCTURE, which doesn't trigger a SpawnPlacementCheck
        if (event.getEntity().getType().equals(EntityType.VILLAGER) ||
                event.getEntity().getType().equals(EntityType.IRON_GOLEM)) {
            event.setSpawnCancelled(true);
        }

        /*
        if (event.getEntity().getType().equals(SUN_PROOF_ZOMBIE)) {
            BlockPos pos = new BlockPos((int) event.getX(), (int) event.getY(), (int) event.getZ());
            BlockPos heightmapPos = event.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos);
            boolean surface = pos.getY() >= heightmapPos.getY();
            logger.info("\nSpawning Zombie at {}, (height = {}, isSurface {})",
                    pos, heightmapPos.getY(), surface);
        }

        if (spawnedTotal % 40 == 0) {
            printStats();
        }
        */
        if (event.getEntity().getType().equals(SUN_PROOF_ZOMBIE)
                && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) {
            logger.info("Spawning zombie: {}", event.getSpawnType());
        }
    }

    // Disallow zombies from summoning aid
    @SubscribeEvent
    public static void onSummonAid(ZombieEvent.SummonAidEvent event) {
        event.setResult(Event.Result.DENY);
    }

    /*
    private static void printStatsAttempted() {
        logger.info("###############################");
        logger.info("Attempted");
        printStats(attemptedCreatures, attemptedTotalCreatures);
        logger.info("###############################");
    }

    private static void printStats() {
        logger.info("###############################");
        logger.info("Attempted");
        printStats(attemptedCreatures, attemptedTotalCreatures);
        logger.info("###############################");
        logger.info("Spawned");
        printStats(spawnedCreatures, spawnedTotalCreatures);
        logger.info("Removed zombies: {}", zombiesRemoved);
        logger.info("###############################");
    }

    private static void printStats(Map<EntityType<?>, Integer> map, Integer total) {
        Set<Map.Entry<EntityType<?>, Integer>> entries = map.entrySet();
        for (Map.Entry<EntityType<?>, Integer> entry : entries) {
            logger.info("{} => {} ({})", entry.getKey(), entry.getValue(), entry.getValue() / (float) total);
        }
    }
    */
}
