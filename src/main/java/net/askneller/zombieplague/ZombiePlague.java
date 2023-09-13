package net.askneller.zombieplague;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.entity.SpawnEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.ZombieEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;

import static net.askneller.zombieplague.entity.ModEntities.SUN_PROOF_ZOMBIE;

@Mod(ZombiePlague.MODID)
public class ZombiePlague {

    public static final String MODID = "zombieplague";
    private static final Logger logger = LogUtils.getLogger();

    // Increase player hunger degradation
    private static final int TICKS_PER_SECOND = 20; // Approximate
    private static final float EXHAUSTION_TICK_FACTOR = 4.0f; // Level of exhaustion that triggers a saturationLevel check
    // Base number of seconds (approx. real time) to trigger a food check
    // (lose one saturationLevel, if saturation is zero then lose one foodLevel, i.e. half a food icon),
    // assuming no eating or other activity
    private static final float SECONDS_PER_FOOD_TICK = 60.0f;
    // Amount to add to exhaustionLevel every tick
    public static final float EXHAUSTION_PER_TICK = EXHAUSTION_TICK_FACTOR / (TICKS_PER_SECOND * SECONDS_PER_FOOD_TICK);

    // Reduce the amount of domesticated animals that spawn
    // The fraction of sheep, cow, pig, and chicken spawns to allow
    public static final double DOMESTIC_ANIMAL_FRACTION = 0.2;

    public ZombiePlague() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(SpawnEvents.class);
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        logger.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            logger.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        logger.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> logger.info("ITEM >> {}", item.toString()));
    }
//
//    @SubscribeEvent
//    public void onServerStarting(ServerStartingEvent event) {
//        // Do something when the server starts
//        logger.info("HELLO from server starting");
//    }

//    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//    public static class ClientModEvents {
//        @SubscribeEvent
//        public static void onClientSetup(FMLClientSetupEvent event) {
//            // Some client setup code
//            logger.info("HELLO FROM CLIENT SETUP");
//            logger.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
//        }
//
//        // Register zombie renderers
//        @SubscribeEvent
//        public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
//            logger.info("Registering renderers");
//            event.registerEntityRenderer(SUN_PROOF_ZOMBIE, ZombieRenderer::new);
//        }
//    }
//
//    // Increase player starvation rate
//    @SubscribeEvent
//    public void onPlayerPostTick(TickEvent.PlayerTickEvent event) {
//        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
//            // Add exhaustion every tick
//            event.player.getFoodData().addExhaustion(EXHAUSTION_PER_TICK);
//        }
//    }
//
//    // Change mob spawning characteristics
//    // This only seems to be called for ModSpawnTypes NATURAL, SPAWNER, and CHUNK_GENERATION
//    @SubscribeEvent
//    public void onCheckEntityPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
//        if (event.getEntityType().getCategory() == MobCategory.MONSTER &&
//                !event.getEntityType().equals(SUN_PROOF_ZOMBIE)) {
//            event.setResult(Event.Result.DENY);
//        }
//
//        // Reduce spawn rate of domestic animals (cows, sheep, pigs, chickens)
//        EntityType<?> type = event.getEntityType();
//        if (type.getCategory() == MobCategory.CREATURE) {
//            if (type.equals(EntityType.COW) || type.equals(EntityType.PIG)
//                    || type.equals(EntityType.CHICKEN) || type.equals(EntityType.SHEEP)) {
//                double random = Math.random();
//                if (random > DOMESTIC_ANIMAL_FRACTION) {
//                    event.setResult(Event.Result.DENY);
//                }
//            }
//        }
//    }
//
//    // Stop village entity spawns
//    @SubscribeEvent
//    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
//        // Villagers and Iron Golems are spawned by type STRUCTURE, which doesn't trigger a SpawnPlacementCheck
//        if (event.getEntity().getType().equals(EntityType.VILLAGER) ||
//                event.getEntity().getType().equals(EntityType.IRON_GOLEM)) {
//            event.setSpawnCancelled(true);
//        }
//    }
//
//    // Disallow zombies spawning aid
//    @SubscribeEvent
//    public void onSummonAid(ZombieEvent.SummonAidEvent event) {
//        event.setResult(Event.Result.DENY);
//    }
//
//    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//    public static class ModEvents {
//
//        // Register new zombie type
//        @SubscribeEvent
//        public static void setupEntities(RegisterEvent event) {
//            if (event.getRegistryKey().equals(ForgeRegistries.Keys.ENTITY_TYPES)) {
//                logger.info("Setting up mod entities");
//                SUN_PROOF_ZOMBIE = build(event.getForgeRegistry(), "zombieplague:sunproofzombie",
//                        EntityType.Builder.<Zombie>of(SunProofZombie::new, MobCategory.MONSTER)
//                                .sized(0.6F, 1.95F)
//                                .clientTrackingRange(8)
//                );
//            }
//        }
//
//        private static <T extends Entity> EntityType<T> build(IForgeRegistry<EntityType> registry,
//                                                              final String key,
//                                                              final EntityType.Builder<T> builder) {
//            EntityType<T> entity = builder.build(key);
//            registry.register(new ResourceLocation(key), entity);
//            return entity;
//        }
//
//        // Create zombie attributes
//        @SubscribeEvent
//        public static void createEntityAttribute(final EntityAttributeCreationEvent event) {
//            logger.info("Creating default SunProofZombie attributes");
//            event.put(SUN_PROOF_ZOMBIE, SunProofZombie.createAttributes().build());
//        }
//
//        // Create zombie spawn restrictions
//        @SubscribeEvent
//        public static void entitySpawnRestrictions(final SpawnPlacementRegisterEvent event) {
//            logger.info("Creating default SunProofZombie spawn placements");
//            event.register(SUN_PROOF_ZOMBIE, SpawnPlacements.Type.ON_GROUND,
//                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
//                    SpawnPlacementRegisterEvent.Operation.REPLACE);
//        }
//    }
}
