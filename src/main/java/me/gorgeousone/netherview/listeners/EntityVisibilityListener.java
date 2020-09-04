package me.gorgeousone.netherview.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.handlers.ViewHandler;
import me.gorgeousone.netherview.wrapping.boundingbox.BoundingBoxUtils;
import me.gorgeousone.netherview.wrapping.boundingbox.WrappedBoundingBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class EntityVisibilityListener {
	
	private final NetherViewPlugin main;
	private final ViewHandler viewHandler;
	private final ProtocolManager protocolManager;
	
	public EntityVisibilityListener(NetherViewPlugin main,
	                                ViewHandler viewHandler) {
		this.main = main;
		this.viewHandler = viewHandler;
		this.protocolManager = ProtocolLibrary.getProtocolManager();
		
		addEntityMoveInterception();
	}
	
	/**
	 * Toggles the visibility of entities if they are moving into or out of a player's viewing frustum
	 * of the portal they are receiving a projection from.
	 */
	private void addEntityMoveInterception() {
		
		protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGHEST, PacketType.Play.Server.REL_ENTITY_MOVE) {
			@Override
			public void onPacketSending(PacketEvent event) {
				
				PacketContainer packet = event.getPacket();
				Player player = event.getPlayer();
				Entity entity = protocolManager.getEntityFromID(player.getWorld(), packet.getIntegers().read(0));
				
				if (entity == null || !main.isEntityHidingEnabled() || !viewHandler.isViewingAPortal(player)) {
					return;
				}
				
				if (!main.isPlayerHidingEnabled() && entity.getType() == EntityType.PLAYER) {
					return;
				}
				
				ViewFrustum viewFrustum = viewHandler.getLastViewFrustum(player);
				
				if (viewFrustum == null) {
					return;
				}
				
				BlockCache cache = viewHandler.getViewedPortalSide(player);
				WrappedBoundingBox box = BoundingBoxUtils.getWrappedBoxOf(entity);
				
				if (viewHandler.getHiddenEntities(player).contains(entity)) {
					
					if (!BoundingBoxUtils.boxIntersectsFrustum(box, viewFrustum)) {
						viewHandler.showEntity(player, entity);
					}
					
				} else {
					
					if (BoundingBoxUtils.boxIntersectsBlockCache(box, cache) &&
					    BoundingBoxUtils.boxIntersectsFrustum(box, viewFrustum)) {
						viewHandler.hideEntity(player, entity);
					}
				}
			}
		});
	}
}