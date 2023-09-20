package net.askneller.zombieplague.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.entity.ModEntities;
import net.askneller.zombieplague.sound.ModSounds;
import net.askneller.zombieplague.world.entity.projectile.BlunderbussShot;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static net.askneller.zombieplague.ZombiePlague.BLUNDERBUSS_SHOT;

public class BlunderbussItem extends ProjectileWeaponItem implements Vanishable {

    private static final Logger logger = LogUtils.getLogger();
    public static final Predicate<ItemStack> SHOT = (p_43017_) -> {
        return p_43017_.is(BLUNDERBUSS_SHOT.get());
    };

    // Particle constants
    private static final int NUM_PARTICLES = 20;
    private static final double Y_OFFSET = 1.5;
    private static final double LOOK_SCALE_FACTOR = 0.2;
    private static final float START_OFFSET_SCALE_FACTOR = 0.4f;

    public static final int DEFAULT_RANGE = 9; // Used for Mob attacks
    public static final int MAX_DAMAGE_RANGE = 5;
    public static final float MAX_DAMAGE = 30.0f;

    public BlunderbussItem(Properties p_43009_) {
        super(p_43009_);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
//        return ARROW_ONLY;
         return SHOT;
    }

    @Override
    public int getDefaultProjectileRange() {
        return DEFAULT_RANGE;
    }

    public void notifyLoaded(Player player) {
        Level level = player.level();
        if (level.isClientSide && level.getGameTime() % 5 == 0)
            player.sendSystemMessage(Component.literal("Loaded!")
                    .withStyle(ChatFormatting.RED));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
//        if (!level.isClientSide) logger.info("Use blunderbuss");
        ItemStack itemstack = player.getItemInHand(hand);
        if (isCharged(itemstack)) {
//            if (!level.isClientSide) logger.info("Charged");
            performShooting(level, player, hand, itemstack, getShootingPower(itemstack), 1.0F);
            setCharged(itemstack, false);
            return InteractionResultHolder.consume(itemstack);
        }
        else if (!player.getProjectile(itemstack).isEmpty()) {
            if (!isCharged(itemstack)) {
//                if (!level.isClientSide) logger.info("Not charged");
//                this.startSoundPlayed = false;
//                this.midLoadSoundPlayed = false;
                player.startUsingItem(hand);
                // TODO change to graphic-based notification like crossbow
                if (level.isClientSide())
                    player.sendSystemMessage(Component.literal("Loading blunderbuss")
                            .withStyle(ChatFormatting.AQUA));
            }

            return InteractionResultHolder.consume(itemstack);
        } else {
            return InteractionResultHolder.fail(itemstack);
        }
//        return InteractionResultHolder.fail(itemstack);
    }

    public int getUseDuration(ItemStack p_40938_) {
        return getChargeDuration(p_40938_) + 3;
    }

    public static int getChargeDuration(ItemStack p_40940_) {
//        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, p_40940_);
        return 25; //i == 0 ? 25 : 25 - 5 * i;
    }

//    public static boolean isCharged(ItemStack p_40933_) {
//        return true;
//    }

    public static boolean isCharged(ItemStack p_40933_) {
        CompoundTag compoundtag = p_40933_.getTag();
        return compoundtag != null && compoundtag.getBoolean("Charged");
    }

    public static void setCharged(ItemStack p_40885_, boolean p_40886_) {
        CompoundTag compoundtag = p_40885_.getOrCreateTag();
        compoundtag.putBoolean("Charged", p_40886_);
    }

    public void releaseUsing(ItemStack p_40875_, Level level, LivingEntity livingEntity, int p_40878_) {
//        if (livingEntity instanceof Player) {
//            if (!level.isClientSide) logger.info("releaseUsing: {}, duration {}", p_40875_.getItem(), p_40878_);
//        }
        int i = this.getUseDuration(p_40875_) - p_40878_;
        float f = getPowerForTime(i, p_40875_);
//        if (livingEntity instanceof Player) {
//            if (!level.isClientSide) logger.info("power {}", f);
//        }
        if (f >= 1.0F && !isCharged(p_40875_) && tryLoadProjectiles(livingEntity, p_40875_)) {
            setCharged(p_40875_, true);
            SoundSource soundsource = livingEntity instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            level.playSound((Player)null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.CROSSBOW_LOADING_END, soundsource, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }

    }

    private static boolean tryLoadProjectiles(LivingEntity entity, ItemStack weapon) {
//        if (!entity.level().isClientSide()) logger.info("tryLoadProjectiles: {}", weapon);
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, weapon);
        int j = i == 0 ? 1 : 3;
        boolean flag = entity instanceof Player && ((Player)entity).getAbilities().instabuild;
//        if (!entity.level().isClientSide()) logger.info("tryLoadProjectiles - playerInstabuild: {}", flag);
        ItemStack itemstack = entity.getProjectile(weapon); // get the stack of projectiles
//        if (!entity.level().isClientSide()) logger.info("livingEntity.getProjectile returned: {} ({})", itemstack, itemstack.getItem().getClass());
        ItemStack itemstack1 = itemstack.copy();

        for(int k = 0; k < j; ++k) {
            if (k > 0) {
                itemstack = itemstack1.copy();
            }

            if (itemstack.isEmpty() && flag) {
//                itemstack = new ItemStack(Items.ARROW); // ArrowItem.java is what appears in inventory, Arrow.java is what spawns in world
                // As evidence Arrow.getPickupItem returns ArrowItem
                itemstack = new ItemStack(BLUNDERBUSS_SHOT.get()); // todo
                itemstack1 = itemstack.copy();
            }

            if (!loadProjectile(entity, weapon, itemstack, k > 0, flag)) {
                return false;
            }
        }

        return true;
    }

    private static boolean loadProjectile(LivingEntity entity, ItemStack weapon, ItemStack ammo, boolean p_40866_, boolean playerInstabuild) {
//        if (!entity.level().isClientSide()) logger.info("loadProjectile - args: 2nd {}, 3rd {}, 4th {}, 5th {}", weapon, ammo, p_40866_, playerInstabuild);
        if (ammo.isEmpty()) {
            return false;
        } else {
//            if (!entity.level().isClientSide()) logger.info("3rd arg class: {}", ammo.getItem().getClass());
            boolean flag = playerInstabuild && ammo.getItem() instanceof ArrowItem;
//            if (!entity.level().isClientSide()) logger.info("loadProjectile - flag (playerInstabuild && is ArrowItem): {}", flag);
            ItemStack itemstack;
            if (!flag && !playerInstabuild && !p_40866_) { // I think this might represent if the ammo came from a stack of them
                itemstack = ammo.split(1); // remove one item from the ammo...
//                if (!entity.level().isClientSide()) logger.info("This {}", itemstack);
                if (ammo.isEmpty() && entity instanceof Player) { // ... and remove it if it is empty
                    ((Player)entity).getInventory().removeItem(ammo);
                }
            } else { // ... this might be if the player was "given" the ammo (e.g. in creative)
                itemstack = ammo.copy();
//                if (!entity.level().isClientSide()) logger.info("Other {}", itemstack);
            }

            addChargedProjectile(weapon, itemstack);
            return true;
        }
    }

    private static void addChargedProjectile(ItemStack weapon, ItemStack ammo) {
        CompoundTag compoundtag = weapon.getOrCreateTag();
        ListTag listtag;
        if (compoundtag.contains("ChargedProjectiles", 9)) {
            listtag = compoundtag.getList("ChargedProjectiles", 10);
        } else {
            listtag = new ListTag();
        }

        CompoundTag compoundtag1 = new CompoundTag();
        ammo.save(compoundtag1);
        listtag.add(compoundtag1);
        compoundtag.put("ChargedProjectiles", listtag);
//        logger.info("addChargedProjectile weapon: {} - {}", weapon, weapon.getTag());
//        logger.info("addChargedProjectile ammo: {} - {}", ammo, ammo.getTag());
    }

    private static float getPowerForTime(int p_40854_, ItemStack p_40855_) {
        float f = (float)p_40854_ / (float)getChargeDuration(p_40855_);
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    // I think this means that once the item has been "released" from using, it still can be "used", like a crossbow
    // This returning true allows the use duration to go negative, so the Finish UseItemEvent is not called,
    // Stop is instead when the player releases the mouse button
    // TODO investigate why Finish is not called on this or crossbow, event only seems to be triggered from LivingEntity.completeUsingItem
    // TODO which is called from LivingEntity.updateUsingItem. updateUsingItem checks !useOnRelease before calling completeUsingItem
    public boolean useOnRelease(ItemStack p_150801_) {
        return p_150801_.is(this);
    }

    public static void performShooting(Level level, LivingEntity entity, InteractionHand hand,
                                       ItemStack itemStack, float power, float p_40893_) {
//        if (!level.isClientSide) logger.info("performShooting");
        if (entity instanceof Player player &&
                net.minecraftforge.event.ForgeEventFactory.onArrowLoose(itemStack, entity.level(), player, 1, true) < 0) {
//            if (!level.isClientSide) logger.info("onArrowLoose here");
            return;
        }

        List<ItemStack> list = getChargedProjectiles(itemStack);
//        if (!level.isClientSide) logger.info("chargedProjectiles: {}", list.size());
        float[] afloat = getShotPitches(entity.getRandom());
//        if (!level.isClientSide) logger.info("getShotPitches: {}", afloat[0]);

        for(int i = 0; i < list.size(); ++i) {
            ItemStack ammo = list.get(i);
            boolean flag = entity instanceof Player && ((Player)entity).getAbilities().instabuild;
            if (!ammo.isEmpty()) {
                if (i == 0) {
                    shootProjectile(level, entity, hand, itemStack, ammo, afloat[i], flag, power, p_40893_, 0.0F);
                } else if (i == 1) {
                    shootProjectile(level, entity, hand, itemStack, ammo, afloat[i], flag, power, p_40893_, -10.0F);
                } else if (i == 2) {
                    shootProjectile(level, entity, hand, itemStack, ammo, afloat[i], flag, power, p_40893_, 10.0F);
                }
            }
        }

        onCrossbowShot(level, entity, itemStack);
    }

    private static float getShootingPower(ItemStack p_40946_) {
        return 6.0f;
    }

    private static void shootProjectile(Level level, LivingEntity livingEntity, InteractionHand hand,
                                        ItemStack weapon, ItemStack ammo, float p_40900_, boolean p_40901_,
                                        float p_40902_, float p_40903_, float p_40904_) {
//        if (!level.isClientSide) logger.info("shootProjectile, weapon {}, ammo {}", weapon, ammo);
//        if (!level.isClientSide) logger.info("float {}, boolean (instabuild?) {}, float (power) {}, float {}, float (angle?) {}",
//                p_40900_, p_40901_, p_40902_, p_40903_, p_40904_);
        if (!level.isClientSide) {
            boolean flag = ammo.is(Items.FIREWORK_ROCKET);
            Projectile projectile;
            if (flag) {
                projectile = new FireworkRocketEntity(level, ammo, livingEntity, livingEntity.getX(), livingEntity.getEyeY() - (double)0.15F, livingEntity.getZ(), true);
            } else {
                projectile = getArrow(level, livingEntity, weapon, ammo);
//                if (p_40901_ || p_40904_ != 0.0F) {
//                    ((AbstractArrow)projectile).pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
//                }
            }

            if (livingEntity instanceof CrossbowAttackMob) {
                CrossbowAttackMob crossbowattackmob = (CrossbowAttackMob)livingEntity;
                crossbowattackmob.shootCrossbowProjectile(crossbowattackmob.getTarget(), weapon, projectile, p_40904_);
            } else {
                Vec3 vec31 = livingEntity.getUpVector(1.0F);
                Quaternionf quaternionf = (new Quaternionf()).setAngleAxis((double)(p_40904_ * ((float)Math.PI / 180F)), vec31.x, vec31.y, vec31.z);
                Vec3 vec3 = livingEntity.getViewVector(1.0F);
                Vector3f vector3f = vec3.toVector3f().rotate(quaternionf);
                projectile.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), p_40902_, p_40903_);
            }

            weapon.hurtAndBreak(flag ? 3 : 1, livingEntity, (p_40858_) -> {
                p_40858_.broadcastBreakEvent(hand);
            });
//            logger.info("Launched from {}, player {} {} {}", projectile.blockPosition(), livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
            level.addFreshEntity(projectile);
//            level.playSound((Player)null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, p_40900_);
            level.playSound(
                    (Player)null,
                    livingEntity.getX(),
                    livingEntity.getY(),
                    livingEntity.getZ(),
                    ModSounds.BLUNDERBUSS_SHOOT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    p_40900_);
        }

        makeParticles(level, livingEntity.getLookAngle(), livingEntity.position());
    }

    // From CampfireBlockEntity
    public static void makeParticles(Level level, Vec3 look, Vec3 position) {
        RandomSource randomsource = level.getRandom();
//        logger.info("Add particles at position {}", position);
//        logger.info("Looking look {}", look);
        Vec3 baseStartPos = new Vec3(
                position.x() + look.x,
                position.y() + Y_OFFSET + look.y,
                position.z() + look.z);
//        logger.info("baseStartPos {}", baseStartPos);
        Vec3 scaledLook = look.scale(LOOK_SCALE_FACTOR);
        for (int i = 0; i < NUM_PARTICLES; i++) {
            double rx = randomsource.nextDouble() * 2.0 - 1.0;
            double ry = randomsource.nextDouble() * 2.0 - 1.0;
            double rz = randomsource.nextDouble() * 2.0 - 1.0;
//            logger.info("\n{}\n{}\n{}", rx, ry, rz);
            Vec3 offsetStart = baseStartPos.offsetRandom(randomsource, START_OFFSET_SCALE_FACTOR);
            level.addParticle(ParticleTypes.LARGE_SMOKE, false,
                    // position
                    offsetStart.x,
                    offsetStart.y,
                    offsetStart.z,
                    // impulse
                    scaledLook.x + rx * 0.1,
                    scaledLook.y + ry * 0.1,
                    scaledLook.z + rz * 0.1);
        }
    }

    private static Projectile getArrow(Level p_40915_, LivingEntity p_40916_, ItemStack p_40917_, ItemStack p_40918_) {
//        ArrowItem arrowitem = (ArrowItem)(p_40918_.getItem() instanceof ArrowItem ? p_40918_.getItem() : Items.ARROW);
//        AbstractArrow abstractarrow = arrowitem.createArrow(p_40915_, p_40918_, p_40916_);
//        if (p_40916_ instanceof Player) {
//            abstractarrow.setCritArrow(true);
//        }
//
//        abstractarrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
//        abstractarrow.setShotFromCrossbow(true);
//        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, p_40917_);
//        if (i > 0) {
//            abstractarrow.setPierceLevel((byte)i);
//        }
//
//        return abstractarrow;
        BlunderbussShot blunderbussShot = new BlunderbussShot(ModEntities.BLUNDERBUSS_SHOT, p_40916_, p_40915_);
        blunderbussShot.setOwner(p_40916_);
        return blunderbussShot;
    }

    private static float[] getShotPitches(RandomSource p_220024_) {
//        logger.info("getShotPitches");
        boolean flag = p_220024_.nextBoolean();
        return new float[]{1.0F, getRandomShotPitch(flag, p_220024_), getRandomShotPitch(!flag, p_220024_)};
    }

    private static float getRandomShotPitch(boolean p_220026_, RandomSource p_220027_) {
        float f = p_220026_ ? 0.63F : 0.43F;
        return 1.0F / (p_220027_.nextFloat() * 0.5F + 1.8F) + f;
    }

    private static void onCrossbowShot(Level p_40906_, LivingEntity p_40907_, ItemStack p_40908_) {
        if (p_40907_ instanceof ServerPlayer serverplayer) {
            if (!p_40906_.isClientSide) {
                CriteriaTriggers.SHOT_CROSSBOW.trigger(serverplayer, p_40908_);
            }

            serverplayer.awardStat(Stats.ITEM_USED.get(p_40908_.getItem()));
        }

//        clearChargedProjectiles(p_40908_);
    }

    private static List<ItemStack> getChargedProjectiles(ItemStack itemStack) {
//        logger.info("getChargedProjectiles: {}, {}", itemStack.getItem(), itemStack.getTag());
        List<ItemStack> list = Lists.newArrayList();
        CompoundTag compoundtag = itemStack.getTag();
        if (compoundtag != null && compoundtag.contains("ChargedProjectiles", 9)) {
            ListTag listtag = compoundtag.getList("ChargedProjectiles", 10);
            if (listtag != null) {
                for(int i = 0; i < listtag.size(); ++i) {
                    CompoundTag compoundtag1 = listtag.getCompound(i);
                    list.add(ItemStack.of(compoundtag1));
                }
            }
        }

        ItemStack stack = new ItemStack(Items.ARROW); // todo change
        return Collections.singletonList(stack);
//        return list;
    }

}
