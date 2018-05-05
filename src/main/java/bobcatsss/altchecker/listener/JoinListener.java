package bobcatsss.altchecker.listener;

import bobcatsss.altchecker.Main;
import bobcatsss.altchecker.UserData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JoinListener implements Listener {
    private Main main;
    private String prefix = ChatColor.translateAlternateColorCodes('&', "&6[&eAltChecker&6] &e");

    public JoinListener (Main main) {
        this.main = main;
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onJoin (PlayerLoginEvent event) {
        if (event.getAddress() == null) return;

        Player p = event.getPlayer();
        if (main.getConfig().getStringList("Ignore-UUID").contains(p.getUniqueId().toString())) return;

        UserData user = UserData.getData(p.getUniqueId().toString());
        user.setName(p.getName());
        user.setUuid(p.getUniqueId().toString());
        user.setLastKnownIP(event.getAddress().getHostAddress());

        main.collectAlts(users -> {
            if (users.isEmpty()) return;
            CompletableFuture.runAsync(() -> {
                StringBuilder names = new StringBuilder();
                users.forEach(userData -> {
                    if (!userData.getUuid().equals(user.getUuid())) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(userData.getUuid()));
                        ChatColor color = ChatColor.YELLOW;
                        if (player.isOnline()) color = ChatColor.GREEN;
                        if (player.isBanned()) color = ChatColor.RED;

                        names.append(color).append(userData.getName()).append(ChatColor.GOLD).append(", ");
                    }
                });
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String s = names.toString();
                        if (s.isEmpty()) return;
                        String list = s.substring(0, s.length()-2); // removes the last 2 characters ", "
                        if (Bukkit.getOnlinePlayers().isEmpty()) return;
                        Bukkit.getOnlinePlayers().stream().filter(sender -> sender.hasPermission("altschecker.view")).forEach(sender -> {
                            sender.sendMessage(prefix+p.getName()+" §7has shared an IP with: " + list);
                        });
                    }
                }.runTask(main);
            });
        }, user.getKnownIps());
    }
}
