package bobcatsss.altchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public class JoinEvent implements Listener {

	private Main plugin;

	public JoinEvent(Main pl) {
		this.plugin = pl;
	}

	public static Map<String, List<String>> altCache = new HashMap<>();
	Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

	private String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	private void sendStaffMessage(String name, List<String> alts) {
		if (!Bukkit.getOnlinePlayers().isEmpty()) {
			for (Player online : Bukkit.getOnlinePlayers()) {
				if (online.hasPermission("altschecker.view")) {
					if (name != null) {
						online.sendMessage(color("&8[&aAlts&8] &6" + name + " has logged in with the same ip as&f: "
								+ formatAlts(alts)));
					}
				}
			}
		}
	}

	private boolean isIgnored(String ip) {
		List<String> toIgnore = altCache.get(ip);
		for (String s : toIgnore) {
			if (ess.getUser(s) != null) {
				User user = ess.getUser(s);
				if (plugin.getConfig().getStringList("Ignore-UUID").contains(user.getConfigUUID().toString())) {
					return true;
				}
			}
		}
		return false;
	}

	public void createCache() {
		for (UUID uuid : ess.getUserMap().getAllUniqueUsers()) {
			User toAdd = ess.getUser(uuid);
			String ip = toAdd.getLastLoginAddress();
			if (altCache.containsKey(ip)) {
				List<String> alts = altCache.get(ip);
				if (!alts.contains(toAdd.getName())) {
					alts.add(toAdd.getName());
					altCache.put(ip, alts);
				}
			} else {
				List<String> alts = new ArrayList<>();
				alts.add(toAdd.getName());
				altCache.put(toAdd.getLastLoginAddress(), alts);
			}
		}
	}

	private String colorAlts(String alt) {
		if (ess.getUser(alt) != null) {
			User altAccount = ess.getUser(alt);
			if (Bukkit.getServer().getBanList(Type.NAME).isBanned(alt)) {
				return color("&c" + alt);
			}
			if (Bukkit.getPlayer(altAccount.getName()) == null) {
				return color("&7" + alt);
			} else {
				return color("&a" + alt);
			}
		}
		return color("&a" + alt);
	}

	private String formatAlts(List<String> alts) {
		StringBuilder builder = new StringBuilder();
		int count = 1;
		for (String alt : alts) {
			if (alts.size() != count) {
				builder.append(colorAlts(alt)).append("&r").append(", ");
			} else {
				builder.append(colorAlts(alt)).append("&r ");
			}
			count++;
		}
		return builder.toString();
	}

	@EventHandler
	public void onJoin(AsyncPlayerPreLoginEvent e) {
		if (altCache.isEmpty()) {
			createCache();
		}
		String ip = e.getAddress().getHostAddress();
		if (altCache.containsKey(ip)) {
			List<String> alts = altCache.get(ip);
			if (!alts.contains(e.getName())) {
				alts.add(e.getName());
				altCache.put(ip, alts);
			}
			if (!isIgnored(ip)) {
				if (alts.size() > 1) {
					if (e.getName() != null && alts != null) {
						sendStaffMessage(e.getName(), alts);
					}
				}
				return;
			}
			return;
		}
		List<String> alts = new ArrayList<>();
		altCache.put(ip, alts);
	}
}