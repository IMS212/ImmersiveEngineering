/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.network;

import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.common.gui.IESlot.ItemHandlerGhost;
import blusunrize.immersiveengineering.common.util.IELogger;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class MessageSetGhostSlots implements IMessage
{
	public static final ResourceLocation ID = IEApi.ieLoc("set_ghost_slot");
	private final Int2ObjectMap<ItemStack> stacksToSet;

	public MessageSetGhostSlots(Int2ObjectMap<ItemStack> stacksToSet)
	{
		this.stacksToSet = stacksToSet;
	}

	public MessageSetGhostSlots(FriendlyByteBuf buf)
	{
		int size = buf.readInt();
		stacksToSet = new Int2ObjectOpenHashMap<>(size);
		for(int i = 0; i < size; i++)
		{
			int slot = buf.readInt();
			stacksToSet.put(slot, buf.readItem());
		}
	}

	@Override
	public void write(FriendlyByteBuf buf)
	{
		buf.writeInt(stacksToSet.size());
		for(Entry<ItemStack> e : stacksToSet.int2ObjectEntrySet())
		{
			buf.writeInt(e.getIntKey());
			buf.writeItem(e.getValue());
		}
	}

	@Override
	public void process(PlayPayloadContext context)
	{
		Player player = context.player().orElseThrow();
		context.workHandler().execute(() -> {
			AbstractContainerMenu container = player.containerMenu;
			if(container!=null)
				for(Entry<ItemStack> e : stacksToSet.int2ObjectEntrySet())
				{
					int slot = e.getIntKey();
					if(slot >= 0&&slot < container.slots.size())
					{
						Slot target = container.slots.get(slot);
						if(!(target instanceof ItemHandlerGhost))
						{
							IELogger.error("Player "+player.getDisplayName()+" tried to set the contents of a non-ghost slot."+
									"This is either a bug in IE or an attempt at cheating.");
							return;
						}
						//TODO this is most likely broken!
						container.setItem(slot, container.getStateId(), e.getValue());
					}
				}
		});
	}

	@Override
	public ResourceLocation id()
	{
		return ID;
	}
}