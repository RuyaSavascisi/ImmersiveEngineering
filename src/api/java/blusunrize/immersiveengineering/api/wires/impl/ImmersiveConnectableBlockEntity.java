/*
 * BluSunrize
 * Copyright (c) 2020
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 *
 */

package blusunrize.immersiveengineering.api.wires.impl;

import blusunrize.immersiveengineering.api.IEProperties.ConnectionModelData;
import blusunrize.immersiveengineering.api.IEProperties.Model;
import blusunrize.immersiveengineering.api.utils.client.CombinedModelData;
import blusunrize.immersiveengineering.api.utils.client.SinglePropertyModelData;
import blusunrize.immersiveengineering.api.wires.ConnectorBlockEntityHelper;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.wires.LocalWireNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;

public abstract class ImmersiveConnectableBlockEntity extends BlockEntity implements IImmersiveConnectable
{
	protected GlobalWireNetwork globalNet;

	public ImmersiveConnectableBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
	{
		super(tileEntityTypeIn, pos, state);
	}

	@Override
	public void setLevel(Level world)
	{
		super.setLevel(world);
		globalNet = GlobalWireNetwork.getNetwork(world);
	}

	@Nonnull
	@Override
	public IModelData getModelData()
	{
		ConnectionModelData state = ConnectorBlockEntityHelper.genConnBlockState(level, this);
		return CombinedModelData.combine(
				new SinglePropertyModelData<>(state, Model.CONNECTIONS), super.getModelData()
		);
	}

	private boolean isUnloaded = false;

	@Override
	public void onChunkUnloaded()
	{
		super.onChunkUnloaded();
		ConnectorBlockEntityHelper.onChunkUnload(globalNet, this);
		isUnloaded = true;
	}

	@Override
	public void onLoad()
	{
		super.onLoad();
		ConnectorBlockEntityHelper.onChunkLoad(this, level);
		isUnloaded = false;
	}

	@Override
	public void setRemoved()
	{
		super.setRemoved();
		if(!isUnloaded)
			ConnectorBlockEntityHelper.remove(level, this);
	}

	private final Int2ObjectMap<LocalWireNetwork> cachedLocalNets = new Int2ObjectArrayMap<>();

	protected LocalWireNetwork getLocalNet(int cpIndex)
	{
		return ConnectorBlockEntityHelper.getLocalNetWithCache(globalNet, getBlockPos(), cpIndex, cachedLocalNets);
	}
}
