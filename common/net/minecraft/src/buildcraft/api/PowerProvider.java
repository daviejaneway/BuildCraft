/** 
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package net.minecraft.src.buildcraft.api;

import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;

public abstract class PowerProvider {

	public int latency;
	public int minEnergyReceived;
	public int maxEnergyReceived;
	public int maxEnergyStored;
	public int minActivationEnergy;
	public @TileNetworkData
	float energyStored = 0;

	protected int powerLoss = 1;
	protected int powerLossRegularity = 100;

	public SafeTimeTracker timeTracker = new SafeTimeTracker();
	public SafeTimeTracker energyLossTracker = new SafeTimeTracker();

	public int[] powerSources = { 0, 0, 0, 0, 0, 0 };

	public void configure(int latency, int minEnergyReceived, int maxEnergyReceived, int minActivationEnergy, int maxStoredEnergy) {
		this.latency = latency;
		this.minEnergyReceived = minEnergyReceived;
		this.maxEnergyReceived = maxEnergyReceived;
		this.maxEnergyStored = maxStoredEnergy;
		this.minActivationEnergy = minActivationEnergy;
	}

	public void configurePowerPerdition(int powerLoss, int powerLossRegularity) {
		this.powerLoss = powerLoss;
		this.powerLossRegularity = powerLossRegularity;
	}

	public final boolean update(IPowerReceptor receptor) {
		if (!preConditions(receptor)) {
			return false;
		}

		TileEntity tile = (TileEntity) receptor;
		boolean result = false;

		if (energyStored >= minActivationEnergy) {
			if (latency == 0) {
				receptor.doWork();
				result = true;
			} else {
				if (timeTracker.markTimeIfDelay(tile.worldObj, latency)) {
					receptor.doWork();
					result = true;
				}
			}
		}

		if (powerLoss > 0 && energyLossTracker.markTimeIfDelay(tile.worldObj, powerLossRegularity)) {

			energyStored -= powerLoss;
			if (energyStored < 0) {
				energyStored = 0;
			}
		}

		for (int i = 0; i < 6; ++i) {
			if (powerSources[i] > 0) {
				powerSources[i]--;
			}
		}

		return result;
	}

	public boolean preConditions(IPowerReceptor receptor) {
		return true;
	}

	public float useEnergy(float min, float max, boolean doUse) {
		float result = 0;

		if (energyStored >= min) {
			if (energyStored <= max) {
				result = energyStored;
				if (doUse) {
					energyStored = 0;
				}
			} else {
				result = max;
				if (doUse) {
					energyStored -= max;
				}
			}
		}

		return result;
	}

	public void readFromNBT(NBTTagCompound nbttagcompound) {
		latency = nbttagcompound.getInteger("latency");
		minEnergyReceived = nbttagcompound.getInteger("minEnergyReceived");
		maxEnergyReceived = nbttagcompound.getInteger("maxEnergyReceived");
		maxEnergyStored = nbttagcompound.getInteger("maxStoreEnergy");
		minActivationEnergy = nbttagcompound.getInteger("minActivationEnergy");

		try {
			energyStored = nbttagcompound.getFloat("storedEnergy");
		} catch (Throwable c) {
			energyStored = 0;
		}
	}

	public void writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setInteger("latency", latency);
		nbttagcompound.setInteger("minEnergyReceived", minEnergyReceived);
		nbttagcompound.setInteger("maxEnergyReceived", maxEnergyReceived);
		nbttagcompound.setInteger("maxStoreEnergy", maxEnergyStored);
		nbttagcompound.setInteger("minActivationEnergy", minActivationEnergy);
		nbttagcompound.setFloat("storedEnergy", energyStored);
	}

	public void receiveEnergy(float quantity, Orientations from) {
		powerSources[from.ordinal()] = 2;

		energyStored += quantity;

		if (energyStored > maxEnergyStored) {
			energyStored = maxEnergyStored;
		}
	}

	public boolean isPowerSource(Orientations from) {
		return powerSources[from.ordinal()] != 0;
	}
}
