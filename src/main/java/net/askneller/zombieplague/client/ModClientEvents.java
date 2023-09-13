package net.askneller.zombieplague.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import static net.askneller.zombieplague.ZombiePlague.MODID;
import static net.askneller.zombieplague.entity.ModEntities.SUN_PROOF_ZOMBIE;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    private static final Logger logger = LogUtils.getLogger();
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        logger.info("HELLO FROM CLIENT SETUP");
        logger.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    // Register zombie renderers
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        logger.info("Registering renderers");
        event.registerEntityRenderer(SUN_PROOF_ZOMBIE, ZombieRenderer::new);
    }
}
