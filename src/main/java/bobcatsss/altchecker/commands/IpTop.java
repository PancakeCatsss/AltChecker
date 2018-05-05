package bobcatsss.altchecker.commands;

import bobcatsss.altchecker.Main;
import bobcatsss.altchecker.UserData;
import bobcatsss.altchecker.commands.api.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class IpTop extends CommandCore {
    public IpTop(Main main) {
        super(main);
    }

    @Command(name = "topips")
    public void onRun(CommandSender sender) {
        if (!sender.isOp()) return;
        Collection<UserData> users = UserData.collectData();
        CompletableFuture.runAsync(() -> {
            Map<String, Integer> tryMap = new HashMap<>();
            long start = System.currentTimeMillis();
            users.forEach(user -> {
                user.getKnownIps().forEach(ip -> tryMap.put(ip, (tryMap.getOrDefault(ip, 0) + 1)));
            });
            long endGather = (System.currentTimeMillis() - start);
            long startSort = System.currentTimeMillis();
            List<Map.Entry<String, Integer>> list = new ArrayList<>(sortByComparator(tryMap, false).entrySet());
            long endSort = (System.currentTimeMillis() - startSort);

            new BukkitRunnable() {
                @Override
                public void run() {
                    int i = 20;
                    sender.sendMessage("Collect Time (" + endGather + "ms)");
                    sender.sendMessage("Sort Time (" + endSort + "ms)");
                    sender.sendMessage("Top 20 Most common IPs:");
                    for (Map.Entry<String, Integer> entry : list) {
                        if (i < 0) break;
                        sender.sendMessage("- " + entry.getKey() + " (x" + entry.getValue() + ")");
                        i--;
                    }
                }
            }.runTask(getMain());
        });
    }

    private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, (o1, o2) -> {
            if (order) {
                return o1.getValue().compareTo(o2.getValue());
            } else {
                return o2.getValue().compareTo(o1.getValue());

            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
