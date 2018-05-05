package bobcatsss.altchecker.commands;

import bobcatsss.altchecker.Main;
import bobcatsss.altchecker.UserData;
import bobcatsss.altchecker.commands.api.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simple.brainsynder.nms.ITellraw;
import simple.brainsynder.utils.Reflection;

public class AltCheck extends CommandCore {

    public AltCheck(Main main) {
        super(main);
    }

    @Command(name = "altcheck")
    public void run(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            if (!sender.hasPermission("altschecker.check")) return;
        }

        if (args.length == 0) {
            sender.sendMessage(prefix + "Usage: /altcheck <name|uuid>");
            return;
        }

        getMain().getUser(args[0], userList -> {
            sender.sendMessage(prefix+"§7Found §e"+userList.size()+" §7result(s) for §e"+args[0]);

            userList.forEach(user -> {
                String data = user.getName()+" | "+user.getUuid();
                getMain().collectAlts(alts -> {
                    if (alts.isEmpty()) {
                        if (sender instanceof Player) {
                            Reflection.getTellraw("Could not find any alt accounts for ")
                                    .color(ChatColor.RED).then(user.getName()).color(ChatColor.GRAY).tooltip(user.getUuid())
                                    .send((Player) sender);
                        }else{
                            sender.sendMessage(prefix + "§cCould not find any alt accounts for §7" + data);
                        }
                        return;
                    }
                    ITellraw raw = Reflection.getTellraw(prefix + ChatColor.YELLOW + args[0]+ " §7has been linked to: ");
                    StringBuilder names = new StringBuilder();
                    int i = 0;
                    for (UserData userData  : alts)  {
                        raw.then(userData.getName()).color(ChatColor.YELLOW).tooltip(userData.getUuid());
                        names.append(userData.getName());
                        if (i != (alts.size()-1)) {
                            names.append(", ");
                            raw.then(", ").color(ChatColor.GOLD);
                        }
                        i++;
                    }

                    if (sender instanceof Player) {
                        raw.send((Player) sender);
                    }else{
                        String s = names.toString();
                        if (s.isEmpty()) {
                            sender.sendMessage(prefix + ChatColor.YELLOW + "[" + data+ "] §7does not have an IP similar to any other players.");
                            return;
                        }
                        sender.sendMessage(prefix + ChatColor.YELLOW + "[" + data+ "] §7has been linked to: " + s.substring(0, s.length() - 2));

                    }
                }, user.getKnownIps());
            });
        }, () -> sender.sendMessage(prefix + "§cCould not find a user with §7" + args[0] + " §cas a name."));
    }
}
