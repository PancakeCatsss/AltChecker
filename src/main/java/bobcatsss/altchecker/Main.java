package bobcatsss.altchecker;

import bobcatsss.altchecker.commands.AltCheck;
import bobcatsss.altchecker.commands.AltReload;
import bobcatsss.altchecker.commands.IpTop;
import bobcatsss.altchecker.commands.api.CommandManager;
import bobcatsss.altchecker.listener.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Main extends JavaPlugin {
    private List<String> skip = Arrays.asList(
            "158.69.97.252",
            "158.85.69.138",
            "127.0.0.1"
    );

    /*
    ,
            "75.97.66.46",
            "5.254.97.107",
            "209.180.130.23",
            "74.115.3.65",
            "96.243.231.12",
            "73.206.5.169",
            "216.172.142.252",
            "109.135.14.230",
            "73.163.74.151",
            "71.100.136.218",
            "70.15.65.151",
            "178.117.65.82",
            "199.255.211.40"
     */

    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        CommandManager.register(new AltReload(this));
        CommandManager.register(new AltCheck(this));
        CommandManager.register(new IpTop(this));

        new BukkitRunnable() {
            @Override
            public void run() {
                StorageMaker storage = getStorage();
                for (String rawUUID : storage.getKeySet()) {
                    UUID uuid = UUID.fromString(rawUUID);
                    if (uuid.version() < 4) continue;

                    UserData data = UserData.getData(rawUUID);
                    data.fromCompound(storage.getCompoundTag(rawUUID));
                }
            }
        }.runTaskLater(this, 20);

        /* COLLECTS DATA FROM THE ESSENTIALS FILES */
        File folder = new File(getDataFolder().getParentFile().toString() + File.separator + "Essentials" + File.separator + "userdata");
        if (!folder.exists()) folder.mkdirs();
        File[] files = folder.listFiles();
        if (files == null) return;
        if (files.length == 0) return;
        CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            JSONArray array = new JSONArray();
            System.out.println("[AltChecker] Loading UserData from Essentials Files...");
            for (File file : files) {
                if (!file.getName().endsWith(".yml")) continue;
                try {
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                    if (uuid.version() < 4) continue;

                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String name = "Steve", lastIP = "127.0.0.1";

                    JSONObject json = new JSONObject();
                    if (config.contains("lastAccountName")) name = config.getString("lastAccountName");
                    if (config.contains("ipAddress")) lastIP = config.getString("ipAddress");
                    json.put("uuid", uuid.toString());
                    json.put("name", name);
                    json.put("lastIP", lastIP);
                    array.add(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    long end = (System.currentTimeMillis() - start);
                    System.out.println("[AltChecker] Data collection finished ("+array.size()+" values), took "+end+"ms to collect.");
                    if (!array.isEmpty()) {
                        array.forEach(o -> {
                            JSONObject json = (JSONObject) o;
                            String name = String.valueOf(json.getOrDefault("name", "Steve")),
                                    lastIP = String.valueOf(json.getOrDefault("lastIP", "127.0.0.1")),
                                    uuid = String.valueOf(json.getOrDefault("uuid", UUID.randomUUID().toString()));
                            UserData data = UserData.getData(uuid);
                            data.setLastKnownIP(lastIP);
                            data.setName(name);
                            data.setUuid(uuid);
                        });
                    }
                }
            }.runTask(this);
        });
    }

    public void onDisable() {
        StorageMaker compound = getStorage();
        UserData.collectData().forEach(user -> {
            compound.setTag(user.getUuid(), user.toCompound());
        });
        compound.save();
    }

    /**
     * An Async method to search for users that share common IP addresses
     *
     * @param value
     *              An Interface that will run when it has finished searching
     * @param targetIP
     *              A list of IP Addresses to compare with all other players
     */
    public void collectAlts(ValueReturn<List<UserData>> value, List<String> targetIP) {
        Collection<UserData> users = UserData.collectData();
        CompletableFuture.runAsync(() -> {
            List<UserData> match = new ArrayList<>();
            users.forEach(user -> {
                if (targetIP.stream().filter(ip -> !skip.contains(ip)).anyMatch(ip -> ip.equals(user.getLastKnownIP()))) {
                    match.add(user); // Checks if ANY of the "targetIPs" are the same as the "LastKnownIP"
                } else {
                    List<String> known = user.getKnownIps();
                    known.removeAll(skip);
                    if (!Collections.disjoint(known, targetIP)) { // Checks if "KnownIPs" contains ANY of the "targetIPs"
                        match.add(user);
                    }else{ // If that fails... loop though all...
                        for (String target : targetIP) {
                            known.forEach(ip -> {
                                if (ip.equals(target)) {
                                    match.add(user);
                                }
                            });
                        }
                    }
                }
            });
            new BukkitRunnable() { // Will be run when the task is completed, this is to sync the data to the server
                @Override
                public void run() {
                    value.run(match);
                }
            }.runTask(this);
        });
    }


    /**
     * An Async method to search for a user based on their name (or UUID, if it cant find the name)
     *
     * @param name
     *             Name that is being searched for (Will also search for the UUID if need be)
     * @param collect
     *                Will run when it finds the user
     */
    public void getUser (String name, ValueReturn<List<UserData>> collect){
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
    public void getUser(String name, ValueReturn<List<UserData>> collect, Runnable onFailure) {
        Collection<UserData> datas = UserData.collectData();
        CompletableFuture.runAsync(() -> {
            List<UserData> users = new ArrayList<>();
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            String uuid = player.getUniqueId().toString();
            for (UserData user : datas){
                if (user.getName().equals(name) || user.getUuid().equals(uuid)) {
                    users.add(user);
                }
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!users.isEmpty()) {
                        collect.run(users);
                    }else{
                        onFailure.run();
                    }
                }
            }.runTask(this);
        });
    }

    public interface UserCollect {
        void run (UserData user);
    }

    public interface ValueReturn<T> {
        void run(T users);
    }

    public StorageMaker getStorage() {
        return new StorageMaker(new File(getDataFolder(), "Storage.cache"));
    }
}
