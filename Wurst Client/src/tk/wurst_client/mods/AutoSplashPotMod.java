/*
 * Copyright © 2014 - 2016 | Wurst-Imperium | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.mods;

import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;

import org.darkstorm.minecraft.gui.component.BoundedRangeComponent.ValueDisplay;

import tk.wurst_client.events.listeners.UpdateListener;
import tk.wurst_client.mods.Mod.Category;
import tk.wurst_client.mods.Mod.Info;
import tk.wurst_client.navigator.settings.SliderSetting;

@Info(category = Category.COMBAT,
	description = "Automatically throws splash healing potions if your health is below the set value.",
	name = "AutoSplashPot",
	tags = "AutoPotion,auto potion,auto splash potion")
public class AutoSplashPotMod extends Mod implements UpdateListener
{
	public float health = 20F;
	
	@Override
	public void initSettings()
	{
		settings.add(new SliderSetting("Health", health, 2, 20, 1,
			ValueDisplay.INTEGER)
		{
			@Override
			public void update()
			{
				health = (float)getValue();
			}
		});
	}
	
	@Override
	public void onEnable()
	{
		wurst.events.add(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		updateMS();
		
		if(pots() == 0)
			return;
		
		if(mc.thePlayer.getHealth() <= health && hasTimePassedM(500l))
			if(hasHotbarPots())
			{
				throwPot();
				updateLastMS();
			}else
				getFromUpInv();
		
	}
	
	@Override
	public void onDisable()
	{
		wurst.events.remove(UpdateListener.class, this);
	}
	
	private boolean isHealthPot(ItemStack stack)
	{
		if(stack == null)
			return false;
		if(stack.getItem() instanceof ItemPotion)
			try
			{
				ItemPotion potion = (ItemPotion)stack.getItem();
				if(ItemPotion.isSplash(stack.getItemDamage()))
					for(Object o : potion.getEffects(stack))
					{
						PotionEffect effect = (PotionEffect)o;
						if(effect.getPotionID() == Potion.heal.id)
							return true;
					}
			}catch(NullPointerException e)
			{
				e.printStackTrace();
			}
		return false;
	}
	
	private boolean hasHotbarPots()
	{
		for(int index = 36; index < 45; index++)
		{
			ItemStack stack =
				mc.thePlayer.inventoryContainer.getSlot(index).getStack();
			if(stack != null)
				if(isHealthPot(stack))
					return true;
		}
		return false;
	}
	
	private void getFromUpInv()
	{
		if(mc.currentScreen instanceof GuiChest)
			return;
		for(int index = 9; index < 36; index++)
		{
			ItemStack stack =
				mc.thePlayer.inventoryContainer.getSlot(index).getStack();
			if(stack != null)
				if(isHealthPot(stack))
				{
					mc.playerController.windowClick(0, index, 0, 1,
						mc.thePlayer);
					break;
				}
		}
	}
	
	private int pots()
	{
		int counter = 0;
		for(int index = 9; index < 45; index++)
		{
			ItemStack stack =
				mc.thePlayer.inventoryContainer.getSlot(index).getStack();
			if(stack != null)
				if(isHealthPot(stack))
					counter += stack.stackSize;
		}
		return counter;
	}
	
	private void throwPot()
	{
		for(int index = 36; index < 45; index++)
		{
			ItemStack stack =
				mc.thePlayer.inventoryContainer.getSlot(index).getStack();
			if(stack != null)
				if(isHealthPot(stack))
				{
					int oldslot = mc.thePlayer.inventory.currentItem;
					NetHandlerPlayClient sendQueue = mc.thePlayer.sendQueue;
					sendQueue
						.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
							mc.thePlayer.rotationYaw, 90.0F,
							mc.thePlayer.onGround));
					sendQueue.addToSendQueue(new C09PacketHeldItemChange(
						index - 36));
					mc.playerController.updateController();
					sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(
						new BlockPos(-1, -1, -1), -1, stack, 0.0F, 0.0F, 0.0F));
					sendQueue.addToSendQueue(new C09PacketHeldItemChange(
						oldslot));
					sendQueue
						.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
							mc.thePlayer.rotationYaw,
							mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
					break;
				}
		}
	}
}
