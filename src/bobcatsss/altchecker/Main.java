package bobcatsss.altchecker;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	
	
	public void onEnable() {
		saveDefaultConfig();
		getCommands();
		getEvents();
	}
	
	public void onDisable() {
	}

	
	public void getCommands() {
		getCommand("alts").setExecutor(new Commands(this));
	}
	public void getEvents() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new JoinEvent(this), this);
	}

}
