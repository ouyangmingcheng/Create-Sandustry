package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.CreateSandustry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 金之心：右键使用后立刻获得一颗没有持续时间限制的伤害吸收心。
 *
 * <p>通过永久 {@link Attributes#MAX_ABSORPTION} 修饰符提高吸收上限，并同步补充
 * 2 点吸收值（原版一颗心）。与伤害吸收药水不同，这部分吸收值不会因效果到期消失，
 * 只会在受到伤害时被正常消耗。</p>
 */
public final class GoldenHeartItem extends Item {
    public static final int HEARTS_PER_USE = 1;
    public static final double ABSORPTION_PER_USE = HEARTS_PER_USE * 2.0D;

    private static final ResourceLocation MAX_ABSORPTION_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "golden_hearts");

    public GoldenHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        AttributeInstance maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null) {
            return InteractionResultHolder.fail(stack);
        }

        AttributeModifier existing = maxAbsorption.getModifier(MAX_ABSORPTION_MODIFIER_ID);
        double bonus = (existing == null ? 0.0D : existing.amount()) + ABSORPTION_PER_USE;
        maxAbsorption.addOrReplacePermanentModifier(
                new AttributeModifier(MAX_ABSORPTION_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        player.setAbsorptionAmount(player.getAbsorptionAmount() + (float) ABSORPTION_PER_USE);

        stack.consume(HEARTS_PER_USE, player);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.createsandustry.golden_heart.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}