package cn.apisium.util.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;

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
