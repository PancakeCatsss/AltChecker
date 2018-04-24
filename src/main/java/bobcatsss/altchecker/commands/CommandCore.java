package bobcatsss.altchecker.commands;

import bobcatsss.altchecker.Main;
import bobcatsss.altchecker.UserData;
import bobcatsss.altchecker.commands.api.CommandListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;

public class CommandCore implements CommandListener {
    private Main main;

    public CommandCore (Main main) {
        this.main = main;
    }

    public Main getMain() {
        return main;
    }

    public void getUser (String name, UserCollect collect){
        getUser(name, collect, ()->{});
    }

    /**
     * An Async method to search for a user based on their name (or UUID, if it cant find the name)
     *
     * @param name
     *             Name that is being searched for (Will also search for the UUID if need be)
     * @param collect
     *                Will run when it finds the user
     * @param onFailure
     *                  Will run when the code fails to find userdata
     */
    void getUser(String name, UserCollect collect, Runnable onFailure) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        String uuid = player.getUniqueId().toString();

        CompletableFuture.runAsync(() -> {
            for (UserData user : UserData.collectData()){
                if (user.getName().equals(name) || user.getUuid().equals(uuid)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            collect.run(user);
                        }
                    }.runTask(main);
                    return;
                }
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    onFailure.run();
                }
            }.runTask(main);
        });
    }

    interface UserCollect {
        void run (UserData user);
    }
}
