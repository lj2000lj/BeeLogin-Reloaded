package cn.apisium.util.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.google.common.collect.Lists;

public class NmsHelper {
	public static Method findMethodByType(Class<?> original, Class<?> returnType, Class<?>... parameters) {
		Methods: for (Method method : original.getDeclaredMethods()) {
			if (method.getReturnType().getName().equals(original.getName())
					&& parameters.length == method.getParameterCount()) {
				for (int i = 0; i < parameters.length; i++) {
					if (!parameters[i].getName().equals(method.getParameters()[i].getName())) {
						continue Methods;
					}
				}
				return method;
			}
		}
		return null;
	}

	public static Field findFirstFieldByType(Class<?> original, Class<?> type) {
		for (Field method : original.getDeclaredFields()) {
			if (method.getType().getName().equals(original.getName()))
				return method;
		}
		return null;
	}

	public static List<Object> getPossibleLoginListeners(InetAddress address, UUID uuid) {
		List<Object> listeners = new ArrayList<>();
		Class<?> loginListener;
		Object login = null;
		for (Object manager : getNetworkManagers()) {
			try {
				loginListener = NmsHelper.getNmsClass("LoginListener");
				if (!((InetSocketAddress) NmsHelper.findFirstFieldByType(manager.getClass(), SocketAddress.class)
						.get(manager)).getAddress().equals(address)) {
					continue;
				}
				if (!NmsHelper.findFirstFieldByType(manager.getClass(), UUID.class).equals(uuid)) {
					continue;
				}
				Class<?> packetListenerClass = NmsHelper.getNmsClass("PacketListener");
				login = NmsHelper.findMethodByType(manager.getClass(), packetListenerClass, new Class[0])
						.invoke(manager, new Object[0]);
				listeners.add(login);
				if (!loginListener.isInstance(login)) {
					continue;
				}
			} catch (Exception e) {
				throw new RuntimeException(
						"Can not get LoginListener instance,  possibily because it is not a craftbukkit implantation");

			}
		}
		if (listeners.isEmpty())
			throw new RuntimeException(
					"Can not get LoginListener instance,  possibily because it is not a craftbukkit implantation");

		return listeners;
	}

	public static List<?> getNetworkManagers() {
		List<?> managers = Collections.synchronizedList(Lists.newArrayList());
		for (Field f : getServerConnection().getClass().getDeclaredFields()) {
			if (!f.isAccessible()) {
				f.setAccessible(true);
			}
			try {
				if ((f.getType().isAssignableFrom(managers.getClass()))) {
					List<?> original = (List<?>) f.get(getNmsServer());
					((Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0])
							.isAssignableFrom(NmsHelper.getNmsClass("NetworkManager"));
					managers = original;
					break;
				}
			} catch (IllegalArgumentException | IllegalAccessException | ClassNotFoundException e) {
				throw new RuntimeException(
						"Can not get NetworkManager instance,  possibily because it is not a craftbukkit implantation");
			}
		}
		return managers;
	}

	public static Object getServerConnection() {
		Object mc = getNmsServer();
		Object serverConnection = null;
		for (Field f : mc.getClass().getDeclaredFields()) {
			if (!f.isAccessible()) {
				f.setAccessible(true);
			}
			try {
				if (!(f.getType().isAssignableFrom(NmsHelper.getNmsClass("ServerConnection")))) {
					continue;
				}
				serverConnection = f.get(mc);
				break;
			} catch (IllegalArgumentException | IllegalAccessException | ClassNotFoundException e) {
				throw new RuntimeException(
						"Can not get ServerConnection instance,  possibily because it is not a craftbukkit implantation");
			}
		}
		if (serverConnection == null) {
			throw new RuntimeException(
					"Can not get ServerConnection instance,  possibily because it is not a craftbukkit implantation");
		}
		return serverConnection;
	}

	public static Object getNmsServer() {
		try {
			Class<?> craftServerClass = getCraftClass("CraftServer");
			Object craftServer = craftServerClass.cast(Bukkit.getServer());
			Class<?> nmsServerClass = craftServer.getClass();
			Method getServerMethod = nmsServerClass.getDeclaredMethod("getServer", new Class[0]);
			if (!getServerMethod.isAccessible()) {
				getServerMethod.setAccessible(true);
			}
			return getServerMethod.invoke(craftServer, new Object[0]);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(
					"Can not find MinecraftServer instance, possibily because it is not a craftbukkit implantation");
		}
	}

	public static Class<?> getNmsClass(String className) throws ClassNotFoundException {
		return getClass("net.minecraft.server", className);
	}

	public static Class<?> getCraftClass(String className) throws ClassNotFoundException {
		return getClass("org.bukkit.craftbukkit", className);
	}

	public static Class<?> getClass(String packagePrefix, String className) throws ClassNotFoundException {
		String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
		String name = packagePrefix.endsWith(".") ? packagePrefix : (packagePrefix + ".") + version + className;
		Class<?> nmsClass = Class.forName(name);
		return nmsClass;
	}
}
