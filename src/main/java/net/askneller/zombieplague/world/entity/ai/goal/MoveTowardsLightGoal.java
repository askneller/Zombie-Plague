package net.askneller.zombieplague.world.entity.ai.goal;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.entity.LightSourceMarkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class MoveTowardsLightGoal extends Goal {

    private static final Logger logger = LogUtils.getLogger();
    public static final float DEFAULT_PROBABILITY = 0.95F; // todo for debugging
    // How far from a source the mob will stop when approaching
    public static final double DESIRED_LIGHT_SOURCE_DISTANCE = 5.0;
    public static final double MAX_LIGHT_SOURCE_DISTANCE = 50.0;
    public static final int CHECK_AROUND_DISTANCE = 3;
    protected final Mob mob;
    @Nullable
    protected Entity lookAt;
    protected final float lookDistance;
    private int lookTime;
    protected final float probability;
    private final boolean onlyHorizontal;
    protected final Class<? extends LivingEntity> lookAtType;
    protected final TargetingConditions lookAtContext;

    protected BlockPos destination;

    public MoveTowardsLightGoal(Mob p_25520_, Class<? extends LivingEntity> p_25521_, float p_25522_) {
        this(p_25520_, p_25521_, p_25522_, DEFAULT_PROBABILITY);
    }

    public MoveTowardsLightGoal(Mob p_25524_, Class<? extends LivingEntity> p_25525_, float p_25526_, float p_25527_) {
        this(p_25524_, p_25525_, p_25526_, p_25527_, false);
    }

    public MoveTowardsLightGoal(Mob p_148118_, Class<? extends LivingEntity> p_148119_, float lookDistance, float probability, boolean p_148122_) {
//        logger.info("Created behaviour");
        this.mob = p_148118_;
        this.lookAtType = p_148119_;
        this.lookDistance = lookDistance;
        this.probability = probability;
        this.onlyHorizontal = p_148122_;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        if (p_148119_ == Player.class) {
            this.lookAtContext = TargetingConditions.forNonCombat().range((double)lookDistance).selector((p_25531_) -> {
                return EntitySelector.notRiding(p_148118_).test(p_25531_);
            });
        } else {
            this.lookAtContext = TargetingConditions.forNonCombat().range((double)lookDistance);
        }

    }

    public boolean canUse() {
        if (this.mob.getRandom().nextFloat() >= this.probability) {
//            logger.info("Below probability");
            return false;
        } else {
            Player nearestPlayer = this.mob.level().getNearestPlayer(this.mob, 10.0);
            if (nearestPlayer == null) {
                return false;
            }
//            if (this.mob.getTarget() != null) {
//                this.lookAt = this.mob.getTarget();
//            }
//
//            if (this.lookAtType == Player.class) {
//                this.lookAt = this.mob.level().getNearestPlayer(this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
//            } else {
//                this.lookAt = this.mob.level().getNearestEntity(this.mob.level().getEntitiesOfClass(this.lookAtType, this.mob.getBoundingBox().inflate((double)this.lookDistance, 3.0D, (double)this.lookDistance), (p_148124_) -> {
//                    return true;
//                }), this.lookAtContext, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
//            }
//
//            return this.lookAt != null;
//            return true;

            // it has to be dark enough to see the light
            // todo ignore for now
//            int rawBrightness = this.mob.level().getLightEngine().getRawBrightness(this.mob.blockPosition(), 0);
//            int lightValue = this.mob.level().getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(this.mob.blockPosition());
//            if (lightValue > LightEngine.MAX_LEVEL / 3) {
//                logger.info("Too bright, raw {}, val {}", rawBrightness, lightValue);
//                return false;
//            }

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
                logger.info("Can use, close lights: {}", closeLights.size());
                boolean lineOfSight = false;
                for (Entity entity: closeLights) {
                    lineOfSight = this.mob.hasLineOfSight(closeLights.get(0));
                    logger.info("line of sight to close {}: {}", entity, lineOfSight);
                    if (lineOfSight) return false;
                }
            } else {
                logger.info("None close");
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
            logger.info("canUse, num in max: {}", inRangeLights.size());
            if (!inRangeLights.isEmpty()) {
                logger.info("Can use, lights in range: {}", inRangeLights.size());
                boolean lineOfSight = false;
                for (Entity entity: inRangeLights) {
                    lineOfSight = this.mob.hasLineOfSight(entity);
                    logger.info("Direct line of sight to {}", entity);
                    if (lineOfSight) {
                        logger.info("Setting destination {}", entity.blockPosition());
                        this.destination = entity.blockPosition();
                        return true;
                    }
                    else {
                        BlockPos pos = entity.blockPosition();
                        // can the entity see the "effect" of the light, i.e. the area lit up by it
                        // will implement this by seeing if the entity has LoS to blocks near the light source
                        BlockPos blockPos = pos.above(CHECK_AROUND_DISTANCE);
                        boolean los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        logger.info("LoS to above {}: {}", blockPos, los);
                        if (los) {
                            logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            return true;
                        }
                        blockPos = pos.below(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        logger.info("LoS to below {}: {}", blockPos, los);
                        if (los) {
                            logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            return true;
                        }
                        blockPos = pos.north(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        logger.info("LoS to north {}: {}", blockPos, los);
                        if (los) {
                            logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            return true;
                        }
                        blockPos = pos.south(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        logger.info("LoS to south {}: {}", blockPos, los);
                        if (los) {
                            logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            return true;
                        }
                        blockPos = pos.east(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        logger.info("LoS to east {}: {}", blockPos, los);
                        if (los) {
                            logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            return true;
                        }
                        blockPos = pos.west(CHECK_AROUND_DISTANCE);
                        los = hasLineOfSight(this.mob, blockPos, entity.level(), entity);
                        logger.info("LoS to west {}: {}", blockPos, los);
                        if (los) {
                            logger.info("Setting destination {}", pos);
                            this.destination = pos;
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public boolean canContinueToUse() {
//        if (!this.lookAt.isAlive()) {
//            return false;
//        } else if (this.mob.distanceToSqr(this.lookAt) > (double)(this.lookDistance * this.lookDistance)) {
//            return false;
//        } else {
//            return this.lookTime > 0;
//        }
        // TODO create empty entity to place with torches or campfires
        // TODO see Marker extends Entity
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
            logger.info("Can continue, entities at pos {}: {}", this.destination, entities.size());
            logger.info("{}", entities.get(0));
        }
        return !entities.isEmpty();
    }

    public void start() {
        this.lookTime = this.adjustedTickDelay(40 + this.mob.getRandom().nextInt(40));
    }

    public void stop() {
        this.lookAt = null;
    }

    public void tick() {
//        if (this.lookAt.isAlive()) {
//            double d0 = this.onlyHorizontal ? this.mob.getEyeY() : this.lookAt.getEyeY();
//            this.mob.getLookControl().setLookAt(this.lookAt.getX(), d0, this.lookAt.getZ());
//            --this.lookTime;
//        }
//        logger.info("Tick");
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

}
