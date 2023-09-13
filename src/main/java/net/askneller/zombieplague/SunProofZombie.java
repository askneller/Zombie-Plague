package net.askneller.zombieplague;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class SunProofZombie extends Zombie {

    private static final Logger LOGGER = LogUtils.getLogger();

    public SunProofZombie(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

//    public static AttributeSupplier.Builder createAttributes() {
//        return Monster.createMonsterAttributes()
//                .add(Attributes.FOLLOW_RANGE, 35.0D)
//                .add(Attributes.MOVEMENT_SPEED, (double)0.23F)
//                .add(Attributes.ATTACK_DAMAGE, 3.0D)
//                .add(Attributes.ARMOR, 2.0D)
//                // Don't call reinforcements
//                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE)
//                ;
//    }

    @Override
    protected void randomizeReinforcementsChance() {
        this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0D);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }
}
