/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.crafting;

import blusunrize.immersiveengineering.api.crafting.cache.CachedRecipeList;
import blusunrize.immersiveengineering.api.utils.SetRestrictedField;
import com.google.common.collect.Lists;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author BluSunrize - 01.03.2016
 * <p>
 * The recipe for the Fermenter
 */
public class FermenterRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, IERecipeSerializer<FermenterRecipe>> SERIALIZER;
	public static final CachedRecipeList<FermenterRecipe> RECIPES = new CachedRecipeList<>(IERecipeTypes.FERMENTER);
	public static final SetRestrictedField<RecipeMultiplier> MULTIPLIERS = SetRestrictedField.common();

	public IngredientWithSize input;
	public final FluidStack fluidOutput;
	@Nonnull
	public final TagOutput itemOutput;

	public FermenterRecipe(FluidStack fluidOutput, @Nonnull TagOutput itemOutput, IngredientWithSize input, int energy)
	{
		super(itemOutput, IERecipeTypes.FERMENTER, 80, energy, MULTIPLIERS);
		this.fluidOutput = fluidOutput;
		this.itemOutput = itemOutput;
		this.input = input;

		setInputListWithSizes(Lists.newArrayList(this.input));
		this.fluidOutputList = Lists.newArrayList(this.fluidOutput);
		this.outputList = new TagOutputList(this.itemOutput);
	}

	@Override
	protected IERecipeSerializer<FermenterRecipe> getIESerializer()
	{
		return SERIALIZER.get();
	}

	public FermenterRecipe setInputSize(int size)
	{
		this.input = this.input.withSize(size);
		return this;
	}

	public static RecipeHolder<FermenterRecipe> findRecipe(Level level, ItemStack input)
	{
		if(input.isEmpty())
			return null;
		for(RecipeHolder<FermenterRecipe> recipe : RECIPES.getRecipes(level))
			if(recipe.value().input.test(input))
				return recipe;
		return null;
	}

	@Override
	public int getMultipleProcessTicks()
	{
		return 0;
	}

	public static SortedMap<Component, Integer> getFluidValuesSorted(Level level, Fluid f, boolean inverse)
	{
		SortedMap<Component, Integer> map = new TreeMap<>(
				Comparator.comparing(
						(Function<Component, String>) Component::getString,
						inverse?Comparator.reverseOrder(): Comparator.naturalOrder()
				)
		);
		for(RecipeHolder<FermenterRecipe> holder : RECIPES.getRecipes(level))
		{
			FermenterRecipe recipe = holder.value();
			if(recipe.fluidOutput!=null&&recipe.fluidOutput.getFluid()==f&&!recipe.input.hasNoMatchingItems())
			{
				ItemStack is = recipe.input.getMatchingStacks()[0];
				map.put(is.getHoverName(), recipe.fluidOutput.getAmount());
			}
		}
		return map;
	}
}