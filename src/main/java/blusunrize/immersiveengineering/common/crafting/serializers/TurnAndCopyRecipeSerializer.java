/*
 * BluSunrize
 * Copyright (c) 2020
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 *
 */

package blusunrize.immersiveengineering.common.crafting.serializers;

import blusunrize.immersiveengineering.common.crafting.fluidaware.TurnAndCopyRecipe;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class TurnAndCopyRecipeSerializer implements RecipeSerializer<TurnAndCopyRecipe>
{
	private record AdditionalData(
			List<Integer> copySlots,
			boolean quarter,
			boolean eights,
			Optional<String> predicate
	)
	{
		public AdditionalData(TurnAndCopyRecipe recipe)
		{
			this(
					recipe.getCopyTargets(),
					recipe.isQuarterTurn(),
					recipe.isEightTurn(),
					Optional.ofNullable(recipe.getBufferPredicate())
			);
		}
	}

	private static final Codec<AdditionalData> ADDITIONAL_CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ExtraCodecs.strictOptionalField(Codec.INT.listOf(), "copyNBT", List.of()).forGetter(AdditionalData::copySlots),
			ExtraCodecs.strictOptionalField(Codec.BOOL, "quarter_turn", false).forGetter(AdditionalData::quarter),
			ExtraCodecs.strictOptionalField(Codec.BOOL, "eight_turn", false).forGetter(AdditionalData::eights),
			ExtraCodecs.strictOptionalField(Codec.STRING, "copy_nbt_predicate").forGetter(AdditionalData::predicate)
	).apply(inst, AdditionalData::new));

	public static final Codec<TurnAndCopyRecipe> CODEC = Codec.pair(
			RecipeSerializer.SHAPED_RECIPE.codec(), ADDITIONAL_CODEC
	).xmap(
			p -> {
				AdditionalData extra = p.getSecond();
				TurnAndCopyRecipe result = new TurnAndCopyRecipe(p.getFirst(), extra.copySlots());
				if(extra.quarter())
					result.allowQuarterTurn();
				if(extra.eights())
					result.allowEighthTurn();
				if(extra.predicate().isPresent())
					result.setNBTCopyPredicate(extra.predicate().get());
				return result;
			},
			r -> Pair.of(r.toVanilla(), new AdditionalData(r))
	);

	@Override
	public Codec<TurnAndCopyRecipe> codec()
	{
		return CODEC;
	}

	@Override
	public TurnAndCopyRecipe fromNetwork(@Nonnull FriendlyByteBuf buffer)
	{
		ShapedRecipe basic = RecipeSerializer.SHAPED_RECIPE.fromNetwork(buffer);
		List<Integer> copySlots = buffer.readList(FriendlyByteBuf::readVarInt);
		TurnAndCopyRecipe recipe = new TurnAndCopyRecipe(basic, copySlots);
		if(buffer.readBoolean())
			recipe.setNBTCopyPredicate(buffer.readUtf(512));
		if(buffer.readBoolean())
			recipe.allowQuarterTurn();
		if(buffer.readBoolean())
			recipe.allowEighthTurn();
		return recipe;
	}

	@Override
	public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull TurnAndCopyRecipe recipe)
	{
		RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe.toVanilla());
		buffer.writeCollection(recipe.getCopyTargets(), FriendlyByteBuf::writeVarInt);
		if(recipe.hasCopyPredicate())
		{
			buffer.writeBoolean(true);
			buffer.writeUtf(recipe.getBufferPredicate());
		}
		else
			buffer.writeBoolean(false);
		buffer.writeBoolean(recipe.isQuarterTurn());
		buffer.writeBoolean(recipe.isEightTurn());
	}
}