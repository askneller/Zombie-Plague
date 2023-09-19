package net.askneller.zombieplague.client.renderer.entity;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.world.entity.projectile.MusketBall;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class MusketBallRenderer<T extends MusketBall> extends EntityRenderer<T> {

    private static final Logger logger = LogUtils.getLogger();

    public MusketBallRenderer(EntityRendererProvider.Context p_174008_) {
        super(p_174008_);
//        logger.info("created");
    }

    @Override
    public ResourceLocation getTextureLocation(T p_114482_) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public boolean shouldRender(T p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
//        logger.info("shouldRender");
        return false;
    }
}
