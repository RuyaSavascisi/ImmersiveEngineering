/*
 * BluSunrize
 * Copyright (c) 2024
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.utils;

import blusunrize.immersiveengineering.api.utils.codec.DualCodec;
import blusunrize.immersiveengineering.api.utils.codec.DualCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeColor;

public record Color4(float r, float g, float b, float a)
{
	public static final Color4 WHITE = new Color4(1, 1, 1, 1);
	public static final DualCodec<ByteBuf, Color4> CODECS = DualCodecs.composite(
			DualCodecs.FLOAT.fieldOf("r"), Color4::r,
			DualCodecs.FLOAT.fieldOf("g"), Color4::g,
			DualCodecs.FLOAT.fieldOf("b"), Color4::b,
			DualCodecs.FLOAT.fieldOf("a"), Color4::a,
			Color4::new
	);

	public Color4(int rgba)
	{
		this(((rgba>>16)&255)/255f, ((rgba>>8)&255)/255f, (rgba&255)/255f, ((rgba>>24)&255)/255f);
	}

	public static Color4 from(DyeColor dyeColor)
	{
		if(dyeColor==null)
			return new Color4(1, 1, 1, 1);
		int rgb = dyeColor.getTextureDiffuseColor();
		return new Color4(((rgb>>16)&255)/255f, ((rgb>>8)&255)/255f, (rgb&255)/255f, 1);
	}

	public static Color4 load(Tag nbt)
	{
		return CODECS.codec().decode(NbtOps.INSTANCE, nbt).getOrThrow().getFirst();
	}

	public Tag save()
	{
		return CODECS.codec().encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}

	public int toInt()
	{
		final int rInt = (int)(255*r);
		final int gInt = (int)(255*g);
		final int bInt = (int)(255*b);
		final int aInt = (int)(255*a);
		return (aInt<<24)|(rInt<<16)|(gInt<<8)|bInt;
	}
}
