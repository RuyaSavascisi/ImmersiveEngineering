/*
 * BluSunrize
 * Copyright (c) 2023
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks.process;

import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.tool.MachineInterfaceHandler;
import blusunrize.immersiveengineering.api.tool.MachineInterfaceHandler.CheckOption;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext.ProcessContextInMachine;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext.ProcessContextInWorld;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntToDoubleFunction;

public class MultiblockProcessor<R extends MultiblockRecipe, CTX extends ProcessContext<R>>
{
	private final List<MultiblockProcess<R, CTX>> processQueue = new ArrayList<>();
	private final int maxQueueLength;
	// Input is the
	private final IntToDoubleFunction minDelayAfter;
	private final int maxProcessPerTick;
	private final Runnable markDirty;
	private final Runnable onQueueChange;
	private final BiFunction<Level, ResourceLocation, @Nullable R> getRecipeFromID;

	public MultiblockProcessor(
			int maxQueueLength,
			float minDelayAfter,
			int maxProcessPerTick,
			Runnable markDirty,
			BiFunction<Level, ResourceLocation, @Nullable R> getRecipeFromID
	)
	{
		this(maxQueueLength, $ -> minDelayAfter, maxProcessPerTick, markDirty, () -> {
		}, getRecipeFromID);
	}

	public MultiblockProcessor(
			int maxQueueLength,
			IntToDoubleFunction minDelayAfter,
			int maxProcessPerTick,
			Runnable markDirty,
			Runnable onQueueChange,
			BiFunction<Level, ResourceLocation, @Nullable R> getRecipeFromID
	)
	{
		this.maxQueueLength = maxQueueLength;
		this.minDelayAfter = minDelayAfter;
		this.maxProcessPerTick = maxProcessPerTick;
		this.markDirty = markDirty;
		this.onQueueChange = onQueueChange;
		this.getRecipeFromID = getRecipeFromID;
	}

	public boolean tickServer(CTX ctx, IMultiblockLevel level, boolean canWork)
	{
		ctx.getEnergy().updateAverage();
		if(!canWork)
			return false;

		int i = 0;
		Iterator<MultiblockProcess<R, CTX>> processIterator = processQueue.iterator();
		boolean tickedAny = false;
		while(processIterator.hasNext()&&i++ < maxProcessPerTick)
		{
			MultiblockProcess<R, CTX> process = processIterator.next();
			if(process.canProcess(ctx, level.getRawLevel()))
			{
				process.doProcessTick(ctx, level);
				tickedAny = true;
			}
			if(process.clearProcess)
			{
				processIterator.remove();
				onQueueChange.run();
			}
		}
		if(tickedAny)
			markDirty.run();
		return tickedAny;
	}

	public Tag toNBT()
	{
		ListTag processList = new ListTag();
		for(final MultiblockProcess<R, CTX> process : processQueue)
		{
			CompoundTag tag = new CompoundTag();
			tag.putString("recipe", process.getRecipeId().toString());
			tag.putInt("process_processTick", process.processTick);
			process.writeExtraDataToNBT(tag);
			processList.add(tag);
		}
		return processList;
	}

	public void fromNBT(Tag nbt, ProcessLoader<R, CTX> loader)
	{
		if(!(nbt instanceof ListTag list))
			return;
		this.processQueue.clear();
		for(final Tag tag : list)
			if(tag instanceof CompoundTag processTag)
			{
				final MultiblockProcess<R, CTX> loadedProcess = loader.fromNBT(getRecipeFromID, processTag);
				if(loadedProcess!=null)
					this.processQueue.add(loadedProcess);
			}
	}

	public BiFunction<Level, ResourceLocation, R> recipeGetter()
	{
		return getRecipeFromID;
	}

	public boolean addProcessToQueue(MultiblockProcess<R, CTX> process, Level level, boolean simulate)
	{
		return addProcessToQueue(process, level, simulate, false);
	}

	public boolean addProcessToQueue(
			MultiblockProcess<R, CTX> process, Level level, boolean simulate, boolean addToPrevious
	)
	{
		if(addToPrevious&&process instanceof MultiblockProcessInWorld)
		{
			// Pattern variables look fine in IntelliJ, but cause the "real" compiler to complain. Probably related to
			// the CTX type parameter
			@SuppressWarnings("PatternVariableCanBeUsed") final MultiblockProcessInWorld<R> newProcess = (MultiblockProcessInWorld<R>)process;
			for(MultiblockProcess<R, CTX> curr : processQueue)
				if(curr instanceof MultiblockProcessInWorld&&process.getRecipeId().equals(curr.getRecipeId()))
				{
					boolean canStack = true;
					final MultiblockProcessInWorld<R> existingProcess = (MultiblockProcessInWorld<R>)curr;
					for(ItemStack old : existingProcess.inputItems)
					{
						for(ItemStack in : newProcess.inputItems)
							if(ItemStack.isSameItem(old, in)&&Utils.compareItemNBT(old, in))
								if(old.getCount()+in.getCount() > old.getMaxStackSize())
								{
									canStack = false;
									break;
								}
						if(!canStack)
							break;
					}
					if(canStack)
					{
						if(!simulate)
							for(ItemStack old : existingProcess.inputItems)
							{
								for(ItemStack in : newProcess.inputItems)
									if(ItemStack.isSameItem(old, in)&&Utils.compareItemNBT(old, in))
									{
										old.grow(in.getCount());
										break;
									}
							}
						return true;
					}
				}
		}
		if(maxQueueLength < 0||processQueue.size() < maxQueueLength)
		{
			if(processQueue.size() > 0)
			{
				MultiblockProcess<R, CTX> previousProcess = processQueue.get(processQueue.size()-1);
				final int maxTime = previousProcess.getMaxTicks(level);
				float dist = previousProcess.processTick/(float)maxTime;
				if(dist < minDelayAfter.applyAsDouble(maxTime))
					return false;
			}

			if(!simulate)
				processQueue.add(process);
			markDirty.run();
			onQueueChange.run();
			return true;
		}
		return false;
	}

	public int getMaxQueueSize()
	{
		return maxQueueLength;
	}

	public int getQueueSize()
	{
		return processQueue.size();
	}

	public float getQueueFill(boolean allowStacking)
	{
		if(maxQueueLength <= 0) // I don't think we have an example of this, but sanity checks are good
			return 0;
		if(!allowStacking) // Easy and early return
			return this.getQueueSize()/(float)maxQueueLength;

		// And the messy one where we have to analyze the whole queue
		return getQueue().stream().map(process -> {
			if(process instanceof MultiblockProcessInWorld<? extends MultiblockRecipe> inWorld)
			{
				float f = 0;
				for(ItemStack stack : inWorld.inputItems)
					if(!stack.isEmpty())
						f = stack.getCount()/(float)stack.getMaxStackSize();
				return f/(float)inWorld.inputItems.size();
			}
			else
			{
				return 1f;
			}
		}).reduce(Float::sum).orElse(0f)/(float)maxQueueLength;
	}

	public CheckOption<MultiblockProcessor<R, CTX>>[] getMachineInterfaceOptions(boolean allowStacking)
	{
		return MachineInterfaceHandler.buildComparativeConditions(value -> value.getQueueFill(allowStacking));
	}


	public List<MultiblockProcess<R, CTX>> getQueue()
	{
		return Collections.unmodifiableList(processQueue);
	}

	public void clear()
	{
		this.processQueue.clear();
		this.markDirty.run();
		this.onQueueChange.run();
	}

	public interface ProcessLoader<R extends MultiblockRecipe, CTX extends ProcessContext<R>>
	{
		MultiblockProcess<R, CTX> fromNBT(BiFunction<Level, ResourceLocation, R> getRecipe, CompoundTag data);
	}

	// Convenience classes to deal with the lack of typedefs
	public static class InWorldProcessor<R extends MultiblockRecipe> extends MultiblockProcessor<R, ProcessContextInWorld<R>>
	{
		public InWorldProcessor(int maxQueueLength, IntToDoubleFunction minDelayAfter, int maxProcessPerTick, Runnable markDirty, Runnable onQueueChange, BiFunction<Level, ResourceLocation, @Nullable R> getRecipeFromID)
		{
			super(maxQueueLength, minDelayAfter, maxProcessPerTick, markDirty, onQueueChange, getRecipeFromID);
		}
	}

	public static class InMachineProcessor<R extends MultiblockRecipe> extends MultiblockProcessor<R, ProcessContextInMachine<R>>
	{
		public InMachineProcessor(int maxQueueLength, float minDelayAfter, int maxProcessPerTick, Runnable markDirty, BiFunction<Level, ResourceLocation, @Nullable R> getRecipeFromID)
		{
			super(maxQueueLength, minDelayAfter, maxProcessPerTick, markDirty, getRecipeFromID);
		}

		public InMachineProcessor(int maxQueueLength, IntToDoubleFunction minDelayAfter, int maxProcessPerTick, Runnable markDirty, Runnable onQueueChange, BiFunction<Level, ResourceLocation, @Nullable R> getRecipeFromID)
		{
			super(maxQueueLength, minDelayAfter, maxProcessPerTick, markDirty, onQueueChange, getRecipeFromID);
		}
	}

	public interface InWorldProcessLoader<R extends MultiblockRecipe> extends ProcessLoader<R, ProcessContextInWorld<R>>
	{
	}
}
