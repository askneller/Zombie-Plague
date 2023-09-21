package net.askneller.zombieplague.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import static net.askneller.zombieplague.ZombiePlague.MODID;

public class ModTags {

    public static class Blocks {

        public static final TagKey<Block> LIGHT_SOURCE = tag("light_source");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(MODID, name));
        }
    }

    public static class Items {

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(new ResourceLocation(MODID, name));
        }
    }
}
