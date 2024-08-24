/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.gui.elements;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonIE.ButtonTexture;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonIE.IIEPressable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

import static blusunrize.immersiveengineering.api.IEApi.ieLoc;

public class GuiButtonCheckbox extends GuiButtonBoolean
{
	public static final ButtonTexture TEXTURE = new ButtonTexture(ieLoc("hud/checkbox"));

	public GuiButtonCheckbox(int x, int y, Component name, Supplier<Boolean> state, IIEPressable<GuiButtonState<Boolean>> handler)
	{
		super(x, y, 8, 8, name, state, TEXTURE, TEXTURE, handler);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		super.renderWidget(graphics, mouseX, mouseY, partialTicks);
		if(this.visible&&getState())
		{
			int color;
			if(!this.active)
				color = 0xA0A0A0;
			else if(this.isHovered)
				color = Lib.COLOUR_I_ImmersiveOrange;
			else
				color = 0xE0E0E0;
			graphics.drawCenteredString(Minecraft.getInstance().font, "\u2714", getX()+width/2, getY()-2, color);
		}
	}
}
