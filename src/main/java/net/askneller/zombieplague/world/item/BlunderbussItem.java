package net.askneller.zombieplague.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
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

public class BlunderbussItem extends ProjectileWeaponItem implements Vanishable {

    private static final Logger logger = LogUtils.getLogger();

    public static final int DEFAULT_RANGE = 15;

    public BlunderbussItem(Properties p_43009_) {
        super(p_43009_);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    @Override
    public int getDefaultProjectileRange() {
        return DEFAULT_RANGE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        logger.info("Use blunderbuss");
        ItemStack itemstack = player.getItemInHand(hand);
        if (isCharged(itemstack)) {
            performShooting(level, player, hand, itemstack, getShootingPower(itemstack), 1.0F);
//            setCharged(itemstack, false);
            return InteractionResultHolder.consume(itemstack);
        }
//        else if (!p_40921_.getProjectile(itemstack).isEmpty()) {
//            if (!isCharged(itemstack)) {
//                this.startSoundPlayed = false;
//                this.midLoadSoundPlayed = false;
//                p_40921_.startUsingItem(p_40922_);
//            }
//
//            return InteractionResultHolder.consume(itemstack);
//        } else {
//            return InteractionResultHolder.fail(itemstack);
//        }
        return InteractionResultHolder.fail(itemstack);
    }

    public static boolean isCharged(ItemStack p_40933_) {
        return true;
    }

    public static void performShooting(Level level, LivingEntity entity, InteractionHand hand,
                                       ItemStack itemStack, float power, float p_40893_) {
        logger.info("performShooting");
        if (entity instanceof Player player &&
                net.minecraftforge.event.ForgeEventFactory.onArrowLoose(itemStack, entity.level(), player, 1, true) < 0) {
            logger.info("onArrowLoose here");
            return;
        }

        List<ItemStack> list = getChargedProjectiles(itemStack);
        logger.info("chargedProjectiles: {}", list.size());
        float[] afloat = getShotPitches(entity.getRandom());
        logger.info("getShotPitches: {}", afloat[0]);

        for(int i = 0; i < list.size(); ++i) {
            ItemStack itemstack = list.get(i);
            boolean flag = entity instanceof Player && ((Player)entity).getAbilities().instabuild;
            if (!itemstack.isEmpty()) {
                if (i == 0) {
                    shootProjectile(level, entity, hand, itemStack, itemstack, afloat[i], flag, power, p_40893_, 0.0F);
                } else if (i == 1) {
                    shootProjectile(level, entity, hand, itemStack, itemstack, afloat[i], flag, power, p_40893_, -10.0F);
                } else if (i == 2) {
                    shootProjectile(level, entity, hand, itemStack, itemstack, afloat[i], flag, power, p_40893_, 10.0F);
                }
            }
        }

        onCrossbowShot(level, entity, itemStack);
    }

    private static float getShootingPower(ItemStack p_40946_) {
        return 6.0f;
    }

    private static void shootProjectile(Level p_40895_, LivingEntity p_40896_, InteractionHand p_40897_,
                                        ItemStack p_40898_, ItemStack p_40899_, float p_40900_, boolean p_40901_,
                                        float p_40902_, float p_40903_, float p_40904_) {
        logger.info("shootProjectile");
        if (!p_40895_.isClientSide) {
            boolean flag = p_40899_.is(Items.FIREWORK_ROCKET);
            Projectile projectile;
            if (flag) {
                projectile = new FireworkRocketEntity(p_40895_, p_40899_, p_40896_, p_40896_.getX(), p_40896_.getEyeY() - (double)0.15F, p_40896_.getZ(), true);
            } else {
                projectile = getArrow(p_40895_, p_40896_, p_40898_, p_40899_);
                if (p_40901_ || p_40904_ != 0.0F) {
                    ((AbstractArrow)projectile).pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
            }

            if (p_40896_ instanceof CrossbowAttackMob) {
                CrossbowAttackMob crossbowattackmob = (CrossbowAttackMob)p_40896_;
                crossbowattackmob.shootCrossbowProjectile(crossbowattackmob.getTarget(), p_40898_, projectile, p_40904_);
            } else {
                Vec3 vec31 = p_40896_.getUpVector(1.0F);
                Quaternionf quaternionf = (new Quaternionf()).setAngleAxis((double)(p_40904_ * ((float)Math.PI / 180F)), vec31.x, vec31.y, vec31.z);
                Vec3 vec3 = p_40896_.getViewVector(1.0F);
                Vector3f vector3f = vec3.toVector3f().rotate(quaternionf);
                projectile.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), p_40902_, p_40903_);
            }

            p_40898_.hurtAndBreak(flag ? 3 : 1, p_40896_, (p_40858_) -> {
                p_40858_.broadcastBreakEvent(p_40897_);
            });
            p_40895_.addFreshEntity(projectile);
            p_40895_.playSound((Player)null, p_40896_.getX(), p_40896_.getY(), p_40896_.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, p_40900_);
        }
    }

    private static AbstractArrow getArrow(Level p_40915_, LivingEntity p_40916_, ItemStack p_40917_, ItemStack p_40918_) {
        ArrowItem arrowitem = (ArrowItem)(p_40918_.getItem() instanceof ArrowItem ? p_40918_.getItem() : Items.ARROW);
        AbstractArrow abstractarrow = arrowitem.createArrow(p_40915_, p_40918_, p_40916_);
        if (p_40916_ instanceof Player) {
            abstractarrow.setCritArrow(true);
        }

        abstractarrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        abstractarrow.setShotFromCrossbow(true);
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, p_40917_);
        if (i > 0) {
            abstractarrow.setPierceLevel((byte)i);
        }

        return abstractarrow;
    }

    private static float[] getShotPitches(RandomSource p_220024_) {
        logger.info("getShotPitches");
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
        logger.info("getChargedProjectiles: {}, {}", itemStack.getItem(), itemStack.getTag());
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

        ItemStack stack = new ItemStack(Items.ARROW);
        return Collections.singletonList(stack);
//        return list;
    }

}
