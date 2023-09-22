package net.askneller.zombieplague.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class LightSourceMarkerEntity extends Entity {

    private static final Logger logger = LogUtils.getLogger();

    private CompoundTag data = new CompoundTag();

    public LightSourceMarkerEntity(EntityType<?> p_147250_, Level p_147251_) {
        super(p_147250_, p_147251_);
        logger.info("Created");
    }

    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_20052_) {
        this.data = p_20052_.getCompound("data");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_20139_) {
        p_20139_.put("data", this.data.copy());
    }

}
