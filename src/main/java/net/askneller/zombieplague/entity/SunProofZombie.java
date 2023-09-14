package net.askneller.zombieplague.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

public class SunProofZombie extends Zombie {

    private static final Logger logger = LogUtils.getLogger();
    private static final int STACK_PRINT = 5;

    public SunProofZombie(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    @Override
    protected void randomizeReinforcementsChance() {
        this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0D);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public float getWalkTargetValue(BlockPos blockPos, LevelReader levelReader) {
        /*
        float walkTargetValue = super.getWalkTargetValue(blockPos, levelReader);
        float animalWalkValue = levelReader.getBlockState(blockPos.below()).is(Blocks.GRASS_BLOCK) ? 10.0F
                : levelReader.getPathfindingCostFromLightLevels(blockPos);
        logger.info("Walk value at {} = {}, animal value = {}", blockPos, walkTargetValue, animalWalkValue);
        return animalWalkValue;
        */

        // From Animal.getWalkTargetValue
        return levelReader.getBlockState(blockPos.below()).is(Blocks.GRASS_BLOCK) ? 10.0F
                : levelReader.getPathfindingCostFromLightLevels(blockPos);
    }

    /*
    @Override
    public void remove(Entity.RemovalReason p_146834_) {
        logger.info("Removing zombie at {} ({})", this.position(), p_146834_);
        SpawnEvents.zombiesRemoved++;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < STACK_PRINT; i++) {
            StackTraceElement element = stackTrace[i];
            System.out.println(element.toString());
        }
        System.out.println("\n");
        super.remove(p_146834_);
    }
    */

    // A lot of Monsters are removed under various conditions, including being "far away"
    // Mob.removeWhenFarAway defaults to true
    @Override
    public boolean removeWhenFarAway(double p_27598_) {
        return false;
    }

}
