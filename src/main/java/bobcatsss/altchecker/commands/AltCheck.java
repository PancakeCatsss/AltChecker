package bobcatsss.altchecker.commands;

import bobcatsss.altchecker.Main;
import bobcatsss.altchecker.commands.api.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AltCheck extends CommandCore {

    public AltCheck(Main main) {
        super(main);
    }

    @Command(name = "altcheck")
    public void run(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            if (!((Player) sender).hasPermission("altschecker.check")) return;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /altcheck <name|uuid>");
            return;
        }

        getUser(args[0], user -> getMain().collectAlts(users -> {
                    if (users.isEmpty()) {
                        sender.sendMessage("Could not find any alt accounts for " + args[0]);
                        return;
                    }

                    StringBuilder names = new StringBuilder();
                    users.forEach(userData -> names.append(userData.getName()).append(", "));
                    String s = names.toString();
                    sender.sendMessage(args[0]+" has also been linked with: " + s.substring(0, s.length()-2));
                }, user.getKnownIps()),
                () -> sender.sendMessage("Could not find a user with '" + args[0] + "' as a name.")
        );
    }
}
