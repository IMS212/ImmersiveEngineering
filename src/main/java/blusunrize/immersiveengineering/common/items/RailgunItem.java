/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.items;

import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.shader.CapabilityShader;
import blusunrize.immersiveengineering.api.shader.CapabilityShader.ShaderWrapper_Item;
import blusunrize.immersiveengineering.api.shader.ShaderRegistry;
import blusunrize.immersiveengineering.api.shader.ShaderRegistry.ShaderAndCase;
import blusunrize.immersiveengineering.api.tool.RailgunHandler;
import blusunrize.immersiveengineering.api.tool.RailgunHandler.IRailgunProjectile;
import blusunrize.immersiveengineering.api.tool.ZoomHandler.IZoomTool;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeEffect;
import blusunrize.immersiveengineering.api.utils.ItemUtils;
import blusunrize.immersiveengineering.common.config.IEServerConfig;
import blusunrize.immersiveengineering.common.entities.RailgunShotEntity;
import blusunrize.immersiveengineering.common.gui.IESlot;
import blusunrize.immersiveengineering.common.items.IEItemInterfaces.IScrollwheel;
import blusunrize.immersiveengineering.common.items.ItemCapabilityRegistration.ItemCapabilityRegistrar;
import blusunrize.immersiveengineering.common.register.IEDataComponents;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class RailgunItem extends UpgradeableToolItem implements IZoomTool, IScrollwheel
{
	public static final String TYPE = "RAILGUN";

	public RailgunItem()
	{
		super(new Properties().stacksTo(1).component(IEDataComponents.GENERIC_ENERGY, 0), TYPE, 2+1);
	}

	@Override
	public Slot[] getWorkbenchSlots(AbstractContainerMenu container, ItemStack stack, Level level, Supplier<Player> getPlayer, IItemHandler toolInventory)
	{
		return new Slot[]{
				new IESlot.Upgrades(container, toolInventory, 0, 80, 32, TYPE, stack, true, level, getPlayer),
				new IESlot.Upgrades(container, toolInventory, 1, 100, 32, TYPE, stack, true, level, getPlayer)
		};
	}

	@Override
	public boolean canModify(ItemStack stack)
	{
		return true;
	}

	@Override
	public void recalculateUpgrades(ItemStack stack, Level w, Player player)
	{
		super.recalculateUpgrades(stack, w, player);
		capStoredEnergyAtMaximum(stack);
	}

	@Override
	public void clearUpgrades(ItemStack stack)
	{
		super.clearUpgrades(stack);
		capStoredEnergyAtMaximum(stack);
	}

	private void capStoredEnergyAtMaximum(ItemStack stack)
	{
		var energy = stack.getOrDefault(IEDataComponents.GENERIC_ENERGY, 0);
		if(energy > getMaxEnergyStored(stack))
			stack.set(IEDataComponents.GENERIC_ENERGY, getMaxEnergyStored(stack));
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		if(slotChanged||CapabilityShader.shouldReequipDueToShader(oldStack, newStack))
			return true;
		else
			return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
	}

	public static void registerCapabilities(ItemCapabilityRegistrar registrar)
	{
		registerCapabilitiesISI(registrar);
		registrar.register(
				EnergyStorage.ITEM,
				stack -> new ComponentEnergyStorage(stack, IEDataComponents.GENERIC_ENERGY.get(), getMaxEnergyStored(stack))
		);
		registrar.register(
				CapabilityShader.ITEM,
				stack -> new ShaderWrapper_Item(IEApi.ieLoc("railgun"), stack)
		);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag)
	{
		IEnergyStorage energy = Objects.requireNonNull(stack.getCapability(EnergyStorage.ITEM));
		String stored = energy.getEnergyStored()+"/"+getMaxEnergyStored(stack);
		list.add(Component.translatable(Lib.DESC+"info.energyStored", stored).withStyle(ChatFormatting.GRAY));
	}

	@Nonnull
	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.NONE;
	}

	@Nonnull
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, @Nonnull InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		int consumption = IEServerConfig.TOOLS.railgun_consumption.get();
		IEnergyStorage energy = Objects.requireNonNull(stack.getCapability(EnergyStorage.ITEM));
		if(energy.extractEnergy(consumption, true)==consumption&&!findAmmo(stack, player).isEmpty())
		{
			player.startUsingItem(hand);
			playChargeSound(player, stack);
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
		}
		return new InteractionResultHolder<>(InteractionResult.PASS, stack);
	}

	public static void playChargeSound(LivingEntity living, ItemStack railgun)
	{
		living.level().playSound(null,
				living.getX(), living.getY(), living.getZ(),
				getChargeTime(railgun) <= 20?IESounds.chargeFast.value(): IESounds.chargeSlow.value(), SoundSource.PLAYERS,
				1.5f, 1f
		);

	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int count)
	{
		int inUse = this.getUseDuration(stack, user)-count;
		if(inUse > getChargeTime(stack)&&inUse%20==user.getRandom().nextInt(20))
		{
			user.level().playSound(null, user.getX(), user.getY(), user.getZ(), IESounds.spark.value(), SoundSource.PLAYERS, .8f+(.2f*user.getRandom().nextFloat()), .5f+(.5f*user.getRandom().nextFloat()));
			ShaderAndCase shader = ShaderRegistry.getStoredShaderAndCase(stack);
			if(shader!=null)
			{
				Vec3 pos = Utils.getLivingFrontPos(user, .4375, user.getBbHeight()*.75, ItemUtils.getLivingHand(user, user.getUsedItemHand()), false, 1);
				shader.registryEntry().getEffectFunction().execute(user.level(), stack, shader.sCase().getShaderType().toString(), pos, null, .0625f);
			}
		}
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int timeLeft)
	{
		if(!world.isClientSide()&&user instanceof Player player)
		{
			int inUse = this.getUseDuration(stack, user)-timeLeft;
			if(inUse < getChargeTime(stack))
				return;
			int consumption = IEServerConfig.TOOLS.railgun_consumption.get();
			IEnergyStorage energy = Objects.requireNonNull(stack.getCapability(EnergyStorage.ITEM));
			if(energy.extractEnergy(consumption, true)==consumption)
			{
				ItemStack ammo = findAmmo(stack, player);
				if(!ammo.isEmpty())
				{
					ItemStack ammoConsumed = ammo.split(1);
					fireProjectile(stack, world, user, ammoConsumed);
					energy.extractEnergy(consumption, false);
				}
			}
		}
	}

	public static Entity fireProjectile(ItemStack railgun, Level world, LivingEntity user, ItemStack ammo)
	{
		IRailgunProjectile projectileProperties = RailgunHandler.getProjectile(ammo);
		float speed = 20;
		Entity shot = new RailgunShotEntity(user.level(), user, speed, 0, ammo);
		shot = projectileProperties.getProjectile(user instanceof Player player?player: null, ammo, shot);
		user.level().playSound(null, user.getX(), user.getY(), user.getZ(), IESounds.railgunFire.value(), SoundSource.PLAYERS, 1, .5f+(.5f*user.getRandom().nextFloat()));
		if(!world.isClientSide)
			user.level().addFreshEntity(shot);

		ShaderAndCase shader = ShaderRegistry.getStoredShaderAndCase(railgun);
		if(shader!=null)
		{
			HumanoidArm handside = user.getMainArm();
			if(user.getUsedItemHand()!=InteractionHand.MAIN_HAND)
				handside = handside==HumanoidArm.LEFT?HumanoidArm.RIGHT: HumanoidArm.LEFT;
			Vec3 pos = Utils.getLivingFrontPos(user, .75, user.getBbHeight()*.75, handside, false, 1);
			shader.registryEntry().getEffectFunction().execute(world, railgun,
					shader.sCase().getShaderType().toString(), pos,
					Vec3.directionFromRotation(user.getRotationVector()), .125f);
		}
		return shot;
	}

	public static ItemStack findAmmo(ItemStack railgun, Player player)
	{
		// Check for cached slot
		var lastSlot = railgun.get(IEDataComponents.RAILGUN_AMMO_SLOT);
		if(lastSlot!=null)
		{
			ItemStack ammo = findAmmoInSlot(player, lastSlot);
			if(!ammo.isEmpty())
				return ammo;
		}

		// Find it otherwise
		if(isAmmo(player.getItemInHand(InteractionHand.OFF_HAND)))
		{
			railgun.set(IEDataComponents.RAILGUN_AMMO_SLOT, 0);
			return player.getItemInHand(InteractionHand.OFF_HAND);
		}
		else if(isAmmo(player.getItemInHand(InteractionHand.MAIN_HAND)))
		{
			railgun.set(IEDataComponents.RAILGUN_AMMO_SLOT, 1);
			return player.getItemInHand(InteractionHand.MAIN_HAND);
		}
		else
			for(int i = 0; i < player.getInventory().getContainerSize(); i++)
			{
				ItemStack itemstack = player.getInventory().getItem(i);
				if(isAmmo(itemstack))
				{
					railgun.set(IEDataComponents.RAILGUN_AMMO_SLOT, 2+i);
					return itemstack;
				}
			}
		return ItemStack.EMPTY;
	}

	public static ItemStack findAmmoInSlot(Player player, int slot)
	{
		ItemStack ammo = ItemStack.EMPTY;
		if(slot==0||slot==1)
			ammo = player.getItemInHand(slot==0?InteractionHand.MAIN_HAND: InteractionHand.OFF_HAND);
		else if(slot > 1&&slot-2 < player.getInventory().getContainerSize())
			ammo = player.getInventory().getItem(slot-2);
		if(isAmmo(ammo))
			return ammo;
		return ItemStack.EMPTY;
	}

	public static boolean isAmmo(ItemStack stack)
	{
		if(stack.isEmpty())
			return false;
		RailgunHandler.IRailgunProjectile prop = RailgunHandler.getProjectile(stack);
		return prop!=null;
	}

	private boolean checkAmmoSlot(ItemStack stack, Player player, int actualSlot)
	{
		if(!findAmmoInSlot(player, actualSlot).isEmpty())
		{
			stack.set(IEDataComponents.RAILGUN_AMMO_SLOT, actualSlot);
			player.getInventory().setChanged();
			return true;
		}
		return false;
	}

	@Override
	public void onScrollwheel(ItemStack stack, Player player, boolean forward)
	{
		int slot = stack.getOrDefault(IEDataComponents.RAILGUN_AMMO_SLOT, 0);
		int count = player.getInventory().getContainerSize()+2;
		if(forward)
		{
			for(int i = 1; i < count; i++)
				if(checkAmmoSlot(stack, player, (slot+i)%count))
					return;
		}
		else
		{
			for(int i = count-1; i >= 1; i--)
				if(checkAmmoSlot(stack, player, (slot+i)%count))
					return;
		}
	}

	public static int getChargeTime(ItemStack railgun)
	{
		return (int)(40/(1+getUpgradesStatic(railgun).get(UpgradeEffect.SPEED)));
	}

	@Override
	public int getUseDuration(ItemStack p_41454_, LivingEntity p_344979_)
	{
		return 72000;
	}

	@Override
	public void removeFromWorkbench(Player player, ItemStack stack)
	{
		IItemHandler inv = stack.getCapability(ItemHandler.ITEM);
		if(inv!=null&&!inv.getStackInSlot(0).isEmpty()&&!inv.getStackInSlot(1).isEmpty())
			Utils.unlockIEAdvancement(player, "tools/upgrade_railgun");
	}

	public static int getMaxEnergyStored(ItemStack container)
	{
		return 8000;
	}


	@Override
	public boolean canZoom(ItemStack stack, Player player)
	{
		return this.getUpgrades(stack).has(UpgradeEffect.SCOPE);
	}

	float[] zoomSteps = new float[]{.1f, .15625f, .2f, .25f, .3125f, .4f, .5f, .625f};

	@Override
	public float[] getZoomSteps(ItemStack stack, Player player)
	{
		return zoomSteps;
	}
}