/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.fluids;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.items.PotionBucketItem;
import blusunrize.immersiveengineering.common.register.IEFluids;
import blusunrize.immersiveengineering.common.register.IEItems.Misc;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static blusunrize.immersiveengineering.ImmersiveEngineering.rl;

public class PotionFluid extends Fluid
{
	public static FluidStack getFluidStackForType(Optional<Holder<Potion>> type, int amount)
	{
		if(type.isEmpty()||type.get().is(Potions.WATER))
			return new FluidStack(Fluids.WATER, amount);
		FluidStack stack = new FluidStack(IEFluids.POTION.get(), amount);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(type.get()));
		return stack;
	}

	public static Potion getType(FluidStack stack)
	{
		return stack.getOrDefault(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER))
				.potion()
				.orElse(Potions.WATER)
				.value();
	}

	@Nonnull
	@Override
	public Item getBucket()
	{
		return Misc.POTION_BUCKET.get();
	}

	@Override
	protected boolean canBeReplacedWith(@Nonnull FluidState fluidState, @Nonnull BlockGetter blockReader,
								  @Nonnull BlockPos pos, @Nonnull Fluid fluid, @Nonnull Direction direction)
	{
		return true;
	}

	@Nonnull
	@Override
	protected Vec3 getFlow(@Nonnull BlockGetter blockReader, @Nonnull BlockPos pos, @Nonnull FluidState fluidState)
	{
		return Vec3.ZERO;
	}

	@Override
	public int getTickDelay(LevelReader p_205569_1_)
	{
		return 0;
	}

	@Override
	protected float getExplosionResistance()
	{
		return 0;
	}

	@Override
	public float getHeight(@Nonnull FluidState p_215662_1_, @Nonnull BlockGetter p_215662_2_, @Nonnull BlockPos p_215662_3_)
	{
		return 0;
	}

	@Override
	public float getOwnHeight(@Nonnull FluidState p_223407_1_)
	{
		return 0;
	}

	@Nonnull
	@Override
	protected BlockState createLegacyBlock(@Nonnull FluidState state)
	{
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public boolean isSource(@Nonnull FluidState state)
	{
		return true;
	}

	@Override
	public int getAmount(@Nonnull FluidState state)
	{
		return 0;
	}

	@Nonnull
	@Override
	public VoxelShape getShape(@Nonnull FluidState p_215664_1_, @Nonnull BlockGetter p_215664_2_, @Nonnull BlockPos p_215664_3_)
	{
		return Shapes.empty();
	}

	@Nonnull
	@Override
	public FluidType getFluidType()
	{
		return IEFluids.POTION_TYPE.value();
	}

	public void addInformation(FluidStack fluidStack, Consumer<Component> tooltip)
	{
		if(fluidStack!=null&&fluidStack.hasTag())
		{
			List<MobEffectInstance> effects = PotionUtils.getAllEffects(fluidStack.getTag());
			if(effects.isEmpty())
				tooltip.accept(Component.translatable("effect.none").withStyle(ChatFormatting.GRAY));
			else
			{
				for(MobEffectInstance instance : effects)
				{
					MutableComponent itextcomponent = Component.translatable(instance.getDescriptionId());
					MobEffect effect = instance.getEffect();
					if(instance.getAmplifier() > 0)
						itextcomponent.append(" ").append(Component.translatable("potion.potency."+instance.getAmplifier()));
					if(instance.getDuration() > 20)
						itextcomponent.append(" (").append(MobEffectUtil.formatDuration(instance, 1, 20)).append(")");

					tooltip.accept(itextcomponent.withStyle(effect.getCategory().getTooltipFormatting()));
				}
			}
			Potion potionType = PotionUtils.getPotion(fluidStack.getTag());
			if(potionType!=Potions.EMPTY)
			{
				String modID = BuiltInRegistries.POTION.getKey(potionType).getNamespace();
				tooltip.accept(Component.translatable(Lib.DESC_INFO+"potionMod", Utils.getModName(modID)).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
	}

	public static class PotionFluidType extends FluidType
	{
		private static final ResourceLocation TEXTURE_STILL = rl("block/fluid/potion_still");
		private static final ResourceLocation TEXTURE_FLOW = rl("block/fluid/potion_flow");

		public PotionFluidType()
		{
			super(Properties.create()
					.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
					.sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
		}

		@Override
		public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer)
		{
			consumer.accept(new IClientFluidTypeExtensions()
			{
				@Override
				public ResourceLocation getStillTexture()
				{
					return TEXTURE_STILL;
				}

				@Override
				public ResourceLocation getFlowingTexture()
				{
					return TEXTURE_FLOW;
				}

				@Override
				public int getTintColor(FluidStack stack)
				{
					if(stack==null||!stack.hasTag())
						return 0xff0000ff;
					return 0xff000000|PotionUtils.getColor(PotionUtils.getAllEffects(stack.getTag()));
				}
			});
		}

		@Override
		public Component getDescription(FluidStack stack)
		{
			if(stack==null||!stack.hasTag())
				return super.getDescription(stack);
			return Component.translatable(PotionUtils.getPotion(stack.getTag()).getName("item.minecraft.potion.effect."));
		}

		@Override
		public ItemStack getBucket(FluidStack stack)
		{
			return PotionBucketItem.forPotion(getType(stack));
		}
	}
}
