package bobcatsss.altchecker;

import bobcatsss.altchecker.commands.AltCheck;
import bobcatsss.altchecker.commands.api.CommandManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import simple.brainsynder.utils.Base64Wrapper;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Main extends JavaPlugin {

    public void onEnable() {
        saveDefaultConfig();

        CommandManager.register(new AltCheck(this));

        new BukkitRunnable() {
            @Override
            public void run() {
                StorageMaker storage = getStorage();
                getStorage().getKeySet().forEach(uuid -> {
                    UserData data = UserData.getData(uuid);
                    data.fromCompound(storage.getCompoundTag(uuid));
                });
            }
        }.runTaskLater(this, 20);

        /* COLLECTS DATA FROM THE ESSENTIALS FILES */
        File folder = new File(getDataFolder().getParentFile().toString() + File.separator + "Essentials" + File.separator + "userdata");
        if (!folder.exists()) folder.mkdirs();
        File[] files = folder.listFiles();
        if (files == null) return;
        if (files.length == 0) return;
        CompletableFuture.runAsync(() -> {
            JSONArray array = new JSONArray();
            for (File file : files) {
                if (!file.getName().endsWith(".yml")) continue;
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
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
                        System.out.println(Base64Wrapper.encodeString(array.toJSONString()));
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

    public void collectAlts(ValueReturn value, List<String> targetIP) {
        Collection<UserData> users = UserData.collectData();
        CompletableFuture.runAsync(() -> {
            List<UserData> match = new ArrayList<>();
            users.forEach(user -> {
                if (targetIP.stream().anyMatch(ip -> ip.equals(user.getLastKnownIP()))) {
                    match.add(user);
                } else {
                    if (!Collections.disjoint(user.getKnownIps(), targetIP)) {
                        match.add(user);
                    }else{
                        for (String target : targetIP) {
                            user.getKnownIps().forEach(ip -> {
                                if (ip.equals(target)) {
                                    match.add(user);
                                }
                            });
                        }
                    }
                }
            });
            new BukkitRunnable() {
                @Override
                public void run() {
                    value.run(match);
                }
            }.runTask(this);
        });
    }

    public interface ValueReturn {
        void run(List<UserData> users);
    }

    public StorageMaker getStorage() {
        return new StorageMaker(new File(getDataFolder(), "Storage.cache"));
    }
}
