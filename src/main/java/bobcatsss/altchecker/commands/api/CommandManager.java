package bobcatsss.altchecker.commands.api;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;

import static org.bukkit.ChatColor.*;

public class CommandManager {

    private static final String
            wildcardSymbol = "*",
            numberWildcardSymbol = "**",
            eitherSymbol = "|",
            errorPrefix = String.valueOf(RED + "§lSorry!" + RESET + GRAY + ' '),
            permissionErrorDefault = errorPrefix + "You don't have permission to access this command.",
            playerErrorDefault = errorPrefix + "You must be a player to access this command.",
            argumentsErrorDefault = errorPrefix + "Invalid Arguments, Please try again!";

    public static void register(CommandListener... listeners) {

        for (CommandListener listener : listeners) {

            Class<?> _class = listener.getClass();
            Method[] classMethods = _class.getMethods();

            Map<String, Set<Method>> map = new HashMap<>();

            for (Method method : classMethods) {

                if (!method.isAnnotationPresent(Command.class)) continue;

                List<String> names = new ArrayList<>();
                if (!method.getAnnotation(Command.class).name().isEmpty()) {

                    Command command = method.getAnnotation(Command.class);

                    if (command.name().trim().contains(" ")) {

                        names.addAll(new ArrayList<>(Arrays.asList(command.name().trim().split(" "))));

                    } else {

                        names.add(command.name());

                    }

                } else if (_class.isAnnotationPresent(CommandClass.class) && !_class.getAnnotation(CommandClass.class).name().isEmpty()) {

                    CommandClass command = _class.getAnnotation(CommandClass.class);

                    if (command.name().trim().contains(" ")) {
                        names.addAll(new ArrayList<>(Arrays.asList(command.name().trim().split(" "))));
                    } else {
                        names.add(command.name());
                    }
                } else {
                    names.add(method.getName());
                }

                for (String name : names) {

                    if (!map.containsKey(name)) {

                        Set<Method> methods = new HashSet<>();
                        methods.add(method);
                        map.put(name, methods);

                    } else {

                        map.get(name).add(method);

                    }

                }

            }

            registerCommands(listener, map);

        }

    }

    private static void registerCommands(CommandListener listener, Map<String, Set<Method>> map) {
        map.forEach((name, methods) -> new CustomCommand(name) {

            @Override
            public boolean execute(CommandSender sender, String cmd, String[] args) {

                String error = "";

                for (Method method : methods) {

                    Command command = method.getAnnotation(Command.class);

                    String permission = command.permission();
                    String arguments = command.arguments();

                    String permissionError = command.permissionError();
                    String playerError = command.playerError();
                    String argumentsError = command.argumentsError();

                    if (listener.getClass().isAnnotationPresent(CommandClass.class)) {

                        CommandClass commandClass = method.getClass().getAnnotation(CommandClass.class);

                        if (permission.isEmpty())
                            permission = commandClass.permission();
                        else permission += commandClass.permission();


                        if (permissionError.equals("default"))
                            permissionError = commandClass.permissionError();

                        if (playerError.equals("default"))
                            playerError = commandClass.playerError();

                        if (argumentsError.equals("default"))
                            argumentsError = commandClass.argumentsError();

                    }

                    if (permissionError.equals("default"))
                        permissionError = permissionErrorDefault;

                    if (playerError.equals("default"))
                        playerError = playerErrorDefault;

                    if (argumentsError.equals("default"))
                        argumentsError = argumentsErrorDefault;


                    boolean player;
                    player = method.getParameterTypes().length != 0 && method.getParameterTypes()[0].equals(Player.class);

                    if (!playerCheck(player, sender)) {
                        if (!(playerError.equalsIgnoreCase("none") || playerError.isEmpty()))
                            error = playerError;

                        continue;
                    }

                    if (!permissionCheck(permission, sender)) {
                        if (!(playerError.equalsIgnoreCase("none") || permissionError.isEmpty()))
                            error = permissionError;

                        continue;
                    }

                    if (!argumentsCheck(arguments, args)) {
                        if (!(argumentsError.equalsIgnoreCase("none") || argumentsError.isEmpty()))
                            error = argumentsError;

                        continue;
                    }


                    run(method, arguments, args, listener, sender, player);
                    error = "";


                }

                if (!error.isEmpty())
                    sender.sendMessage(error);

                return true;
            }

        });
    }

    private static void run(Method method, String arguments, String[] args, CommandListener listener, CommandSender sender, boolean player) {
        try {
            int parameters = method.getParameterTypes().length;

            if (parameters == 0)
                method.invoke(listener);
            else if (parameters == 1) {
                if (method.getParameterTypes()[0].equals(Player.class) || method.getParameterTypes()[0].equals(CommandSender.class)) {
                    method.invoke(listener, sender);
                } else {
                    if (arguments.isEmpty())
                        method.invoke(listener, (Object) args);
                    else
                        method.invoke(listener, (Object) getArgs(arguments, args));
                }
            } else if (parameters == 2) {
                if (method.getParameterTypes()[0].equals(Player.class) || method.getParameterTypes()[0].equals(CommandSender.class)) {
                    if (arguments.isEmpty())
                        method.invoke(listener, sender, args);
                    else
                        method.invoke(listener, sender, getArgs(arguments, args));
                } else {
                    if (!arguments.isEmpty())
                        method.invoke(listener, getArgs(arguments, args), sender);
                    else
                        method.invoke(listener, args, sender);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean permissionCheck(String permissions, CommandSender sender) {
        if (sender.isOp() || permissions.isEmpty()) return true;

        if (permissions.trim().contains(" ")) {
            for (String permission : permissions.trim().split(" ")) {
                if (!sender.hasPermission(permission))
                    return false;
            }
        } else if (!sender.hasPermission(permissions))
            return false;

        return true;
    }

    private static boolean playerCheck(boolean player, CommandSender sender) {
        return !player || sender instanceof Player;
    }

    private static boolean argumentsCheck(String arguments, String[] args) {
        if (arguments.isEmpty()) return true;
        String[] wildcards = arguments.split(" ");
        if (wildcards.length != args.length) return false;

        for (int i = 0; i < wildcards.length; i++) {
            if (wildcards[i].equals(wildcardSymbol))
                continue;
            if (wildcards[i].contains(eitherSymbol))
                for (String o : wildcards[i].split("\\|"))
                    if (!o.equalsIgnoreCase(args[i]))
                        return false;
            if (wildcards[i].contains(numberWildcardSymbol))
                if (!isNumber(wildcards[i]))
                    return false;
            if (!wildcards[i].toLowerCase().equals(args[i].toLowerCase()))
                return false;

        }

        return true;
    }

    private static String[] getArgs(String arguments, String[] args) {
        String[] wildcards = arguments.split(" ");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < wildcards.length; i++) {
            if (wildcards[i].equals(wildcardSymbol) || wildcards[i].contains(eitherSymbol) || wildcards[i].equals(numberWildcardSymbol))
                list.add(args[i]);
        }
        String[] array = new String[list.size()];
        array = list.toArray(array);

        return array;
    }

    private static boolean isNumber(String string) {
        return string.matches("-?\\d+(\\.\\d+)?");
    }

    abstract static class CustomCommand extends BukkitCommand {
        CustomCommand(String name) {
            super(name);
            try {

                SimpleCommandMap smp = (SimpleCommandMap) Class.forName("org.bukkit.craftbukkit." +
                        Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".CraftServer")
                        .getMethod("getCommandMap").invoke(Bukkit.getServer());
                smp.register(name, this);
                register(smp);
            } catch (Exception ignored) {
            }
        }

    }

}
