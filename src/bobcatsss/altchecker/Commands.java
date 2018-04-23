package bobcatsss.altchecker;

import java.util.List;
import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public class Commands implements CommandExecutor {

	private Main plugin;

	public Commands(Main pl) {
		this.plugin = pl;
	}

	private String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
	@SuppressWarnings("deprecation")
	private String getUUID(String name) {
		if(Bukkit.getPlayerExact(name) != null) {
			return Bukkit.getPlayerExact(name).getUniqueId().toString();
		}
		if(Bukkit.getOfflinePlayer(name).hasPlayedBefore()) {
			return Bukkit.getOfflinePlayer(name).getUniqueId().toString();
		}
		return null;
	}

	private boolean isIgnored(String ip) {
		if(JoinEvent.altCache.containsKey(ip)) {
			List<String> names = JoinEvent.altCache.get(ip);
			for(String name : names) {
				if(getUUID(name) != null) {
					if(plugin.getConfig().getStringList("Ignore-UUID").contains(getUUID(name))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	
	private String colorAlts(String alt) {
		User altAccount = ess.getUser(alt);
		if (altAccount != null) {
			if (Bukkit.getServer().getBanList(Type.NAME).isBanned(alt)) {
				return color("&c" + alt);
			}
			if (Bukkit.getPlayer(altAccount.getName()) == null) {
				return color("&7" + alt);
			} else {
				return color("&a" + alt);
			}
		}
		return null;
	}

	private String formatAlts(List<String> alts) {
		StringBuilder builder = new StringBuilder();
		int count = 1;
		for (String alt : alts) {
			if (alts.size() != count) {
				if(colorAlts(alt) != null) {
				builder.append(colorAlts(alt)).append("&r").append(", ");
				} else {
					builder.append(alt).append("&r").append(", ");
				}
			} else {
				if(colorAlts(alt) != null) {
				builder.append(colorAlts(alt)).append("&r ");
				} else {
					builder.append(alt).append("&r");
				}
			}
			count++;
		}
		return builder.toString();
	}

	Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {
		if (cmd.getName().equalsIgnoreCase("alts")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("You must be a Player to use this Command");
				return true;
			}
			Player p = (Player) sender;
			if (args.length == 0) {
				if (p.hasPermission("altschecker.check")) {
					p.sendMessage(color("&8[&aAlts&8] &7Usage: /alts reload | /alts check <player>"));
					return true;
				}
				p.sendMessage(color("&cYou don't have permission to use this command"));
				return true;
			}
			if (args.length == 1) {
				if (p.hasPermission("altschecker.reload")) {
					if (args[0].equalsIgnoreCase("reload")) {
						plugin.reloadConfig();
						plugin.saveConfig();
						p.sendMessage(color("&8[&aAlts&8] &3Config has been reload."));
						return true;
					}
					p.sendMessage(color("&8[&aAlts&8] &7Usage: /alts reload | /alts check <player>"));
					return true;
				}
				p.sendMessage(color("&cYou don't have permission to use this command"));
				return true;
			}
			if (args.length >= 2) {
				if (p.hasPermission("altschecker.check")) {
					if (args[0].equalsIgnoreCase("check")) {
						User toCheck = ess.getUser(args[1]);
						if (toCheck != null) {
							String ip = toCheck.getLastLoginAddress();
							if (JoinEvent.altCache.containsKey(ip)) {
								List<String> alts = JoinEvent.altCache.get(ip);
								if (alts.size() > 1) {
									if (isIgnored(ip)) {
										if (p.hasPermission("altschecker.check.ignored")) {
											p.sendMessage(color("&8[&aAlts&8] &3Known accounts for &6"
													+ toCheck.getName() + " &3are&f: \n" + formatAlts(alts)));
											return true;
										}
										p.sendMessage(color(
												"&8[&aAlts&8] &cYou don't have permission to check this account."));
										return true;
									}
									p.sendMessage(color("&8[&aAlts&8] &3Known accounts for &6" + toCheck.getName()
											+ " &3are&f: \n" + formatAlts(alts)));
									return true;
								}
								p.sendMessage(color(
										"&8[&aAlts&8] &6" + toCheck.getName() + " &3doesn't have any alt accounts."));
								return true;
							}
							p.sendMessage(
									color("&8[&aAlts&8] &6" + toCheck.getName() + " &3doesn't have any alt accounts."));
							return true;
						}
						p.sendMessage(color("&8[&aAlts&8] &c Could not find the user &6" + args[1]));
						return true;
					}
					p.sendMessage(color("&8[&aAlts&8] &7Usage: /alts check <player>"));
					return true;
				}
				p.sendMessage(color("&cYou don't have permission to use this command."));
				return true;
			}
		}
		return false;
	}
}
