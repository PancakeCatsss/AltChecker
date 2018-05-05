package bobcatsss.altchecker.commands;

import bobcatsss.altchecker.Main;
import bobcatsss.altchecker.commands.api.CommandListener;
import org.bukkit.ChatColor;

public class CommandCore implements CommandListener {
    private Main main;
    String prefix = ChatColor.translateAlternateColorCodes('&', "&6[&eAltChecker&6] &7");

    public CommandCore (Main main) {
        this.main = main;
    }

    public Main getMain() {
        return main;
    }
}
