package cn.apisium.beelogin;

import java.lang.reflect.Field;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.plugin.java.JavaPlugin;

import cn.apisium.authlib.GameProfile;
import cn.apisium.beelogin.api.BeeLoginApi;
import cn.apisium.beelogin.variable.NonConfig;
import cn.apisium.beelogin.variable.Variables;
import cn.apisium.util.bukkit.NmsHelper;

public class Main extends JavaPlugin {
	public static final String tokenPerfix = "$A(";
	public static final String kickedName = "$K(kicked";

	@Override
	public void onEnable() {
		getLogger().info("BeeLogin Reloaded has loaded");
	}

	public Main loadConfig() {// dirty reflection stuffs to load config
		Class<?> variables = Variables.class;
		for (Field variable : variables.getDeclaredFields()) {
			if (variable.isAnnotationPresent(NonConfig.class)) {
				continue;
			}
			String path = capitalFirst(variable.getName());
			try {
				Object defaultValue = variable.get(null);
				Object config = this.getConfig().get(path, null);
				if (config != null)
					variable.set(null, config);
				else if (defaultValue != null)
					this.getConfig().set(path, defaultValue);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		this.saveConfig();
		return this;
	}

	public String capitalFirst(String string) {
		char[] cs = string.toCharArray();
		cs[0] = Character.toUpperCase(cs[0]);
		return String.valueOf(cs);
	}

	public void changeDetails(AsyncPlayerPreLoginEvent event) {
		RuntimeException exception = null;
		try {
			Class<?> loginListener = NmsHelper.getNmsClass("LoginListener");
			List<Object> listeners = NmsHelper.getPossibleLoginListeners(event.getAddress(), event.getUniqueId());
			boolean processed = false;
			for (Object login : listeners) {
				if (processed) {
					loginListener.getMethod("disconnect", new Class[] { String.class }).invoke(login,
							Variables.unauthorizedMessage);
				}
				Field f = NmsHelper.findFirstFieldByType(login.getClass(), loginListener);
				f.setAccessible(true);
				try {
					f.set(login,
							new com.mojang.authlib.GameProfile(GameProfile.getID(event.getUniqueId(), event.getName()),
									GameProfile.getName(event.getUniqueId(), event.getName())));
					processed = true;
				} catch (IllegalArgumentException | IllegalAccessException e) {
					loginListener.getMethod("disconnect", new Class[] { String.class }).invoke(login, "Internal error");
					exception = new RuntimeException(
							"Can not change GameProfile instance,  possibily because it is not a craftbukkit implantation");
					processed = true;
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException("Unknown exception", e);
		}
		if (exception != null) {
			throw exception;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onAuth(AsyncPlayerPreLoginEvent event) {
		try {
			changeDetails(event);
		} catch (Throwable e) {
			event.setLoginResult(Result.KICK_OTHER);
			event.setKickMessage(Variables.unauthorizedMessage);
		}
		if (event.getName().equalsIgnoreCase(kickedName)) {// well, at this stage it should not happened actually, just
															// for making sure...
			event.setLoginResult(Result.KICK_OTHER);
			event.setKickMessage(Variables.unauthorizedMessage);
		} else if (event.getName().startsWith(tokenPerfix)) {// kicked as what the name said
			event.setLoginResult(Result.KICK_OTHER);
			event.setKickMessage(Variables.tokenKickMessage);
		} else if (!BeeLoginApi.authed(event.getUniqueId(), event.getName())) {// kicked if unauthorized
			event.setLoginResult(Result.KICK_OTHER);
			event.setKickMessage(Variables.unauthorizedMessage);
		}
	}
}
