/*
 * BluSunrize
 * Copyright (c) 2020
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.items;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent.DisplayItemsAdapter;

public class FakeIconItem extends IEBaseItem
{
	public FakeIconItem()
	{
		super(new Properties().stacksTo(1));
	}

	@Override
	public DisplayItemsAdapter getCreativeTabFiller()
	{
		return (enabledFlags, populator, hasPermissions) -> {};
	}
}