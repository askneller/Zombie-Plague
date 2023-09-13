package net.askneller.zombieplague.server;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.TickEvent;
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
}
