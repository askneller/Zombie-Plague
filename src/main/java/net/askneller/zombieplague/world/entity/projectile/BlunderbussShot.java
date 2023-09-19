package net.askneller.zombieplague.world.entity.projectile;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.world.item.BlunderbussItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import static net.askneller.zombieplague.world.item.BlunderbussItem.DEFAULT_RANGE;
import static net.askneller.zombieplague.world.item.BlunderbussItem.MAX_DAMAGE;
import static net.askneller.zombieplague.world.item.BlunderbussItem.MAX_DAMAGE_RANGE;

// In-world projectile
public class BlunderbussShot extends Projectile {

    private static final Logger logger = LogUtils.getLogger();

    private Vec3 start;

    public BlunderbussShot(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
//        logger.info("Shot created");
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//        System.out.println("\nTrace");
//        int len = 10;
//        if (stackTrace.length < 10)
//            len = stackTrace.length;
//        for (int i = 0; i < len; i++) {
//            System.out.println(stackTrace[i]);
//        }
//        System.out.println("\n");
    }


    public BlunderbussShot(EntityType<? extends Projectile> entityType, double x, double y, double z, Level level) {
        this(entityType, level);
        this.setPos(x, y, z);
        this.start = new Vec3(x, y, z);
    }

    public BlunderbussShot(EntityType<? extends Projectile> p_36717_, LivingEntity p_36718_, Level p_36719_) {
        this(p_36717_, p_36718_.getX(), p_36718_.getEyeY() - (double)0.1F, p_36718_.getZ(), p_36719_);
        this.setOwner(p_36718_);
//        if (p_36718_ instanceof Player) {
//            this.pickup = AbstractArrow.Pickup.ALLOWED;
//        }

    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        // All projectile movement logic seems to be in subclasses of Projectile
        super.tick();
        // Below from ThrowableProjectile
        Vec3 pos = new Vec3(getX(), getY(), getZ());
        double distance = 999999.0; //start.distanceTo(pos);
        if (!this.level().isClientSide) {
            distance = start.distanceTo(pos);
//            logger.info("Tick! at {} {} {}", getX(), getY(), getZ());
//            logger.info("Distance {}", distance);
        }
        if (distance > BlunderbussItem.DEFAULT_RANGE) { // TODO any client-side issues? May need to send start to client
//            logger.info("Exceeded range. Discarding");
            this.discard();
        }

        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
//        logger.info("Hit result {}", hitresult);
        boolean flag = false;
        if (hitresult.getType() == HitResult.Type.BLOCK) {
//            if (!this.level().isClientSide) logger.info("Hit block");
            BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
            BlockState blockstate = this.level().getBlockState(blockpos);
//            if (!this.level().isClientSide) logger.info("Block pos {}, state {}", blockpos, blockstate);
            if (blockstate.is(Blocks.NETHER_PORTAL)) {
                this.handleInsidePortal(blockpos);
                flag = true;
            } else if (blockstate.is(Blocks.END_GATEWAY)) {
                BlockEntity blockentity = this.level().getBlockEntity(blockpos);
                if (blockentity instanceof TheEndGatewayBlockEntity && TheEndGatewayBlockEntity.canEntityTeleport(this)) {
                    TheEndGatewayBlockEntity.teleportEntity(this.level(), blockpos, blockstate, this, (TheEndGatewayBlockEntity)blockentity);
                }

                flag = true;
            }
        }

        if (hitresult.getType() != HitResult.Type.MISS && !flag && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
//            if (!this.level().isClientSide) logger.info("Call onHit");
            this.onHit(hitresult);
        }

        this.checkInsideBlocks();
        Vec3 vec3 = this.getDeltaMovement();
        double d2 = this.getX() + vec3.x;
        double d0 = this.getY() + vec3.y;
        double d1 = this.getZ() + vec3.z;
        this.updateRotation();
        float f;
        if (this.isInWater()) {
            for(int i = 0; i < 4; ++i) {
                float f1 = 0.25F;
                this.level().addParticle(ParticleTypes.BUBBLE, d2 - vec3.x * 0.25D, d0 - vec3.y * 0.25D, d1 - vec3.z * 0.25D, vec3.x, vec3.y, vec3.z);
            }

            f = 0.8F;
        } else {
            f = 0.99F;
        }
//        if (!this.level().isClientSide) logger.info("f {}", f);

        this.setDeltaMovement(vec3.scale((double)f));
        if (!this.isNoGravity()) {
            Vec3 vec31 = this.getDeltaMovement();
            this.setDeltaMovement(vec31.x, vec31.y - (double)this.getGravity(), vec31.z);
        }

//        if (!this.level().isClientSide) logger.info("Delta move {}", this.getDeltaMovement());
        this.setPos(d2, d0, d1);
    }

    protected void onHit(HitResult p_37406_) {
        super.onHit(p_37406_);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte)3);
//            logger.info("Discarding");
            this.discard();
        }
    }

    protected void onHitEntity(EntityHitResult p_37404_) {
        super.onHitEntity(p_37404_);
        Entity entity = p_37404_.getEntity();
        Vec3 pos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        double distance = start.distanceTo(pos);
//        logger.info("onHitEntity at {}, distance {}", pos, distance);
        if (distance <= DEFAULT_RANGE) {
            float dmg = distance <= MAX_DAMAGE_RANGE ? MAX_DAMAGE : interpolateDamage((float) distance); //entity instanceof Blaze ? 3 : 0;
//            logger.info("onHitEntity at {}, distance {}, dmg {}", pos, start.distanceTo(pos), dmg);
//        if (entity instanceof LivingEntity) {
//            LivingEntity living = (LivingEntity) entity;
//            logger.info("Hurting for {}, health {}", i, living.getHealth());
//        }

            entity.hurt(this.damageSources().thrown(this, this.getOwner()), dmg);
        }
//        else {
//            logger.info("No damage!");
//        }
//        if (entity instanceof LivingEntity) {
//            LivingEntity living = (LivingEntity) entity;
//            logger.info("Hurt for {}, health {}", i, living.getHealth());
//        }
    }

    private float interpolateDamage(float distance) {
        float aboveMaxDmgRange = distance - (float) MAX_DAMAGE_RANGE;
        float fractionToMaxRange = aboveMaxDmgRange / ((float) BlunderbussItem.DEFAULT_RANGE - MAX_DAMAGE_RANGE);
        float damage = Math.max(0.0f, MAX_DAMAGE * (1.0f - fractionToMaxRange));
//        logger.info("aboveMaxDmgRange {}, fractionToMaxRange {}, damage {}",
//                aboveMaxDmgRange, fractionToMaxRange, damage);
        return damage;
    }

    protected float getGravity() {
        return 0.03F;
    }

}
