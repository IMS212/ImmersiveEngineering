/*
 * BluSunrize
 * Copyright (c) 2020
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.multiblocks;

import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.common.register.IEMultiblockLogic;
import net.minecraft.core.BlockPos;
public class SawmillMultiblock extends IETemplateMultiblock
{
	public SawmillMultiblock()
	{
		super(IEApi.ieLoc("multiblocks/sawmill"),
				new BlockPos(2, 1, 1), new BlockPos(2, 0, 2), new BlockPos(5, 3, 3),
				IEMultiblockLogic.SAWMILL);
	}

	@Override
	public float getManualScale()
	{
		return 12;
	}
}