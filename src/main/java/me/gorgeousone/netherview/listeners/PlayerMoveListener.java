package me.gorgeousone.netherview.listeners;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.handlers.ViewHandler;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {
	
	private NetherView main;
	private ViewHandler viewHandler;
	
	public PlayerMoveListener(NetherView main, ViewHandler viewHandler) {
		this.main = main;
		this.viewHandler = viewHandler;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		Player player = event.getPlayer();
		
		if (player.getGameMode() == GameMode.SPECTATOR || !player.hasPermission(NetherView.VIEW_PERM)) {
			return;
		}
		
		World playerWorld = player.getWorld();
		
		if (playerWorld.getEnvironment() == World.Environment.THE_END || !main.canCreatePortalViews(playerWorld)) {
			return;
		}
		
		Location from = event.getFrom();
		Location to = event.getTo();
		
		if (!from.toVector().equals(to.toVector())) {
			
			Vector playerMovement = to.clone().subtract(from).toVector();
			viewHandler.displayNearestPortalTo(player, player.getEyeLocation().add(playerMovement));
		}
	}
	
	@EventHandler
	public void onPlayerSneak(PlayerToggleSneakEvent event) {
		
		Player player = event.getPlayer();
		
		if (viewHandler.hasViewSession(player)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					viewHandler.displayNearestPortalTo(player, player.getEyeLocation());
				}
			}.runTaskLater(main, 2);
		}
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		
		if (event.getPlayer().hasPermission(NetherView.VIEW_PERM) && event.getNewGameMode() == GameMode.SPECTATOR) {
			viewHandler.hideViewSession(event.getPlayer());
		}
	}
}