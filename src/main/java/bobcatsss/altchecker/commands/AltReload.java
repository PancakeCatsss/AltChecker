package bobcatsss.altchecker.commands;

import bobcatsss.altchecker.Main;
import bobcatsss.altchecker.commands.api.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AltReload extends CommandCore {

    public AltReload(Main main) {
        super(main);
    }

    @Command(name = "altreload")
    public void run(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            if (!sender.hasPermission("altschecker.reload")) return;
        }

        getMain().reloadConfig();
        sender.sendMessage(prefix+"AltChecker Config.yml has been reloaded");
    }
}
