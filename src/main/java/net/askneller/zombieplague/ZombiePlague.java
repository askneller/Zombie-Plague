package net.askneller.zombieplague;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.entity.SpawnEvents;
import net.askneller.zombieplague.server.ServerEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
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

}
