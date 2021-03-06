package me.gorgeousone.netherview.message;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtils {
	
	private MessageUtils() {}
	
	public static boolean debugMessagesEnabled;
	public static boolean warningMessagesEnabled;
	
	public static void setWarningMessagesEnabled(boolean warningMessagesEnabled) {
		MessageUtils.warningMessagesEnabled = warningMessagesEnabled;
	}
	
	public static void setDebugMessagesEnabled(boolean debugMessagesEnabled) {
		MessageUtils.debugMessagesEnabled = debugMessagesEnabled;
	}
	
	public static void sendInfo(CommandSender sender, Message message, String... placeholderValues) {
		sender.sendMessage(message.getMessage(placeholderValues));
	}
	
	public static void sendWarning(CommandSender sender, Message message, String... placeholderValues) {
		
		if (warningMessagesEnabled) {
			sender.sendMessage(message.getMessage(placeholderValues));
		}
	}
	
	public static void printDebug(String message) {
		
		if (debugMessagesEnabled) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] " + message);
		}
	}
	
	public static void sendStaffInfo(String message) {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			
			if (player.isOp()) {
				player.sendMessage(message);
			}
		}
		
		Bukkit.getConsoleSender().sendMessage(message);
	}
	
	public static void sendStaffInfo(BaseComponent[] message) {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			
			if (player.isOp()) {
				player.spigot().sendMessage(message);
			}
		}
		
		Bukkit.getConsoleSender().spigot().sendMessage(message);
	}
}
