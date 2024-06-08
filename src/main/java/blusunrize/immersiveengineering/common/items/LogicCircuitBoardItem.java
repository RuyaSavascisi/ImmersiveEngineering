/*
 * BluSunrize
 * Copyright (c) 2021
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.items;

import blusunrize.immersiveengineering.api.client.TextUtils;
import blusunrize.immersiveengineering.api.tool.LogicCircuitHandler.LogicCircuitInstruction;
import blusunrize.immersiveengineering.common.register.IEDataComponents;
import blusunrize.immersiveengineering.common.register.IEItems.Misc;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nonnull;
import java.util.List;

public class LogicCircuitBoardItem extends IEBaseItem
{
	public LogicCircuitBoardItem()
	{
		super(new Properties().stacksTo(1));
	}

	@Nonnull
	@Override
	public String getDescriptionId(ItemStack stack)
	{
		return this.getDescriptionId();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag)
	{
		LogicCircuitInstruction instruction = stack.get(IEDataComponents.CIRCUIT_INSTRUCTION);
		if(instruction!=null)
			list.add(TextUtils.applyFormat(instruction.getFormattedString(), ChatFormatting.GRAY));
	}

	public static ItemStack buildCircuitBoard(LogicCircuitInstruction instruction)
	{
		ItemStack stack = new ItemStack(Misc.LOGIC_CIRCUIT_BOARD.get());
		stack.set(IEDataComponents.CIRCUIT_INSTRUCTION, instruction);
		return stack;
	}
}