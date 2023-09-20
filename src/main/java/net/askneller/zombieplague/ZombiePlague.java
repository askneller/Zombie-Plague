package net.askneller.zombieplague;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.entity.SpawnEvents;
import net.askneller.zombieplague.server.ServerEvents;
import net.askneller.zombieplague.sound.ModSounds;
import net.askneller.zombieplague.world.item.BlunderbussItem;
import net.askneller.zombieplague.world.item.MusketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

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

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final RegistryObject<Item> PLANT_FIBRE = ITEMS.register("plant_fibre", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLUNDERBUSS = ITEMS.register("blunderbuss",
            () -> new BlunderbussItem(new Item.Properties().stacksTo(1).durability(465)));
    public static final RegistryObject<Item> BLUNDERBUSS_SHOT = ITEMS.register("blunderbuss_shot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MUSKET = ITEMS.register("musket",
            () -> new MusketItem(new Item.Properties().stacksTo(1).durability(465)));
    public static final RegistryObject<Item> MUSKET_BALL = ITEMS.register("musket_ball", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_NITRE = ITEMS.register("raw_nitre", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SALTPETRE = ITEMS.register("saltpetre", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Block> NITRE = BLOCKS.register("nitre", () -> new Block(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.0F, 3.0F)));
    public static final RegistryObject<Item> NITRE_BLOCK_ITEM = ITEMS.register("nitre", () -> new BlockItem(NITRE.get(), new Item.Properties()));

    public static final RegistryObject<Block> SULFUR_ORE = BLOCKS.register("sulfur_ore", () -> new Block(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.0F, 3.0F)));
    public static final RegistryObject<Item> SULFUR_ORE_BLOCK_ITEM = ITEMS.register("sulfur_ore", () -> new BlockItem(SULFUR_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RAW_SULFUR = ITEMS.register("raw_sulfur", () -> new Item(new Item.Properties()));

    public ZombiePlague() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);

        ModSounds.register(modEventBus);

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

}
