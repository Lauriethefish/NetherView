package me.gorgeousone.netherview.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NmsUtils {
	
	private static final String VERSION = Bukkit.getServer().getClass().getName().split("\\.")[3];
	private static Method ENTITY_GET_HANDLE;
	private static Method ENTITY_GET_DATA_WATCHER;
	
	static {
		try {
			ENTITY_GET_HANDLE = NmsUtils.getCraftBukkitClass("entity.CraftEntity").getMethod("getHandle");
			ENTITY_GET_DATA_WATCHER = NmsUtils.getNmsClass("Entity").getMethod("getDataWatcher");
		} catch (NoSuchMethodException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static Object getHandle(Entity entity) throws InvocationTargetException, IllegalAccessException {
		return ENTITY_GET_HANDLE.invoke(entity);
	}
	
	public static Object getDataWatcher(Entity entity) throws InvocationTargetException, IllegalAccessException {
		return ENTITY_GET_DATA_WATCHER.invoke(getHandle(entity));
	}
	
	public static Class<?> getNmsClass(String nmsClassString) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + VERSION + "." + nmsClassString);
	}
	
	public static Class<?> getCraftBukkitClass(String cbClassString) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + VERSION + "." + cbClassString);
	}
}
