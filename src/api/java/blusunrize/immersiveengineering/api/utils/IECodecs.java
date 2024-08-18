/*
 * BluSunrize
 * Copyright (c) 2024
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.network.codec.ByteBufCodecs.collection;

public class IECodecs
{
	public static StreamCodec<ByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, Vec3::x,
			ByteBufCodecs.DOUBLE, Vec3::y,
			ByteBufCodecs.DOUBLE, Vec3::z,
			Vec3::new
	);
	public static final StreamCodec<FriendlyByteBuf, int[]> VAR_INT_ARRAY_STREAM_CODEC = new StreamCodec<>()
	{
		@Override
		public int[] decode(FriendlyByteBuf p_320376_)
		{
			return p_320376_.readVarIntArray();
		}

		@Override
		public void encode(FriendlyByteBuf p_320158_, int[] p_320396_)
		{
			p_320158_.writeVarIntArray(p_320396_);
		}
	};

	public static final Codec<NonNullList<Ingredient>> NONNULL_INGREDIENTS = Ingredient.LIST_CODEC.xmap(
			l -> {
				NonNullList<Ingredient> result = NonNullList.create();
				result.addAll(l);
				return result;
			},
			Function.identity()
	);

	public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, NonNullList<V>> nonNullList()
	{
		return p_320272_ -> collection($ -> NonNullList.create(), p_320272_);
	}

	public static <T> StreamCodec<ByteBuf, TagKey<T>> tagCodec(ResourceKey<? extends Registry<T>> key)
	{
		return ResourceLocation.STREAM_CODEC.map(rl -> TagKey.create(key, rl), TagKey::location);
	}

	public static <E extends Enum<E>> Codec<E> enumCodec(E[] keys)
	{
		return Codec.intRange(0, keys.length-1).xmap(i -> keys[i], E::ordinal);
	}

	public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumStreamCodec(E[] keys)
	{
		return ByteBufCodecs.VAR_INT.map(i -> keys[i], E::ordinal);
	}

	public static <K, B extends ByteBuf, T>
	StreamCodec<B, Map<K, T>> mapStreamCodec(StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, T> valueCodec)
	{
		return StreamCodec.<B, Pair<K, T>, K, T>composite(
						keyCodec, Pair::getFirst,
						valueCodec, Pair::getSecond,
						Pair::of
				).apply(ByteBufCodecs.list())
				.map(IECodecs::listToMap, IECodecs::mapToList);
	}

	public static <E extends Enum<E>, B extends ByteBuf, T>
	StreamCodec<B, EnumMap<E, T>> enumMapStreamCodec(E[] keys, StreamCodec<? super B, T> valueCodec)
	{
		final var keyCodec = enumStreamCodec(keys);
		return mapStreamCodec(keyCodec, valueCodec).map(EnumMap::new, Function.identity()).cast();
	}

	public static <K, T> Codec<Map<K, T>> listBasedMap(Codec<K> keyCodec, Codec<T> valueCodec)
	{
		Codec<Pair<K, T>> entryCodec = RecordCodecBuilder.create(inst -> inst.group(
				keyCodec.fieldOf("key").forGetter(Pair::getFirst),
				valueCodec.fieldOf("value").forGetter(Pair::getSecond)
		).apply(inst, Pair::of));
		return entryCodec.listOf().xmap(IECodecs::listToMap, IECodecs::mapToList);
	}

	public static <E extends Enum<E>, T> Codec<EnumMap<E, T>> listBasedEnumMap(E[] keys, Codec<T> valueCodec)
	{
		final var keyCodec = enumCodec(keys);
		return listBasedMap(keyCodec, valueCodec).xmap(EnumMap::new, Function.identity());
	}

	public static <V> Codec<List<V>> directDispatchMap(
			Function<String, Codec<V>> valueCodec, Function<V, String> getKey
	)
	{
		return new Codec<>()
		{
			@Override
			public <T> DataResult<Pair<List<V>, T>> decode(DynamicOps<T> ops, T input)
			{
				return ops.getMapValues(input).flatMap(s -> {
					DataResult<List<V>> result = DataResult.success(new ArrayList<>());
					for(var entry : s.toList())
						result = result.flatMap(
								current -> ops.getStringValue(entry.getFirst()).flatMap(
										key -> valueCodec.apply(key).decode(ops, entry.getSecond()).map(
												value -> {
													var newMap = new ArrayList<>(current);
													newMap.add(value.getFirst());
													return newMap;
												}
										)
								)
						);
					return result.map(m -> new Pair<>(m, ops.empty()));
				});
			}

			@Override
			public <T> DataResult<T> encode(List<V> input, DynamicOps<T> ops, T prefix)
			{
				DataResult<T> result = DataResult.success(prefix);
				for(var entry : input)
					result = result.flatMap(oldT -> {
						var type = getKey.apply(entry);
						var maybeValue = valueCodec.apply(type).encodeStart(ops, entry);
						var key = ops.createString(type);
						return maybeValue.flatMap(valueT -> ops.mergeToMap(oldT, key, valueT));
					});
				return result;
			}
		};
	}

	// DFU unit codec produces an error when presented with a non-map input
	public static <T> Codec<T> betterUnitCodec(T value)
	{
		return new Codec<T>()
		{
			@Override
			public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input)
			{
				return DataResult.success(Pair.of(value, input));
			}

			@Override
			public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix)
			{
				return DataResult.success(prefix);
			}
		};
	}

	public static <C> Codec<Set<C>> setOf(Codec<C> codec)
	{
		return codec.listOf().xmap(Set::copyOf, List::copyOf);
	}

	public static <B extends ByteBuf, C> StreamCodec<B, Set<C>> setOf(StreamCodec<B, C> codec)
	{
		return codec.apply(ByteBufCodecs.list()).map(Set::copyOf, List::copyOf);
	}

	private static <K, T>
	Map<K, T> listToMap(List<Pair<K, T>> l)
	{
		return l.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
	}

	private static <K, T>
	List<Pair<K, T>> mapToList(Map<K, T> m)
	{
		return m.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue())).toList();
	}
}