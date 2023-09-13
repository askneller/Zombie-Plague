package net.askneller.zombieplague;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
import net.minecraftforge.registries.*;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ZombiePlague.MODID)
public class ZombiePlague
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "zombieplague";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "zombieplague" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "zombieplague" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "zombieplague" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "zombieplague:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "zombieplague:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates a new food item with the id "zombieplague:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "zombieplague:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    private static final int TICKS_PER_SECOND = 20; // Approximate

    private static final float EXHAUSTION_TICK_FACTOR = 4.0f; // Level of exhaustion that triggers a saturationLevel check

    // Base number of seconds (approx. real time) to trigger a food check
    // (lose one saturationLevel, if saturation is zero then lose one foodLevel, i.e. half a food icon),
    // assuming no eating or other activity
    private static final float SECONDS_PER_FOOD_TICK = 60.0f;

    // Amount to add to exhaustionLevel every tick
    private static final float EXHAUSTION_PER_TICK = EXHAUSTION_TICK_FACTOR / (TICKS_PER_SECOND * SECONDS_PER_FOOD_TICK);

    private static long systemTimeStart;
    private static long systemTimeLast;
    private static float lastExhaust = 5.0f;

    private static int spawnedTotal = 0;
    private static int sunZombieSpawned = 0;
    private static Map<EntityType<?>, Integer> attempted = new HashMap<>();
    private static Map<EntityType<?>, Integer> actual = new HashMap<>();

    // The fraction of sheep, cow, pig, and chicken spawns to allow
    private static final double DOMESTIC_ANIMAL_FRACTION = 0.2;

    public static EntityType<? extends Zombie> SUN_PROOF_ZOMBIE;

    public ZombiePlague()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        systemTimeStart = System.currentTimeMillis();
        systemTimeLast = systemTimeStart;
        LOGGER.info("HELLO from server starting, time {}", systemTimeStart);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        // ======================================================================================
        // Adam Zombie Mod Stuff
        @SubscribeEvent
        public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event)
        {
            LOGGER.info("Registering renderers");
            event.registerEntityRenderer(SUN_PROOF_ZOMBIE, ZombieRenderer::new);
        }
    }


    // ======================================================================================
    // ======================================================================================
    // ======================================================================================
    // Adam Zombie Mod Stuff

    // TODO for changing food related stuff look at net.minecraft.world.food.FoodData and its tick() method, called
    // TODO from Player.tick()
    // TODO Look at making changes in player post tick, see
    //
    @SubscribeEvent
    public void onPlayerPostTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
//            long gameTime = event.player.level().getGameTime();
//            long dayTime = event.player.level().getDayTime();
//            long millis = System.currentTimeMillis();
//            LOGGER.info("Elapsed {} ms", (millis - systemTimeLast));
//            systemTimeLast = millis;
//            LOGGER.info("Player tick: Game {}, Day {}", gameTime, dayTime);
//            LOGGER.info("FoodData {}", printFoodData(event.player.getFoodData()));
//            float exhaust = event.player.getFoodData().getExhaustionLevel();
//            if (exhaust < lastExhaust) {
//                LOGGER.info("Exhaust reset. Client? {}", event.player.level().isClientSide);
//                long millis = System.currentTimeMillis();
//                LOGGER.info("Elapsed {} ms", (millis - systemTimeLast));
//                LOGGER.info("{}", printFoodData(event.player.getFoodData()));
//                systemTimeLast = millis;
//            }
//            lastExhaust = exhaust;
            // Add exhaustion every tick
            event.player.getFoodData().addExhaustion(EXHAUSTION_PER_TICK);
        }

    }
    // in ForgeEventFactory

    private String printFoodData(FoodData data) {
        return String.format("lvl %d, sat %f, exh %f, last %d",
                data.getFoodLevel(), data.getSaturationLevel(), data.getExhaustionLevel(), data.getLastFoodLevel());
    }


    @SubscribeEvent
    public void onCheckEntityPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
//        LOGGER.info("Spawn check for {} at {}, ({})", event.getEntityType(), event.getPos(), event.getSpawnType());
        // This only seems to be called for ModSpawnTypes NATURAL, SPAWNER and CHUNK_GENERATION
        if (event.getEntityType().getCategory() == MobCategory.MONSTER) {
//            ++spawnedTotal;
//            LOGGER.info("Spawn check for {} at {}, ({})", event.getEntityType(), event.getPos(), event.getSpawnType());
//
//            if (event.getEntityType().equals(SUN_PROOF_ZOMBIE)) {
//                ++sunZombieSpawned;
//                LOGGER.info("Total {}, myZombie {}, fraction {}", spawnedTotal, sunZombieSpawned, ((float) sunZombieSpawned / spawnedTotal));
//            }

            if (event.getEntityType().getCategory() == MobCategory.MONSTER &&
                    !event.getEntityType().equals(SUN_PROOF_ZOMBIE)) {
//            LOGGER.info("DENIED!");
                event.setResult(Event.Result.DENY);
            } else {
                BlockPos pos = event.getPos();
                BlockPos heightmapPos = event.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos);
//                LOGGER.info("Spawn check for {} at {}, ({}), height {}, surface {}", event.getEntityType(), pos,
//                        event.getSpawnType(), heightmapPos.getY(), pos.getY() >= heightmapPos.getY());
            }
        }

        // Reduce spawn rate of domestic animals (cows, sheep, pigs, chickens)
        EntityType<?> type = event.getEntityType();
        if (type.getCategory() == MobCategory.CREATURE) {
//            ++spawnedTotal;
//            attempted.computeIfPresent(type, (entityType, number) -> number + 1);
//            attempted.computeIfAbsent(type, (entityType -> 0));

            if (type.equals(EntityType.COW) || type.equals(EntityType.PIG)
                    || type.equals(EntityType.CHICKEN) || type.equals(EntityType.SHEEP)) {
                double random1 = Math.random();
                if (random1 > DOMESTIC_ANIMAL_FRACTION) {
                    event.setResult(Event.Result.DENY);
                }
//                else {
//                    actual.computeIfPresent(type, (entityType, number) -> number + 1);
//                    actual.computeIfAbsent(type, (entityType -> 0));
//                }
            }

//            if (spawnedTotal % 100 == 0) {
//                LOGGER.info("\nAttempted");
//                Set<Map.Entry<EntityType<?>, Integer>> entries = attempted.entrySet();
//                for (Map.Entry<EntityType<?>, Integer> entry : entries) {
//                    LOGGER.info("{} - {}", entry.getKey(), entry.getValue());
//                }LOGGER.info("\nActual");
//                entries = actual.entrySet();
//                for (Map.Entry<EntityType<?>, Integer> entry : entries) {
//                    LOGGER.info("{} - {}", entry.getKey(), entry.getValue());
//                }
//            }
        }
//        else if (event.getEntityType().equals(EntityType.VILLAGER)
//                || event.getEntityType().equals(EntityType.WANDERING_TRADER)
//                || event.getEntityType().equals(EntityType.WITHER_SKULL)
//                || event.getEntityType().equals(EntityType.IRON_GOLEM)
//                || event.getEntityType().equals(EntityType.SNOW_GOLEM)) {
//            LOGGER.info("Spawn check for {} at {}", event.getEntityType(), event.getPos());
//            event.setResult(Event.Result.DENY);
//        }
    }

    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Villagers and Iron Golems are spawned by type STRUCTURE, which doesn't trigger a SpawnPlacementCheck
        if (event.getEntity().getType().equals(EntityType.VILLAGER)) {
//            LOGGER.info("Finalize villager at {}, {}, {}, type: {}", event.getX(), event.getY(), event.getZ(), event.getSpawnType());
            event.setSpawnCancelled(true);
        } else if (event.getEntity().getType().equals(EntityType.IRON_GOLEM)) {
//            LOGGER.info("Finalize golem at {}, {}, {}, type: {}", event.getX(), event.getY(), event.getZ(), event.getSpawnType());
            event.setSpawnCancelled(true);
        }

        if (event.getEntity().getType().equals(SUN_PROOF_ZOMBIE)) {
//            LOGGER.info("\nFinalize sun Zombie at {}, {}, {}, type: {}",
//                    event.getX(), event.getY(), event.getZ(), event.getSpawnType());

            BlockPos pos = new BlockPos((int) event.getX(), (int) event.getY(), (int) event.getZ());
            BlockPos heightmapPos = event.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos);
                LOGGER.info("Spawn check for {} at {}, ({}), height {}, surface {}", SUN_PROOF_ZOMBIE, pos,
                        event.getSpawnType(), heightmapPos.getY(), pos.getY() >= heightmapPos.getY());
        }
    }

    @SubscribeEvent
    public void onSummonAid(ZombieEvent.SummonAidEvent event) {
//        LOGGER.info("Summoning aid at x {}, y {}, z {}", event.getX(), event.getY(), event.getZ());
        event.setResult(Event.Result.DENY);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void setupEntities(RegisterEvent event) {
            if (event.getRegistryKey().equals(ForgeRegistries.Keys.ENTITY_TYPES)) {
                LOGGER.info("Setting up mod entities");
                SUN_PROOF_ZOMBIE = build(event.getForgeRegistry(), "zombieplague:sunproofzombie",
                        EntityType.Builder.<Zombie>of(SunProofZombie::new, MobCategory.MONSTER)
                                .sized(0.6F, 1.95F)
                                .clientTrackingRange(8)
                );
            }
        }

        private static <T extends Entity> EntityType<T> build(IForgeRegistry<EntityType> registry,
                                                              final String key,
                                                              final EntityType.Builder<T> builder)
        {
            EntityType<T> entity = builder.build(key);

            registry.register(new ResourceLocation(key), entity);
            return entity;
        }

        @SubscribeEvent
        public static void createEntityAttribute(final EntityAttributeCreationEvent event)
        {
            LOGGER.info("Creating default Zombie attributes");
            event.put(SUN_PROOF_ZOMBIE, SunProofZombie.createAttributes().build());
        }
        @SubscribeEvent
        public static void entitySpawnRestrictions(final SpawnPlacementRegisterEvent event)
        {
            LOGGER.info("Creating default Zombie spawn placements");
            event.register(SUN_PROOF_ZOMBIE, SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}
