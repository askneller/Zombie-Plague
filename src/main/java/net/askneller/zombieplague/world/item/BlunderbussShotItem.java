package net.askneller.zombieplague.world.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// In-inventory / hand projectile
public class BlunderbussShotItem extends Item {

    public BlunderbussShotItem(Properties p_41383_) {
        super(p_41383_);
    }

    public AbstractArrow createArrow(Level p_40513_, ItemStack p_40514_, LivingEntity p_40515_) {
        Arrow arrow = new Arrow(p_40513_, p_40515_);
        arrow.setEffectsFromItem(p_40514_);
        return arrow;
    }

    public boolean isInfinite(ItemStack stack, ItemStack bow, net.minecraft.world.entity.player.Player player) {
        return false;
    }
}
