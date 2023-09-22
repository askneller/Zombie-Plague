package net.askneller.zombieplague.world.entity.ai.goal;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.entity.LightSourceMarkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;

public class MoveTowardsLightGoal extends RandomStrollGoal {

    private static final Logger logger = LogUtils.getLogger();
    public static final float DEFAULT_PROBABILITY = 0.95F; // todo for debugging
    // How far from a source the mob will stop when approaching
    public static final double DESIRED_LIGHT_SOURCE_DISTANCE = 10.0;
    public static final double DEFAULT_SPEED_MODIFIER = 0.8;
    public static final double MIN_DISTANCE = 5.0;
    public static final double MAX_LIGHT_SOURCE_DISTANCE = 50.0;
    public static final int CHECK_AROUND_DISTANCE = 3;

    protected final float probability = DEFAULT_PROBABILITY;

    protected BlockPos destination;

    public MoveTowardsLightGoal(PathfinderMob mob, double speedModifier, int interval) {
        super(mob, speedModifier, interval);
    }

    public boolean canUse() {
        // TODO implement an interval between selection
        if (this.mob.getRandom().nextFloat() >= this.probability) {
//            logger.info("Below probability");
            return false;
        } else {
            // todo remove after debugging
            Player nearestPlayer = this.mob.level().getNearestPlayer(this.mob, 15.0);
            if (nearestPlayer == null) {
                return false;
            }

            // it has to be dark enough to see the light
            int rawBrightness = this.mob.level().getLightEngine().getRawBrightness(this.mob.blockPosition(), 0);
            int lightValue = this.mob.level().getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(this.mob.blockPosition());
            int maxLocalRawBrightness = this.mob.level().getMaxLocalRawBrightness(this.mob.blockPosition());
            if (maxLocalRawBrightness > LightEngine.MAX_LEVEL / 2) {
                if (printDebug(this.mob.level())) {
                    logger.info("Too bright, raw {}, val {}", rawBrightness, lightValue);
                    logger.info("Sky darken {}", this.mob.level().getSkyDarken());
                    logger.info("Max local {}", this.mob.level().getMaxLocalRawBrightness(this.mob.blockPosition()));
                }
                return false;
            }

            // the mob is already near light sources and can see at least one directly
            // ignore any sources that the mob cannot see "near" (a few blocks either side)
            List<Entity> closeLights = this.mob.level().getEntities(this.mob,
                    new AABB(this.mob.getX() - DESIRED_LIGHT_SOURCE_DISTANCE,
                            this.mob.getY() - DESIRED_LIGHT_SOURCE_DISTANCE,
                            this.mob.getZ() - DESIRED_LIGHT_SOURCE_DISTANCE,
                            this.mob.getX() + DESIRED_LIGHT_SOURCE_DISTANCE,
                            this.mob.getY() + DESIRED_LIGHT_SOURCE_DISTANCE,
                            this.mob.getZ() + DESIRED_LIGHT_SOURCE_DISTANCE),
                    (entity -> entity instanceof LightSourceMarkerEntity));
            if (!closeLights.isEmpty()) {
                if (printDebug(this.mob.level())) logger.info("Can use, close lights: {}", closeLights.size());
                boolean lineOfSight = false;
                for (Entity entity: closeLights) {
                    lineOfSight = this.mob.hasLineOfSight(closeLights.get(0));
                    if (printDebug(this.mob.level())) logger.info("line of sight to close {}: {}", entity, lineOfSight);
                    if (lineOfSight) return false;
                }
            } else {
                if (printDebug(this.mob.level())) logger.info("None close");
            }

            // there is a light source in range
            // the mob can see it, or around it
            List<Entity> inRangeLights = this.mob.level().getEntities(this.mob,
                    new AABB(this.mob.getX() - MAX_LIGHT_SOURCE_DISTANCE,
                            this.mob.getY() - MAX_LIGHT_SOURCE_DISTANCE,
                            this.mob.getZ() - MAX_LIGHT_SOURCE_DISTANCE,
                            this.mob.getX() + MAX_LIGHT_SOURCE_DISTANCE,
                            this.mob.getY() + MAX_LIGHT_SOURCE_DISTANCE,
                            this.mob.getZ() + MAX_LIGHT_SOURCE_DISTANCE),
                    (entity -> entity instanceof LightSourceMarkerEntity));
            if (printDebug(this.mob.level())) logger.info("canUse, num in max: {}", inRangeLights.size());
            if (!inRangeLights.isEmpty()) {
                if (printDebug(this.mob.level())) logger.info("Can use, lights in range: {}", inRangeLights.size());
                boolean lineOfSight = false;
                for (Entity entity: inRangeLights) {
                    lineOfSight = this.mob.hasLineOfSight(entity);
                    if (printDebug(this.mob.level())) logger.info("Direct line of sight to {}", entity);
                    if (lineOfSight) {
                        if (printDebug(this.mob.level())) logger.info("Setting destination {}", entity.blockPosition());
                        this.destination = entity.blockPosition();
                        setDestination(this.destination);
                        return true;
                    }
                    else {
                        BlockPos pos = entity.blockPosition();
                        // can the entity see the "effect" of the light, i.e. the area lit up by it
                        // will implement this by seeing if the entity has LoS to blocks near the light source
                        BlockPos blockPos = pos.above(CHECK_AROUND_DISTANCE);
                        boolean los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        if (printDebug(this.mob.level())) logger.info("LoS to above {}: {}", blockPos, los);
                        if (los) {
                            if (printDebug(this.mob.level())) logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            setDestination(this.destination);
                            return true;
                        }
                        blockPos = pos.below(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        if (printDebug(this.mob.level())) logger.info("LoS to below {}: {}", blockPos, los);
                        if (los) {
                            if (printDebug(this.mob.level())) logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            setDestination(this.destination);
                            return true;
                        }
                        blockPos = pos.north(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        if (printDebug(this.mob.level())) logger.info("LoS to north {}: {}", blockPos, los);
                        if (los) {
                            if (printDebug(this.mob.level())) logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            setDestination(this.destination);
                            return true;
                        }
                        blockPos = pos.south(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        if (printDebug(this.mob.level())) logger.info("LoS to south {}: {}", blockPos, los);
                        if (los) {
                            if (printDebug(this.mob.level())) logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            setDestination(this.destination);
                            return true;
                        }
                        blockPos = pos.east(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        if (printDebug(this.mob.level())) logger.info("LoS to east {}: {}", blockPos, los);
                        if (los) {
                            if (printDebug(this.mob.level())) logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            setDestination(this.destination);
                            return true;
                        }
                        blockPos = pos.west(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        if (printDebug(this.mob.level())) logger.info("LoS to west {}: {}", blockPos, los);
                        if (los) {
                            if (printDebug(this.mob.level())) logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            setDestination(this.destination);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public boolean canContinueToUse() {
        if (this.destination == null) return false;

        // is the light still there
        List<Entity> entities = this.mob.level().getEntities(this.mob,
                new AABB(this.destination.getX() - 2.0D,
                        this.destination.getY() - 2.0D,
                        this.destination.getZ() - 2.0D,
                        this.destination.getX() + 2.0D,
                        this.destination.getY() + 2.0D,
                        this.destination.getZ() + 2.0D),
                (entity -> entity instanceof LightSourceMarkerEntity));
        if (!entities.isEmpty()) {
            if (printDebug(this.mob.level())) logger.info("Can continue, entities at pos {}: {}", this.destination, entities.size());
            if (printDebug(this.mob.level())) logger.info("{}", entities.get(0));
            boolean anyMatch = entities.stream().anyMatch((entity -> {
                if (printDebug(this.mob.level())) logger.info("Distance {}, from {}", this.mob.distanceTo(entity), this.mob.blockPosition());
                boolean inDistance = this.mob.distanceTo(entity) <= MIN_DISTANCE;
                if (inDistance) {
                    if (printDebug(this.mob.level())) logger.info("inDistance {}, {}", entity, this.mob.distanceTo(entity));
                    return true;
                }
                return false;
            }));
            if (printDebug(this.mob.level())) logger.info("anyMatch {}", anyMatch);
            if (printDebug(this.mob.level())) logger.info("NAV {}", this.mob.getNavigation().isInProgress());
            return !anyMatch;

        }
        return false;
    }

    private void setDestination(BlockPos pos) {
        if (pos != null) {
            this.wantedX = pos.getX();
            this.wantedY = pos.getY();
            this.wantedZ = pos.getZ();
        }
    }

    public void start() {
        if (printDebug(this.mob.level())) logger.info("Starting go to {}", this.destination);
        super.start();
        if (printDebug(this.mob.level())) logger.info("nav target {}", this.mob.getNavigation().getTargetPos());
    }

    public boolean hasLineOfSight(Entity from, BlockPos to, Level toLevel, Entity p_147185_) {
        if (toLevel != from.level()) {
            return false;
        } else {
            Vec3 vec3 = new Vec3(from.getX(), from.getEyeY(), from.getZ());
            Vec3 vec31 = new Vec3(to.getX(), to.getY(), to.getZ());
            if (vec31.distanceTo(vec3) > 128.0D) {
                return false;
            } else {
                return from.level().clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == HitResult.Type.MISS;
            }
        }
    }

    private boolean printDebug(Level level) {
        return level.getGameTime() % 20 == 0;
    }
}
