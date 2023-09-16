package net.askneller.zombieplague.server;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.entity.ModEntities;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.slf4j.Logger;

import static net.askneller.zombieplague.ZombiePlague.EXHAUSTION_PER_TICK;

public class ServerEvents {

    private static final Logger logger = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        logger.info("HELLO from server starting");
    }

    // Increase player starvation rate
    @SubscribeEvent
    public static void onPlayerPostTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            // Add exhaustion every tick
            event.player.getFoodData().addExhaustion(EXHAUSTION_PER_TICK);
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player) {
            logger.info("LivingEntityUseItemEvent.Start: item {}, duration {}", event
                    .getItem().getItem(), event.getDuration());
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player) {
            logger.info("LivingEntityUseItemEvent.Tick: item {}, duration {}", event
                    .getItem().getItem(), event.getDuration());
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof Player) {
            logger.info("LivingEntityUseItemEvent.Stop: item {}, duration {}", event
                    .getItem().getItem(), event.getDuration());
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player) {
            logger.info("LivingEntityUseItemEvent.Finish: item {}, duration {}", event
                    .getItem().getItem(), event.getDuration());
        }
    }

    /*
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity().getType().equals(ModEntities.BLUNDERBUSS_SHOT)) {
            logger.info("Leave {}", event.getEntity());
        }
    }
    */
}
