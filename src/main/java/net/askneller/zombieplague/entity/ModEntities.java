package net.askneller.zombieplague.entity;

import com.mojang.logging.LogUtils;
import net.askneller.zombieplague.world.entity.projectile.BlunderbussShot;
import net.askneller.zombieplague.world.entity.projectile.MusketBall;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;

import static net.askneller.zombieplague.ZombiePlague.MODID;

public class ModEntities {

    private static final Logger logger = LogUtils.getLogger();

    public static EntityType<? extends Zombie> SUN_PROOF_ZOMBIE;
    public static EntityType<BlunderbussShot> BLUNDERBUSS_SHOT;
    public static EntityType<MusketBall> MUSKET_BALL;
    public static EntityType<LightSourceMarkerEntity> LIGHT_SOURCE_MARKER;

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEntityEvents {

        // Register new zombie type
        @SubscribeEvent
        public static void setupEntities(RegisterEvent event) {
            if (event.getRegistryKey().equals(ForgeRegistries.Keys.ENTITY_TYPES)) {
                logger.info("Setting up mod entities");
                SUN_PROOF_ZOMBIE = build(event.getForgeRegistry(), "zombieplague:sunproofzombie",
                        EntityType.Builder.<Zombie>of(SunProofZombie::new, MobCategory.CREATURE) // MobCategory.MONSTER)
                                .sized(0.6F, 1.95F)
                                .clientTrackingRange(8)
                );

                BLUNDERBUSS_SHOT = build(event.getForgeRegistry(), "zombieplague:blunderbuss_shot",
                        EntityType.Builder.<BlunderbussShot>of(BlunderbussShot::new, MobCategory.MISC)
                                .sized(0.5F, 0.5F)
                                .clientTrackingRange(4)
                                .updateInterval(20)
                );

                MUSKET_BALL = build(event.getForgeRegistry(), "zombieplague:musket_ball",
                        EntityType.Builder.<MusketBall>of(MusketBall::new, MobCategory.MISC)
                                .sized(0.5F, 0.5F)
                                .clientTrackingRange(4)
                                .updateInterval(20)
                );

                LIGHT_SOURCE_MARKER = build(event.getForgeRegistry(), "zombieplague:light_source_marker",
                        EntityType.Builder.<LightSourceMarkerEntity>of(LightSourceMarkerEntity::new, MobCategory.MISC)
                                .sized(0, 0)
                                .clientTrackingRange(0)
                );
            }
        }

        private static <T extends Entity> EntityType<T> build(IForgeRegistry<EntityType> registry,
                                                              final String key,
                                                              final EntityType.Builder<T> builder) {
            EntityType<T> entity = builder.build(key);
            registry.register(new ResourceLocation(key), entity);
            return entity;
        }

        // Create zombie attributes
        @SubscribeEvent
        public static void createEntityAttribute(final EntityAttributeCreationEvent event) {
            logger.info("Creating default SunProofZombie attributes");
            event.put(SUN_PROOF_ZOMBIE, SunProofZombie.createAttributes().build());
        }

        // Create zombie spawn restrictions
        @SubscribeEvent
        public static void entitySpawnRestrictions(final SpawnPlacementRegisterEvent event) {
            logger.info("Creating default SunProofZombie spawn placements");
            event.register(SUN_PROOF_ZOMBIE, SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntityEvents::checkAnimalLikeSpawnRules, // Monster::checkMonsterSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }

        public static boolean checkAnimalLikeSpawnRules(EntityType<? extends Monster> p_218105_,
                                                        LevelAccessor p_218106_,
                                                        MobSpawnType p_218107_,
                                                        BlockPos p_218108_,
                                                        RandomSource p_218109_) {
            /*
            boolean b = p_218106_.getBlockState(p_218108_.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_218106_, p_218108_);
            logger.info("Spawn at {} = {}", p_218108_, b);
            return b;
            */
            return p_218106_.getBlockState(p_218108_.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) &&
                    isBrightEnoughToSpawn(p_218106_, p_218108_);
        }

        protected static boolean isBrightEnoughToSpawn(BlockAndTintGetter p_186210_, BlockPos p_186211_) {
            return p_186210_.getRawBrightness(p_186211_, 0) > 8;
        }

    }
}
