package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IXmlRepresentableUnit;
import cr0s.warpdrive.data.JumpBlock;
import org.w3c.dom.Element;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Represents a single filler block.
 **/
public class Filler implements IXmlRepresentableUnit {
	
	public static final Filler DEFAULT;
	static {
		DEFAULT = new Filler();
		DEFAULT.name           = "-default-";
		DEFAULT.block          = Blocks.air;
		DEFAULT.metadata       = 0;
		DEFAULT.tagCompound    = null;
	}
	
	private String name;
	public Block block;
	public int metadata;
	public NBTTagCompound tagCompound = null;
	
	@Override
	public String getName() {
		return name;
	}
	
	public Filler() {
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		
		// Check there is a block name
		if (!element.hasAttribute("block")) {
			throw new InvalidXmlException("Filler " + element + " is missing a block attribute!");
		}
		
		final String nameBlock = element.getAttribute("block");
		block = Block.getBlockFromName(nameBlock);
		if (block == null) {
			WarpDrive.logger.warn("Skipping missing block " + nameBlock);
			return false;
		}
		
		// Get metadata attribute, defaults to 0
		metadata = 0;
		final String stringMetadata = element.getAttribute("metadata");
		if (!stringMetadata.isEmpty()) {
			try {
				metadata = Integer.parseInt(stringMetadata);
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException("Invalid metadata for block " + nameBlock);
			}
		}
		
		// Get nbt attribute, default to null/none
		tagCompound = null;
		final String stringNBT = element.getAttribute("nbt");
		if (!stringNBT.isEmpty()) {
			try {
				tagCompound = (NBTTagCompound) JsonToNBT.func_150315_a(stringNBT);
			} catch (final NBTException exception) {
				throw new InvalidXmlException("Invalid nbt for block " + nameBlock);
			}
		}
		
		name = nameBlock + "@" + metadata + "{" + tagCompound + "}";
		
		return true;
	}

	public void setBlock(final World world, final int x, final int y, final int z) {
		JumpBlock.setBlockNoLight(world, x, y, z, block, metadata, 2);
		
		if (tagCompound != null) {
			final TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity == null) {
				WarpDrive.logger.error("No TileEntity found for Filler %s at (%d %d %d)",
				                       getName(),
				                       x, y, z);
				return;
			}
			
			final NBTTagCompound nbtTagCompoundTileEntity = new NBTTagCompound();
			tileEntity.writeToNBT(nbtTagCompoundTileEntity);
			
			for (final Object key : tagCompound.func_150296_c()) {
				if (key instanceof String) {
					nbtTagCompoundTileEntity.setTag((String) key, tagCompound.getTag((String) key));
				}
			}
			
			tileEntity.onChunkUnload();
			tileEntity.readFromNBT(nbtTagCompoundTileEntity);
			tileEntity.validate();
			tileEntity.markDirty();
			
			JumpBlock.refreshBlockStateOnClient(world, x, y, z);
		}
	}
	
	@Override
	public IXmlRepresentableUnit constructor() {
		return new Filler();
	}
	
	@Override
	public boolean equals(final Object object) {
		return object instanceof Filler
			&& (block == null || block.equals(((Filler)object).block))
			&& metadata == ((Filler)object).metadata
			&& (tagCompound == null || tagCompound.equals(((Filler)object).tagCompound));
	}
	
	@Override
	public String toString() {
		return "Filler(" + block.getUnlocalizedName() + "@" + metadata + ")";
	}

	@Override
	public int hashCode() {
		return Block.getIdFromBlock(block) * 16 + metadata + (tagCompound == null ? 0 : tagCompound.hashCode() * 4096 * 16);
	}
}
