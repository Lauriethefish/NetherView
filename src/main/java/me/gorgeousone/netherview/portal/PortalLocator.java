package me.gorgeousone.netherview.portal;

import me.gorgeousone.netherview.Message;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.utils.FacingUtils;
import me.gorgeousone.netherview.utils.MessageException;
import me.gorgeousone.netherview.utils.MessageUtils;
import me.gorgeousone.netherview.wrapper.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class PortalLocator {
	
	private static Material PORTAL_MATERIAL;
	private static int MAX_PORTAL_SIZE;
	
	public static void configureVersion(Material portalMaterial) {
		PortalLocator.PORTAL_MATERIAL = portalMaterial;
	}
	
	public static void setMaxPortalSize(int portalSize) {
		MAX_PORTAL_SIZE = portalSize;
	}
	
	/**
	 * Finds the portal block a player might have touched at the location or the blocks next to it
	 * (players in creative mode often teleport to the nether before their location appears to be inside a portal).
	 */
	public static Block getNearbyPortalBlock(Location location) {
		
		Block block = location.getBlock();
		
		if (block.getType() == PORTAL_MATERIAL) {
			return block;
		}
		
		for (BlockFace face : FacingUtils.getAxesFaces()) {
			Block neighbor = block.getRelative(face);
			
			if (neighbor.getType() == PORTAL_MATERIAL) {
				return neighbor;
			}
		}
		
		return null;
	}
	
	public static Portal locatePortalStructure(Block portalBlock) throws MessageException {
		
		//this only happens when some data read from the portal config is wrong
		//that's why it is not a gray message
		if (portalBlock.getType() != PORTAL_MATERIAL) {
			throw new IllegalStateException("No portal block found at " + new BlockVec(portalBlock).toString());
		}
		
		World world = portalBlock.getWorld();
		AxisAlignedRect portalRect = getPortalRect(portalBlock);
		
		BlockVec portalMin = new BlockVec(portalRect.getMin());
		BlockVec portalMax = new BlockVec(portalRect.getMax());
		portalMax.add(new BlockVec(portalRect.getPlane().getNormal()));
		
		Set<Block> innerBlocks = getInnerPortalBlocks(world, portalMin, portalMax);
		
		BlockVec frameExtent = new BlockVec(portalRect.getCrossNormal());
		frameExtent.setY(1);
		
		portalMin.subtract(frameExtent);
		portalMax.add(frameExtent);
		
		Set<Block> frameBlocks = getPortalFrameBlocks(world, portalMin, portalMax, portalRect.getAxis());
		return new Portal(world, portalRect, innerBlocks, frameBlocks, portalMin, portalMax);
	}
	
	/**
	 * Returns a rectangle with the size and location of the rectangle the inner portal blocks form.
	 */
	private static AxisAlignedRect getPortalRect(Block portalBlock) throws MessageException {
		
		Axis portalAxis = FacingUtils.getAxis(portalBlock);
		
		Vector rectMin = new Vector(
				portalBlock.getX(),
				getPortalExtent(portalBlock, BlockFace.DOWN).getY(),
				portalBlock.getZ());
		
		Vector rectMax = rectMin.clone();
		rectMax.setY(getPortalExtent(portalBlock, BlockFace.UP).getY() + 1);
		
		if (portalAxis == Axis.X) {
			rectMin.setX(getPortalExtent(portalBlock, BlockFace.WEST).getX());
			rectMax.setX(getPortalExtent(portalBlock, BlockFace.EAST).getX() + 1);
			
		} else {
			rectMin.setZ(getPortalExtent(portalBlock, BlockFace.NORTH).getZ());
			rectMax.setZ(getPortalExtent(portalBlock, BlockFace.SOUTH).getZ() + 1);
		}
		
		//translate the portalRect towards the middle of the block;
		AxisAlignedRect portalRect = new AxisAlignedRect(portalAxis, rectMin, rectMax);
		
		if (portalRect.width() > MAX_PORTAL_SIZE || portalRect.height() > MAX_PORTAL_SIZE) {
			throw new MessageException(Message.PORTAL_TOO_BIG, String.valueOf(MAX_PORTAL_SIZE));
		}
		
		portalRect.translate(portalRect.getPlane().getNormal().multiply(0.5));
		return portalRect;
	}
	
	/**
	 * Returns the last portal block of a portal's extent into a certain direction.
	 */
	private static Block getPortalExtent(Block sourceBlock, BlockFace facing) throws MessageException {
		
		Block blockIterator = sourceBlock;
		
		for (int i = 0; i <= MAX_PORTAL_SIZE; i++) {
			
			Block nextBlock = blockIterator.getRelative(facing);
			
			if (nextBlock.getType() != PORTAL_MATERIAL) {
				return blockIterator;
			}
			
			blockIterator = nextBlock;
		}
		
		MessageUtils.printDebug("Detection stopped after exceeding " + MAX_PORTAL_SIZE + " portal blocks towards " + facing.name() + " at " + new BlockVec(blockIterator).toString());
		throw new MessageException(Message.PORTAL_TOO_BIG, String.valueOf(MAX_PORTAL_SIZE));
	}
	
	/**
	 * Returns a set of blocks of all portal blocks of a portal according to the passed rectangle.
	 */
	private static Set<Block> getInnerPortalBlocks(World world,
	                                               BlockVec portalMin,
	                                               BlockVec portalMax) throws MessageException {
		
		Set<Block> portalBlocks = new HashSet<>();
		
		for (int x = portalMin.getX(); x < portalMax.getX(); x++) {
			for (int y = portalMin.getY(); y < portalMax.getY(); y++) {
				for (int z = portalMin.getZ(); z < portalMax.getZ(); z++) {
					
					Block portalBlock = world.getBlockAt(x, y, z);
					
					if (portalBlock.getType() == PORTAL_MATERIAL) {
						portalBlocks.add(portalBlock);
						
					} else {
						
						MessageUtils.printDebug("Expected portal block at " + new BlockVec(x, y, z).toString());
						String worldType = world.getEnvironment().name().toLowerCase().replaceAll("_", " ");
						throw new MessageException(Message.PORTAL_NOT_INTACT, worldType);
					}
				}
			}
		}
		
		return portalBlocks;
	}
	
	/**
	 * Returns a set of blocks where obsidian needs to be placed for a portal frame according to the given portal bounds.
	 */
	private static Set<Block> getPortalFrameBlocks(World world,
	                                               BlockVec portalMin,
	                                               BlockVec portalMax,
	                                               Axis portalAxis) throws MessageException {
		
		Set<Block> frameBlocks = new HashSet<>();
		
		int portalMinX = portalMin.getX();
		int portalMinY = portalMin.getY();
		int portalMinZ = portalMin.getZ();
		int portalMaxX = portalMax.getX();
		int portalMaxY = portalMax.getY();
		int portalMaxZ = portalMax.getZ();
		
		for (int x = portalMinX; x < portalMaxX; x++) {
			for (int y = portalMinY; y < portalMaxY; y++) {
				for (int z = portalMinZ; z < portalMaxZ; z++) {
					
					//only check for obsidian frame blocks that are at the border of this "flat cuboid"
					if (y > portalMinY && y < portalMaxY - 1 &&
					    (portalAxis == Axis.X ? (x > portalMinX) : (z > portalMinZ)) &&
					    (portalAxis == Axis.X ? (x < portalMaxX - 1) : (z < portalMaxZ - 1))) {
						continue;
					}
					
					Block portalBlock = world.getBlockAt(x, y, z);
					
					if (portalBlock.getType() == Material.OBSIDIAN) {
						frameBlocks.add(portalBlock);
						
					} else if ((((portalAxis == Axis.X) && ((x == portalMinX     && y == portalMinY    ) ||
					                                        (x == portalMinX     && y == portalMaxY - 1) ||
					                                        (x == portalMaxX - 1 && y == portalMinY    ) ||
					                                        (x == portalMaxX - 1 && y == portalMaxY - 1))) ||
					            ((portalAxis == Axis.Z) && ((z == portalMinZ     && y == portalMinY    ) ||
					                                        (z == portalMinZ     && y == portalMaxY - 1) ||
					                                        (z == portalMaxZ - 1 && y == portalMinY    ) ||
					                                        (z == portalMaxZ - 1 && y == portalMaxY - 1)))) &&
					           portalBlock.getType().isOccluding()) {
						//allow corner blocks to be any occluding block
						frameBlocks.add(portalBlock);
						
					} else {
						
						MessageUtils.printDebug("Expected obsidian/occluding block at "
						                        + portalBlock.getWorld().getName() + ", "
						                        + new BlockVec(portalBlock).toString());
						
						String worldType = world.getEnvironment().name().toLowerCase().replaceAll("_", " ");
						throw new MessageException(Message.PORTAL_FRAME_INCOMPLETE, worldType);
					}
				}
			}
		}
		
		return frameBlocks;
	}
}