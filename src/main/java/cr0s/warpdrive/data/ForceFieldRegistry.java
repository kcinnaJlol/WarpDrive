package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldRelay;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

/**
 * Registry of all known force field blocks in the loaded worlds, grouped by frequency
 * 
 * @author LemADEC
 */
public class ForceFieldRegistry {
	
	private static final HashMap<Integer, CopyOnWriteArraySet<GlobalPosition>> registry = new HashMap<>();
	private static int countAdd = 0;
	private static int countRemove = 0;
	private static int countRead = 0;
	
	public static Set<TileEntity> getTileEntities(final int beamFrequency, final WorldServer worldSource, final int x, final int y, final int z) {
		countRead++;
		if (WarpDriveConfig.LOGGING_FORCEFIELD_REGISTRY) {
			if (countRead % 1000 == 0) {
				WarpDrive.logger.info("ForceFieldRegistry stats: read " + countRead + " add " + countAdd + " remove " + countRemove + " => " + ((float) countRead) / (countRemove + countRead + countAdd) + "% read");
			}
		}
		final CopyOnWriteArraySet<GlobalPosition> setGlobalPositions = registry.get(beamFrequency);
		if (setGlobalPositions == null || worldSource == null) {
			return new CopyOnWriteArraySet<>();
		}
		// find all relevant tiles by world and frequency, keep relays in range as starting point
		final Set<TileEntity> setNonRelays = new HashSet<>();
		final Set<TileEntity> setRelays = new HashSet<>();
		Set<TileEntity> setToIterate = new HashSet<>();
		int range2;
		final int maxRange2 = ForceFieldSetup.FORCEFIELD_RELAY_RANGE * ForceFieldSetup.FORCEFIELD_RELAY_RANGE;
		for (final GlobalPosition globalPosition : setGlobalPositions) {
			final WorldServer world = globalPosition.getWorldServerIfLoaded();
			if (world != null) {
				// skip if it's in another dimension
				if (world != worldSource) {
					continue;
				}
				
				// confirm frequency and split by groups
				final TileEntity tileEntity = world.getTileEntity(globalPosition.x, globalPosition.y, globalPosition.z);
				if ( (tileEntity instanceof IBeamFrequency)
				  && ((IBeamFrequency)tileEntity).getBeamFrequency() == beamFrequency ) {
					if (tileEntity instanceof TileEntityForceFieldRelay) {
						// add relays in range as start point
						range2 = (globalPosition.x - x) * (globalPosition.x - x) + (globalPosition.y - y) * (globalPosition.y - y) + (globalPosition.z - z) * (globalPosition.z - z);
						if (range2 <= maxRange2) {
							setToIterate.add(tileEntity);
						} else {
							setRelays.add(tileEntity);
						}
					} else {
						setNonRelays.add(tileEntity);
					}
					continue;
				}
			}
			// world isn't loaded or block no longer exist => remove from registry
			countRemove++;
			setGlobalPositions.remove(globalPosition);
			if (WarpDriveConfig.LOGGING_FORCEFIELD_REGISTRY) {
				printRegistry("removed");
			}
		}
		
		// no relays in range => just add that one block
		if (setToIterate.isEmpty()) {
			final Set<TileEntity> setResult = new HashSet<>();
			setResult.add(worldSource.getTileEntity(x, y, z));
			return setResult;
		}
		
		// find all relays in that network
		Set<TileEntity> setToIterateNext;
		final Set<TileEntity> setRelaysInRange = new HashSet<>();
		while(!setToIterate.isEmpty()) {
			setToIterateNext = new HashSet<>();
			for (final TileEntity tileEntityCurrent : setToIterate) {
				setRelaysInRange.add(tileEntityCurrent);
				for (final TileEntity tileEntityEntry : setRelays) {
					if (!setRelaysInRange.contains(tileEntityEntry) && !setToIterate.contains(tileEntityEntry) && !setToIterateNext.contains(tileEntityEntry)) {
						range2 = (tileEntityCurrent.xCoord - tileEntityEntry.xCoord) * (tileEntityCurrent.xCoord - tileEntityEntry.xCoord)
						       + (tileEntityCurrent.yCoord - tileEntityEntry.yCoord) * (tileEntityCurrent.yCoord - tileEntityEntry.yCoord)
						       + (tileEntityCurrent.zCoord - tileEntityEntry.zCoord) * (tileEntityCurrent.zCoord - tileEntityEntry.zCoord);
						if (range2 <= maxRange2) {
							setToIterateNext.add(tileEntityEntry);
						}
					}
				}
			}
			setToIterate = setToIterateNext;
		}
		
		// find all non-relays in range of that network
		final Set<TileEntity> setEntries = new HashSet<>();
		for (final TileEntity tileEntityRelayInRange : setRelaysInRange) {
			for (final TileEntity tileEntityEntry : setNonRelays) {
				if (!setEntries.contains(tileEntityEntry)) {
					range2 = (tileEntityRelayInRange.xCoord - tileEntityEntry.xCoord) * (tileEntityRelayInRange.xCoord - tileEntityEntry.xCoord)
					       + (tileEntityRelayInRange.yCoord - tileEntityEntry.yCoord) * (tileEntityRelayInRange.yCoord - tileEntityEntry.yCoord)
					       + (tileEntityRelayInRange.zCoord - tileEntityEntry.zCoord) * (tileEntityRelayInRange.zCoord - tileEntityEntry.zCoord);
					if (range2 <= maxRange2) {
						setEntries.add(tileEntityEntry);
					}
				}
			}
		}
		setEntries.addAll(setRelaysInRange);
		return setEntries;
	}
	
	public static void updateInRegistry(final IBeamFrequency tileEntity) {
		assert(tileEntity instanceof TileEntity);
		
		countRead++;
		CopyOnWriteArraySet<GlobalPosition> setGlobalPositions = registry.get(tileEntity.getBeamFrequency());
		if (setGlobalPositions == null) {
			setGlobalPositions = new CopyOnWriteArraySet<>();
		}
		for (final GlobalPosition globalPosition : setGlobalPositions) {
			if (globalPosition.equals(tileEntity)) {
				// already registered
				return;
			}
		}
		// not found => add
		countAdd++;
		setGlobalPositions.add(new GlobalPosition((TileEntity) tileEntity));
		registry.put(tileEntity.getBeamFrequency(), setGlobalPositions);
		if (WarpDriveConfig.LOGGING_FORCEFIELD_REGISTRY) {
			printRegistry("added");
		}
	}
	
	public static void removeFromRegistry(final IBeamFrequency tileEntity) {
		assert(tileEntity instanceof TileEntity);
		
		countRead++;
		final Set<GlobalPosition> setGlobalPositions = registry.get(tileEntity.getBeamFrequency());
		if (setGlobalPositions == null) {
			// noting to remove
			return;
		}
		for (final GlobalPosition globalPosition : setGlobalPositions) {
			if (globalPosition.equals(tileEntity)) {
				// found it, remove and exit
				countRemove++;
				setGlobalPositions.remove(globalPosition);
				return;
			}
		}
		// not found => ignore it
	}
	
	public static void printRegistry(final String trigger) {
		WarpDrive.logger.info("Force field registry (" + registry.size() + " entries after " + trigger + "):");
		
		for (final Map.Entry<Integer, CopyOnWriteArraySet<GlobalPosition>> entry : registry.entrySet()) {
			final StringBuilder message = new StringBuilder();
			for (final GlobalPosition globalPosition : entry.getValue()) {
				if (message.length() > 0) {
					message.append(", ");
				}
				message.append(globalPosition.dimensionId).append(": ").append(globalPosition.x).append(" ").append(globalPosition.y).append(" ").append(globalPosition.z);
			}
			WarpDrive.logger.info(String.format("- %d entries at frequency %d : %s",
			                                    entry.getValue().size(),
			                                    entry.getKey(),
			                                    message));
		}
	}
}
