package com.jaskarth.ecotones.world.items;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import com.jaskarth.ecotones.util.DynamicEatTimeItem;

public class JamItem extends AliasedBlockItem implements DynamicEatTimeItem {
    public JamItem(Block block, Settings settings) {
        super(block, settings);
    }

    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity livingEntity) {
        super.finishUsing(stack, world, livingEntity);

        if (stack.isEmpty()) {
            return new ItemStack(EcotonesItems.JAR);
        } else {
            if (livingEntity instanceof PlayerEntity && !((PlayerEntity)livingEntity).getAbilities().creativeMode) {
                ItemStack bottle = new ItemStack(EcotonesItems.JAR);
                PlayerEntity playerEntity = (PlayerEntity)livingEntity;
                if (!playerEntity.getInventory().insertStack(bottle)) {
                    playerEntity.dropItem(bottle, false);
                }
            }

            return stack;
        }
    }

    public int getMaxUseTime(ItemStack stack) {
        return 80;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    public SoundEvent getDrinkSound() {
        return SoundEvents.ITEM_HONEY_BOTTLE_DRINK;
    }

    public SoundEvent getEatSound() {
        return SoundEvents.ITEM_HONEY_BOTTLE_DRINK;
    }

    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
}
