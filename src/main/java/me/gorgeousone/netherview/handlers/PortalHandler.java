package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PortalHandler {
	
	private NetherView main;
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<Portal, Set<Portal>> linkedPortals;
	private Map<BlockCache, Set<ProjectionCache>> linkedProjections;
	
	public PortalHandler(NetherView main) {
		
		this.main = main;
		
		worldsWithPortals = new HashMap<>();
		linkedPortals = new HashMap<>();
		linkedProjections = new HashMap<>();
	}
	
	public void resetCaches() {
	
//		for (UUID worldID : worldsWithPortals.keySet()) {
//			for (Portal portal : worldsWithPortals.get(worldID)) {
//
//			}
//		}
		
		worldsWithPortals.clear();
		linkedPortals.clear();
	}
	
	public Set<Portal> getPortals(World world) {
		return worldsWithPortals.getOrDefault(world.getUID(), new HashSet<>());
	}
	
	public boolean hasPortals(World world) {
		return worldsWithPortals.containsKey(world.getUID());
	}
	
	/**
	 * Returns the first portal that contains the passed block as part of the portal surface. Returns null if none was found.
	 */
	public Portal getPortalByBlock(Block portalBlock) {
		
		for (Portal portal : getPortals(portalBlock.getWorld())) {
			if (portal.getPortalBlocks().contains(portalBlock)) {
				return portal;
			}
		}
		
		return null;
	}
	
	public Portal getPortalByHashCode(int portalHashCode) {
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			for (Portal portal : worldsWithPortals.get(worldID)) {
				
				if (portal.hashCode() == portalHashCode) {
					return portal;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the the nearest portal in a world to the passed Location. Returns null if none was found.
	 * @param mustBeLinked specify if the returned portal should be linked already
	 */
	public Portal getNearestPortal(Location playerLoc, boolean mustBeLinked) {
		
		World playerWorld = playerLoc.getWorld();
		
		if (!main.canCreatePortalsViews(playerWorld)) {
			return null;
		}
		
		Portal nearestPortal = null;
		double minDist = -1;
		
		for (Portal portal : getPortals(playerLoc.getWorld())) {
			
			if (mustBeLinked && !portal.isLinked()) {
				continue;
			}
			
			double dist = portal.getLocation().distanceSquared(playerLoc);
			
			if (nearestPortal == null || dist < minDist) {
				nearestPortal = portal;
				minDist = dist;
			}
		}
		
		return nearestPortal;
	}
	
	/**
	 * Returns a Set of all portals connected with their projections to the passed portal. Returns an empty set if none was found.
	 */
	public Set<Portal> getPortalsLinkedTo(Portal portal) {
		return new HashSet<>(linkedPortals.getOrDefault(portal, new HashSet<>()));
	}
	
	/**
	 * Returns all block caches (2 for each portal) of all portals in specified world.
	 */
	public Set<BlockCache> getBlockCaches(World world) {
		
		Set<BlockCache> caches = new HashSet<>();
		
		for (Portal portal : getPortals(world)) {
			
			if (portal.areCachesLoaded()) {
				caches.add(portal.getFrontCache());
				caches.add(portal.getBackCache());
			}
		}
		
		return caches;
	}
	
	/**
	 * Returns a Set of projection caches that are not connected to a portal but to a specific block cache (one of two for a portal).
	 * Returns an empty Set if none were found.
	 */
	public Set<ProjectionCache> getProjectionsLinkedTo(BlockCache cache) {
		return linkedProjections.getOrDefault(cache, new HashSet<>());
	}
	
	/**
	 * Locates and registers a new portal
	 * @param portalBlock one block of the structure required to detect the rest of it
	 */
	public Portal addPortalStructure(Block portalBlock) {
		
		Portal portal = PortalLocator.locatePortalStructure(portalBlock);
		UUID worldID = portal.getWorld().getUID();
		
		worldsWithPortals.putIfAbsent(worldID, new HashSet<>());
		worldsWithPortals.get(worldID).add(portal);
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "Debug: Portal located at " + portal.toString());
		}
		
		return portal;
	}
	
	/**
	 * Removes all references to a registered portal
	 */
	public void removePortal(Portal portal) {
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "Debug: Removing portal at " + portal.toString());
		}
		
		//unlink the portal
		if (portal.isLinked()) {
			Portal counterPortal = portal.getCounterPortal();
			linkedProjections.get(counterPortal.getFrontCache()).remove(portal.getBackProjection());
			linkedProjections.get(counterPortal.getBackCache()).remove(portal.getFrontProjection());
		}
		
		//unlink any portal linked to this portal
		if (linkedPortals.containsKey(portal)) {
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "Debug: Un-linking " + linkedPortals.get(portal).size() + " other portals.");
			}
			
			for (Portal linkedPortal : linkedPortals.get(portal))
				linkedPortal.unlink();
			
			linkedProjections.remove(portal.getFrontCache());
			linkedProjections.remove(portal.getBackCache());
		}
		
		linkedPortals.remove(portal);
		getPortals(portal.getWorld()).remove(portal);
	}
	
	/**
	 * Links a portal to it's counter portal it will teleport to.
	 */
	public void linkPortalTo(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal)) {
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "Debug: Cannot connect portal with size "
				                        + (int) portal.getPortalRect().width() + "x" + (int) portal.getPortalRect().height() + " to portal of size "
                                        + (int) counterPortal.getPortalRect().width() + "x" + (int) counterPortal.getPortalRect().height());
			}
			
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are not the same size, it is difficult to get a clear view...");
		}
		
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		
		if (!counterPortal.areCachesLoaded()) {
			counterPortal.setBlockCaches(BlockCacheFactory.createBlockCaches(counterPortal, main.getPortalProjectionDist()));
		}
		
		BlockCache cache1 = counterPortal.getFrontCache();
		BlockCache cache2 = counterPortal.getBackCache();
		
		//the projections caches are switching positions because of to the rotation transform
		ProjectionCache projection1 = new ProjectionCache(portal, cache2, linkTransform);
		ProjectionCache projection2 = new ProjectionCache(portal, cache1, linkTransform);
		
		portal.setLinkedTo(counterPortal, new AbstractMap.SimpleEntry<>(projection1, projection2));
		
		linkedPortals.putIfAbsent(counterPortal, new HashSet<>());
		linkedPortals.get(counterPortal).add(portal);
		
		linkedProjections.putIfAbsent(cache1, new HashSet<>());
		linkedProjections.putIfAbsent(cache2, new HashSet<>());
		linkedProjections.get(cache1).add(projection2);
		linkedProjections.get(cache2).add(projection1);
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "Debug: Linked portal from "
			                        + portal.toString() + " to portal at "
			                        + counterPortal.toString());
		}
	}
	
	public void savePortals(FileConfiguration portalConfig) {
		
		ConfigurationSection portalLocations = portalConfig.createSection("portal-locations");
		ConfigurationSection portalLinks = portalConfig.createSection("linked-portals");
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			
			List<String> portalsInWorld = new ArrayList<>();
			
			for (Portal portal : worldsWithPortals.get(worldID)) {
				
				portalsInWorld.add(new BlockVec(portal.getLocation()).toString());
				
				if (portal.isLinked()) {
					portalLinks.set(String.valueOf(portal.hashCode()), portal.getCounterPortal().hashCode());
				}
			}
			
			portalLocations.set(worldID.toString(), portalsInWorld);
		}
	}
	
	public void loadPortals(FileConfiguration portalConfig) {
	
		if (!portalConfig.contains("portal-locations")) {
			return;
		}
		
		ConfigurationSection portalLocations = portalConfig.getConfigurationSection("portal-locations");
		
		for (String worldID : portalLocations.getKeys(false)) {
		
			World worldWithPortals = Bukkit.getWorld(UUID.fromString(worldID));
			
			if (worldWithPortals == null) {
				System.out.println("World with ID: '" + worldID + "' could not be found. Portals from this world will not be loaded.");
				continue;
			}
			
			List<String> portalBlocksLocs = portalLocations.getStringList(worldID);
			
			for (String serializedBlockVec : portalBlocksLocs) {
				
				try {
					BlockVec portalLoc = BlockVec.fromString(serializedBlockVec);
					addPortalStructure(worldWithPortals.getBlockAt(portalLoc.getX(), portalLoc.getY(), portalLoc.getZ()));
					
				}catch (IllegalArgumentException | IllegalStateException e) {
					Bukkit.getConsoleSender().sendMessage("Unable to load portal at " + serializedBlockVec + ": " + e.getMessage());
				}
			}
		}
	}
	
	public void loadPortalLinks(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("linked-portals")) {
			return;
		}
		
		ConfigurationSection portalLinks = portalConfig.getConfigurationSection("linked-portals");
		
		for (String portalHashString : portalLinks.getKeys(false)) {
			
			Portal portal = getPortalByHashCode(Integer.parseInt(portalHashString));
			Portal counterPortal = getPortalByHashCode(portalLinks.getInt(portalHashString));
			
			if(portal != null && counterPortal != null) {
				linkPortalTo(portal, counterPortal);
			}
		}
	}
	
	/**
	 * Calculates a Transform that is needed to translate and rotate block types at the positions of the block cache
	 * of the counter portal to the related position in the projection cache of the portal.
	 */
	private Transform calculateLinkTransform(Portal portal, Portal counterPortal) {
		
		Transform linkTransform;
		Vector distance = portal.getLocation().toVector().subtract(counterPortal.getLocation().toVector());
		
		linkTransform = new Transform();
		linkTransform.setTranslation(new BlockVec(distance));
		linkTransform.setRotCenter(new BlockVec(counterPortal.getPortalRect().getMin()));
		
		//during the rotation some weird shifts happen
		//I did not figure out where they come from, for now some extra translations are a good workaround
		if (portal.getAxis() == counterPortal.getAxis()) {
			
			linkTransform.setRotY180Deg();
			int portalBlockWidth = (int) portal.getPortalRect().width() - 1;
			
			if (counterPortal.getAxis() == Axis.X) {
				linkTransform.translate(new BlockVec(portalBlockWidth, 0, 0));
			} else {
				linkTransform.translate(new BlockVec(0, 0, portalBlockWidth));
			}
			
		} else if (counterPortal.getAxis() == Axis.X) {
			linkTransform.setRotY90DegRight();
			linkTransform.translate(new BlockVec(0, 0, 1));
			
		} else {
			linkTransform.setRotY90DegLeft();
			linkTransform.translate(new BlockVec(1, 0, 0));
		}
		
		return linkTransform;
	}
}