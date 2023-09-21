package net.askneller.zombieplague.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class LightSourceMarkerEntity extends Marker {

    private static final Logger logger = LogUtils.getLogger();

    public LightSourceMarkerEntity(EntityType<?> p_147250_, Level p_147251_) {
        super(p_147250_, p_147251_);
        logger.info("Created");
    }
}
