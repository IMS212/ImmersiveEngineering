/*
 * BluSunrize
 * Copyright (c) 2020
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 *
 */

package blusunrize.immersiveengineering.common.entities;

import blusunrize.immersiveengineering.common.blocks.wooden.WoodenBarrelBlockEntity;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import blusunrize.immersiveengineering.common.register.IEBlocks.WoodenDevices;
import blusunrize.immersiveengineering.common.register.IEEntityTypes;
import blusunrize.immersiveengineering.common.register.IEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class BarrelMinecartEntity extends IEMinecartEntity<WoodenBarrelBlockEntity>
{
	public MinecartFluidHandler minecartFluidHandler = new MinecartFluidHandler(this);

	public BarrelMinecartEntity(Level world, double x, double y, double z)
	{
		this(IEEntityTypes.BARREL_MINECART.get(), world, x, y, z);
	}

	public BarrelMinecartEntity(EntityType<?> type, Level world, double x, double y, double z)
	{
		super(type, world, x, y, z);
	}

	public BarrelMinecartEntity(EntityType<?> type, Level world)
	{
		super(type, world);
	}

	public static <T extends BarrelMinecartEntity>
	void registerCapabilities(RegisterCapabilitiesEvent ev, Supplier<EntityType<T>> type)
	{
		ev.registerEntity(FluidHandler.ENTITY, type.get(), (e, $) -> e.minecartFluidHandler);
	}

	@Override
	public ItemStack getPickResult()
	{
		return new ItemStack(IEItems.Minecarts.CART_WOODEN_BARREL.get());
	}

	@Override
	public void writeTileToItem(ItemStack itemStack)
	{
		CompoundTag tag = new CompoundTag();
		this.containedBlockEntity.writeTank(tag, true);
		if(!tag.isEmpty())
			itemStack.setTag(tag);
	}

	@Override
	public void readTileFromItem(LivingEntity placer, ItemStack itemStack)
	{
		this.containedBlockEntity.onBEPlaced(itemStack);
	}

	@Nonnull
	@Override
	public InteractionResult interact(@Nonnull Player player, @Nonnull InteractionHand hand)
	{
		if(super.interact(player, hand)==InteractionResult.SUCCESS)
			return InteractionResult.SUCCESS;
		ItemStack itemstack = player.getItemInHand(hand);
		if(FluidUtil.getFluidHandler(itemstack).isPresent())
		{
			this.containedBlockEntity.interact(null, player, hand, itemstack, 0, 0, 0);
			return InteractionResult.SUCCESS;//always return true to avoid placing lava in the world
		}
		return InteractionResult.PASS;
	}

	@Override
	protected Supplier<WoodenBarrelBlockEntity> getTileProvider()
	{
		return () -> new WoodenBarrelBlockEntity(BlockPos.ZERO, WoodenDevices.WOODEN_BARREL.defaultBlockState());
	}

	@Override
	public BlockState getDisplayBlockState()
	{
		return IEBlocks.WoodenDevices.WOODEN_BARREL.defaultBlockState();
	}


	static class MinecartFluidHandler implements IFluidHandler
	{
		final BarrelMinecartEntity minecart;

		public MinecartFluidHandler(BarrelMinecartEntity minecart)
		{
			this.minecart = minecart;
		}

		@Override
		public int getTanks()
		{
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank)
		{
			return this.minecart.containedBlockEntity.tank.getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank)
		{
			return this.minecart.containedBlockEntity.tank.getTankCapacity(tank);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack)
		{
			return this.minecart.containedBlockEntity.tank.isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action)
		{
			int filled = this.minecart.containedBlockEntity.tank.fill(resource, action);
			updateContainingEntity();
			return filled;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action)
		{
			FluidStack drained = this.minecart.containedBlockEntity.tank.drain(resource, action);
			updateContainingEntity();
			return drained;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action)
		{
			FluidStack drained = this.minecart.containedBlockEntity.tank.drain(maxDrain, action);
			updateContainingEntity();
			return drained;
		}

		private void updateContainingEntity()
		{
			this.minecart.updateSynchedData();
		}
	}
}
