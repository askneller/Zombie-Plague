package net.askneller.zombieplague.server;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.client.WeaponLoadingOverlay;
import net.askneller.zombieplague.entity.LightSourceMarkerEntity;
import net.askneller.zombieplague.util.ModTags;
import net.askneller.zombieplague.world.item.BlunderbussItem;
import net.askneller.zombieplague.world.item.MusketItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.slf4j.Logger;

import static net.askneller.zombieplague.ZombiePlague.BLUNDERBUSS;
import java.util.List;

import static net.askneller.zombieplague.ZombiePlague.EXHAUSTION_PER_TICK;
import static net.askneller.zombieplague.ZombiePlague.MUSKET;
import static net.askneller.zombieplague.entity.ModEntities.LIGHT_SOURCE_MARKER;

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
//        if (event.getEntity() instanceof Player) {
//            logger.info("LivingEntityUseItemEvent.Start: item {}, duration {}", event
//                    .getItem().getItem(), event.getDuration());
//        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player) {
//            logger.info("LivingEntityUseItemEvent.Tick: item {}, duration {}", event
//                    .getItem().getItem(), event.getDuration());
            // TODO change to graphic-based notification like crossbow
            Item item = event.getItem().getItem();
            updateLoading(item, event.getDuration());
            if (item instanceof BlunderbussItem && event.getDuration() <= 0) {
                ((BlunderbussItem) item).notifyLoaded((Player) event.getEntity());
            } else if (item instanceof MusketItem && event.getDuration() <= 0) {
                ((MusketItem) item).notifyLoaded((Player) event.getEntity());
            }
        }
    }

    private static void updateLoading(Item item, int remaining) {
        if (remaining <= 0) {
            return;
        }
        float percent = (float) remaining / item.getUseDuration(null);
        if (item instanceof BlunderbussItem) {
            WeaponLoadingOverlay.update("Loading Blunderbuss", (1.0f - percent));
        } else if (item instanceof MusketItem) {
            WeaponLoadingOverlay.update("Loading Musket", (1.0f - percent));
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Stop event) {
//        if (event.getEntity() instanceof Player) {
//            logger.info("LivingEntityUseItemEvent.Stop: item {}, duration {}", event
//                    .getItem().getItem(), event.getDuration());
//            if (event.getItem().is(BLUNDERBUSS.get()) || event.getItem().is(MUSKET.get())) {
//                WeaponLoadingOverlay.clear();
//            }
//        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
//        if (event.getEntity() instanceof Player) {
//            logger.info("LivingEntityUseItemEvent.Finish: item {}, duration {}", event
//                    .getItem().getItem(), event.getDuration());
//        }
    }

    /*
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity().getType().equals(ModEntities.BLUNDERBUSS_SHOT)) {
            logger.info("Leave {}", event.getEntity());
        }
    }
    */


    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        EntityType<?> type = event.getEntity().getType();
        if (type.equals(EntityType.MARKER) || type.equals(LIGHT_SOURCE_MARKER)) {
            logger.info("Join {}", event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().is(ModTags.Blocks.LIGHT_SOURCE)) {
            logger.info("Placed {}", event.getPlacedBlock());
            LightSourceMarkerEntity entity = new LightSourceMarkerEntity(LIGHT_SOURCE_MARKER, event.getEntity().level());
            BlockPos pos = event.getPos();
            entity.setPos(pos.getX(), pos.getY(), pos.getZ());
            event.getEntity().level().addFreshEntity(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRemoveBlock(BlockEvent.BreakEvent event) {
        if (event.getState().is(ModTags.Blocks.LIGHT_SOURCE)) {
            logger.info("Removed {}", event.getState());
            BlockPos pos = event.getPos();
            AABB aabb = new AABB(pos.getX() - 1.0D,
                    pos.getY() - 1.0D,
                    pos.getZ() - 1.0D,
                    pos.getX() + 1.0D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 1.0D);
            List<LightSourceMarkerEntity> entitiesOfClass =
                    event.getLevel().getEntitiesOfClass(LightSourceMarkerEntity.class, aabb);
            logger.info("entitiesOfClass {}", entitiesOfClass.size());
            if (!entitiesOfClass.isEmpty()) {
                logger.info("Discarding {}", entitiesOfClass);
                entitiesOfClass.forEach(Entity::discard);
                entitiesOfClass =
                        event.getLevel().getEntitiesOfClass(LightSourceMarkerEntity.class, aabb);
                logger.info("after entitiesOfClass {}", entitiesOfClass.size());
            }
        }
    }

}
