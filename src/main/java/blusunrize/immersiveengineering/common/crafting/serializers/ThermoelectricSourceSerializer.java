/*
 * BluSunrize
 * Copyright (c) 2021
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.crafting.serializers;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.energy.ThermoelectricSource;
import blusunrize.immersiveengineering.common.network.PacketUtils;
import blusunrize.immersiveengineering.common.register.IEBlocks.MetalDevices;
import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static blusunrize.immersiveengineering.api.crafting.builders.ThermoelectricSourceBuilder.*;

public class ThermoelectricSourceSerializer extends IERecipeSerializer<ThermoelectricSource>
{
	public static final Codec<ThermoelectricSource> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.INT.fieldOf(TEMPERATURE_KEY).forGetter(r -> r.temperature),
			TagKey.codec(Registries.BLOCK).optionalFieldOf(BLOCK_TAG_KEY).forGetter(r -> r.blocks.leftOptional()),
			maybeListOrSingle(ForgeRegistries.BLOCKS.getCodec(), SINGLE_BLOCK_KEY).forGetter(r -> r.blocks.rightOptional())
	).apply(inst, (temperature, tag, fixedBlocks) -> {
		Preconditions.checkState(tag.isPresent()!=fixedBlocks.isPresent());
		if(tag.isPresent())
			return new ThermoelectricSource(tag.get(), temperature);
		else
			return new ThermoelectricSource(fixedBlocks.get(), temperature);
	}));

	@Override
	public Codec<ThermoelectricSource> codec()
	{
		return CODEC;
	}

	@Override
	public ItemStack getIcon()
	{
		return new ItemStack(MetalDevices.THERMOELECTRIC_GEN);
	}

	@Nullable
	@Override
	public ThermoelectricSource fromNetwork(@Nonnull FriendlyByteBuf buffer)
	{
		List<Block> blocks = PacketUtils.readList(buffer, buf -> buf.readRegistryIdUnsafe(ForgeRegistries.BLOCKS));
		int temperature = buffer.readInt();
		return new ThermoelectricSource(blocks, temperature);
	}

	@Override
	public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull ThermoelectricSource recipe)
	{
		PacketUtils.writeList(
				buffer, recipe.getMatchingBlocks(), (b, buf) -> buf.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS, b)
		);
		buffer.writeInt(recipe.getTemperature());
	}
}
