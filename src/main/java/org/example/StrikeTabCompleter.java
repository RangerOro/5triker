package org.example;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StrikeTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("punish");
            completions.add("remove");
            completions.add("edit");
            completions.add("inspect");
            completions.add("info");
            completions.add("rules");
            completions.add("log");
            completions.add("list");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "punish":
                case "remove":
                case "inspect":
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(player -> player.getName())
                            .collect(Collectors.toList()));
                    break;
                case "edit":
                    completions.add("add");
                    completions.add("delete");
                    completions.add("change");
                    break;
                case "log":
                case "list":
                    completions.add("1");
                    completions.add("2");
                    completions.add("3");
                    break;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("punish")) {
                completions.addAll(TrikerPlugin.rules.keySet());
            } else if (args[0].equalsIgnoreCase("edit")) {
                if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("change")) {
                    completions.addAll(TrikerPlugin.rules.keySet());
                } else if (args[1].equalsIgnoreCase("add")) {
                    completions.add("1A");
                    completions.add("1B");
                    completions.add("2A");
                    completions.add("3A");
                    completions.add("CUSTOM1");
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(player -> player.getName())
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("edit")) {
                if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("change")) {
                    completions.add("0.25");
                    completions.add("0.5");
                    completions.add("1.0");
                    completions.add("2.0");
                    completions.add("3.0");
                    completions.add("5.0");
                }
            } else if (args[0].equalsIgnoreCase("punish")) {
                completions.add("Spamming");
                completions.add("Hacking");
                completions.add("Homophobia");
                completions.add("Lag Machine");
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
