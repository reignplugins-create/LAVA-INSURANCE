package com.lavainsurance;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class LavaInsuranceCharmItem extends Item {
    public LavaInsuranceCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // enchanted-looking shimmer so it's easy to spot in an inventory
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.lavainsurance.lava_insurance_charm.tooltip1"));
        tooltip.add(Component.translatable("item.lavainsurance.lava_insurance_charm.tooltip2"));
        tooltip.add(Component.translatable("item.lavainsurance.lava_insurance_charm.tooltip3"));
    }
}
