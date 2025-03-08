package org.example;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class TrikerPlugin extends JavaPlugin {
    public static final Map<String, Double> rules = new HashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final Map<UUID, Double> playerStrikes = new HashMap<>();

    private double maxStrikes;
    private long banDuration;
    private boolean autoUnban;
    private String msgInsufficientPermission, msgPlayerNotFound, msgPunishSuccess, msgRemoveSuccess, msgBan, msgUnban, infoHeader, infoAuthor, infoDesigner, infoDescription;
    private File dataFile;

    private double rule1A = 0.25;
    private double rule1B = 1.0;
    private double rule1C = 5.0;
    private double rule1D = 1.0;
    private double rule2A = 0.25;
    private double rule2B = 1.0;
    private double rule2C = 3.0;
    private double rule3A = 5.0;
    private double rule3B = 0.25;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        this.getCommand("s").setExecutor(new StrikeCommandExecutor());
        this.getCommand("s").setTabCompleter(new StrikeTabCompleter());
        dataFile = new File(getDataFolder(), "data.json");
        loadDataFromFile();
        getLogger().info("5Triker plugin enabled!");
    }

    @Override
    public void onDisable() {
        saveDataToFile();
        saveConfig();
        getLogger().info("5Triker plugin disabled.");
    }

    private void loadConfigValues() {
        maxStrikes = getConfig().getDouble("max-strikes", 5.0);
        banDuration = getConfig().getLong("ban-duration", 31536000000L);
        autoUnban = getConfig().getBoolean("auto-unban", true);

        Map<?, ?> cfgRules = getConfig().getConfigurationSection("rules").getValues(false);
        rules.clear();
        for (Map.Entry<?, ?> entry : cfgRules.entrySet()) {
            String key = entry.getKey().toString().toUpperCase();
            double value = Double.parseDouble(entry.getValue().toString());
            rules.put(key, value);
            updateRuleVariable(key, value);
        }

        msgInsufficientPermission = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.insufficient-permission", "&cNo permission."));
        msgPlayerNotFound = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.player-not-found", "&cPlayer %player% not found."));
        msgPunishSuccess = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.punish-success", "&aAdded %strikes% strikes to %player%. New total: %total%."));
        msgRemoveSuccess = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.remove-success", "&aRemoved %strikes% strikes from %player%."));
        msgBan = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.ban-message", "&cYou have been banned for exceeding %max% strikes."));
        msgUnban = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.unban-message", "&a%player% has been unbanned because their strikes are now below %max%."));
        infoHeader = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.header", "&e5Triker Plugin v%version%"));
        infoAuthor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.author", "&aDeveloped by Ranger_Oro"));
        infoDesigner = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.designer", "&aDesigned by Aemadeous"));
        infoDescription = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.description", "&aTracks and manages strikes for rule enforcement."));
    }

    private void saveDataToFile() {
        JSONObject jsonData = new JSONObject();
        JSONArray playersArray = new JSONArray();
        playerStrikes.forEach((uuid, strikes) -> {
            JSONObject playerObj = new JSONObject();
            playerObj.put("uuid", uuid.toString());
            playerObj.put("strikes", strikes);
            playersArray.add(playerObj);
        });
        jsonData.put("players", playersArray);

        JSONArray logsArray = new JSONArray();
        logs.forEach(logsArray::add);
        jsonData.put("logs", logsArray);

        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            try (FileWriter writer = new FileWriter(dataFile)) {
                writer.write(jsonData.toJSONString());
            }
            getLogger().info("Data saved successfully.");
        } catch (IOException e) {
            getLogger().severe("Failed to save data: " + e.getMessage());
        }
    }

    private void loadDataFromFile() {
        if (!dataFile.exists()) {
            getLogger().info("Data file not found. Starting fresh.");
            return;
        }
        try (FileReader reader = new FileReader(dataFile)) {
            JSONObject jsonData = (JSONObject) new JSONParser().parse(reader);
            JSONArray playersArray = (JSONArray) jsonData.get("players");
            if (playersArray != null) {
                for (Object obj : playersArray) {
                    JSONObject playerObj = (JSONObject) obj;
                    UUID uuid = UUID.fromString((String) playerObj.get("uuid"));
                    double strikes = ((Number) playerObj.get("strikes")).doubleValue();
                    playerStrikes.put(uuid, strikes);
                }
            }
            JSONArray logsArray = (JSONArray) jsonData.get("logs");
            if (logsArray != null) {
                for (Object log : logsArray) {
                    logs.add((String) log);
                }
            }
            getLogger().info("Data loaded successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to load data: " + e.getMessage());
        }
    }

    private void updateRuleVariable(String rule, Double value) {
        if (value == null) {
            switch (rule) {
                case "1A": rule1A = 0.25; break;
                case "1B": rule1B = 1.0; break;
                case "1C": rule1C = 5.0; break;
                case "1D": rule1D = 1.0; break;
                case "2A": rule2A = 0.25; break;
                case "2B": rule2B = 1.0; break;
                case "2C": rule2C = 3.0; break;
                case "3A": rule3A = 5.0; break;
                case "3B": rule3B = 0.25; break;
            }
        } else {
            switch (rule) {
                case "1A": rule1A = value; break;
                case "1B": rule1B = value; break;
                case "1C": rule1C = value; break;
                case "1D": rule1D = value; break;
                case "2A": rule2A = value; break;
                case "2B": rule2B = value; break;
                case "2C": rule2C = value; break;
                case "3A": rule3A = value; break;
                case "3B": rule3B = value; break;
                default: break;
            }
        }
    }

    private class StrikeCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                displayHelp(sender);
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "punish":
                    handlePunishCommand(sender, args);
                    break;
                case "remove":
                    handleRemoveCommand(sender, args);
                    break;
                case "edit":
                    handleEditCommand(sender, args);
                    break;
                case "inspect":
                    handleInspectCommand(sender, args);
                    break;
                case "info":
                    displayInfo(sender);
                    break;
                case "rules":
                    handleRulesCommand(sender);
                    break;
                case "log":
                    handleLogCommand(sender, args);
                    break;
                case "list":
                    handleListCommand(sender, args);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /s for help.");
                    break;
            }
            return true;
        }

        private void displayHelp(CommandSender sender) {
            sender.sendMessage(ChatColor.YELLOW + "5Triker Commands:");
            sender.sendMessage(ChatColor.GREEN + "/s punish <player> <rule> [clarification] - Add strikes to a player.");
            sender.sendMessage(ChatColor.GREEN + "/s remove <strikes> <player> - Remove strikes from a player.");
            sender.sendMessage(ChatColor.GREEN + "/s edit <add/delete/change> <rule> [amount] - Manage rules.");
            sender.sendMessage(ChatColor.GREEN + "/s inspect <player> - View a player's strike count and punishment logs.");
            sender.sendMessage(ChatColor.GREEN + "/s info - View plugin information.");
            sender.sendMessage(ChatColor.GREEN + "/s rules - View all rules and their strike values.");
            sender.sendMessage(ChatColor.GREEN + "/s log [page] - View recent moderator actions.");
            sender.sendMessage(ChatColor.GREEN + "/s list [page] - View players with strikes.");
        }

        private void handlePunishCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("fivetriker.punish")) {
                sender.sendMessage(ChatColor.RED + msgInsufficientPermission);
                return;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /s punish <player> <rule> [clarification]");
                return;
            }
            String playerName = args[1];
            String rule = args[2].toUpperCase();
            if (!rules.containsKey(rule)) {
                sender.sendMessage(ChatColor.RED + "Invalid rule. Use /s rules for valid rules.");
                return;
            }
            try {
                UUID playerUUID = fetchPlayerUUID(playerName);
                if (playerUUID == null) {
                    sender.sendMessage(ChatColor.RED + msgPlayerNotFound.replace("%player%", playerName));
                    return;
                }
                double strikesToAdd = rules.get(rule);
                double newStrikes = playerStrikes.getOrDefault(playerUUID, 0.0) + strikesToAdd;
                playerStrikes.put(playerUUID, newStrikes);
                String clarification = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No clarification provided.";
                logs.add(sender.getName() + " punished " + playerName + " with " + strikesToAdd + " strikes for rule " + rule + ". Clarification: " + clarification);
                sender.sendMessage(ChatColor.GREEN + msgPunishSuccess.replace("%strikes%", String.valueOf(strikesToAdd)).replace("%player%", playerName).replace("%total%", String.valueOf(newStrikes)));
                sender.sendMessage(ChatColor.GRAY + "Clarification: " + clarification);
                Player punishedPlayer = Bukkit.getOfflinePlayer(playerUUID).getPlayer();
                if (punishedPlayer != null && punishedPlayer.isOnline()) {
                    punishedPlayer.sendMessage(ChatColor.RED + "You have been punished for violating rule: " + rule + ". You now have " + newStrikes + " strikes.");
                    punishedPlayer.sendMessage(ChatColor.GRAY + "Clarification: " + clarification);
                }
                if (newStrikes >= maxStrikes) {
                    Date expirationDate = banDuration == 0 ? null : new Date(System.currentTimeMillis() + banDuration);
                    Bukkit.getBanList(BanList.Type.NAME).addBan(playerName, msgBan.replace("%max%", String.valueOf(maxStrikes)), expirationDate, null);
                    if (punishedPlayer != null && punishedPlayer.isOnline()) {
                        punishedPlayer.kickPlayer(ChatColor.RED + "You have been banned for exceeding " + maxStrikes + " strikes.");
                    }
                    sender.sendMessage(ChatColor.RED + playerName + " has been banned" + (expirationDate == null ? " permanently." : " until " + expirationDate.toString() + "."));
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "An error occurred while fetching player data.");
                e.printStackTrace();
            }
        }

        private void handleRemoveCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("fivetriker.remove")) {
                sender.sendMessage(ChatColor.RED + msgInsufficientPermission);
                return;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /s remove <strikes> <player>");
                return;
            }
            try {
                double strikesToRemove = Double.parseDouble(args[1]);
                String playerName = args[2];
                UUID playerUUID = fetchPlayerUUID(playerName);
                if (playerUUID == null) {
                    sender.sendMessage(ChatColor.RED + msgPlayerNotFound.replace("%player%", playerName));
                    return;
                }
                double currentStrikes = playerStrikes.getOrDefault(playerUUID, 0.0);
                double newStrikes = Math.max(0, currentStrikes - strikesToRemove);
                playerStrikes.put(playerUUID, newStrikes);
                logs.add(sender.getName() + " removed " + strikesToRemove + " strikes from " + playerName + ".");
                sender.sendMessage(ChatColor.GREEN + msgRemoveSuccess.replace("%strikes%", String.valueOf(strikesToRemove)).replace("%player%", playerName));
                Player targetPlayer = Bukkit.getOfflinePlayer(playerUUID).getPlayer();
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(ChatColor.YELLOW + "Your strikes have been reduced by " + strikesToRemove + ". You now have " + newStrikes + " strikes.");
                }
                if (autoUnban && newStrikes < maxStrikes) {
                    BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                    if (banList.isBanned(playerName)) {
                        banList.pardon(playerName);
                        sender.sendMessage(ChatColor.GREEN + msgUnban.replace("%player%", playerName).replace("%max%", String.valueOf(maxStrikes)));
                        logs.add(playerName + " was unbanned after having their strikes reduced.");
                    }
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number of strikes. Please enter a valid number.");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "An error occurred.");
                e.printStackTrace();
            }
        }

        private void handleEditCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("fivetriker.edit")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to edit rules.");
                return;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /s edit <add/delete/change> <rule> [amount]");
                return;
            }

            String action = args[1].toLowerCase();
            String rule = args[2].toUpperCase();

            switch (action) {
                case "add":
                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.RED + "Usage: /s edit add <rule> <amount>");
                        return;
                    }

                    double amountToAdd;
                    try {
                        amountToAdd = Double.parseDouble(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid value. Please enter a valid number.");
                        return;
                    }

                    if (rules.containsKey(rule)) {
                        sender.sendMessage(ChatColor.RED + "Rule already exists. Use /s edit change to modify it.");
                        return;
                    }

                    rules.put(rule, amountToAdd);
                    getConfig().set("rules." + rule, amountToAdd);
                    saveConfig();

                    logs.add(sender.getName() + " added rule " + rule + " with value " + amountToAdd + ".");
                    sender.sendMessage(ChatColor.GREEN + "Added rule " + rule + " with " + amountToAdd + " strikes.");
                    break;

                case "delete":
                    if (!rules.containsKey(rule)) {
                        sender.sendMessage(ChatColor.RED + "Rule not found.");
                        return;
                    }

                    rules.remove(rule);
                    getConfig().set("rules." + rule, null);
                    saveConfig();

                    logs.add(sender.getName() + " deleted rule " + rule + ".");
                    sender.sendMessage(ChatColor.GREEN + "Deleted rule " + rule + ".");
                    break;

                case "change":
                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.RED + "Usage: /s edit change <rule> <amount>");
                        return;
                    }

                    double newValue;
                    try {
                        newValue = Double.parseDouble(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid value. Please enter a valid number.");
                        return;
                    }

                    if (!rules.containsKey(rule)) {
                        sender.sendMessage(ChatColor.RED + "Rule not found. Use /s edit add to create a new rule.");
                        return;
                    }

                    rules.put(rule, newValue);
                    getConfig().set("rules." + rule, newValue);
                    saveConfig();

                    logs.add(sender.getName() + " changed rule " + rule + " to value " + newValue + ".");
                    sender.sendMessage(ChatColor.GREEN + "Updated rule " + rule + " to " + newValue + " strikes.");
                    break;

                default:
                    sender.sendMessage(ChatColor.RED + "Invalid action. Use add, delete, or change.");
                    break;
            }
        }

        private void handleInspectCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("fivetriker.inspect")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to inspect players.");
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /s inspect <player>");
                return;
            }
            String playerName = args[1];
            try {
                UUID playerUUID = fetchPlayerUUID(playerName);
                if (playerUUID == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return;
                }
                double strikes = playerStrikes.getOrDefault(playerUUID, 0.0);
                sender.sendMessage(ChatColor.GREEN + playerName + " has " + strikes + " strikes.");
                sender.sendMessage(ChatColor.YELLOW + "Recent punishments for " + playerName + ":");
                logs.stream()
                        .filter(log -> log.matches(".*punished " + playerName + " .*"))
                        .limit(10)
                        .forEach(log -> sender.sendMessage(ChatColor.RED + log));
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "An error occurred.");
                e.printStackTrace();
            }
        }

        private void displayInfo(CommandSender sender) {
            sender.sendMessage(infoHeader.replace("%version%", getDescription().getVersion())); // Ensure only one header line
            sender.sendMessage(infoAuthor);
            sender.sendMessage(infoDesigner);
            sender.sendMessage(infoDescription);
        }

        private void handleRulesCommand(CommandSender sender) {
            sender.sendMessage(ChatColor.YELLOW + "Rules and their strike values:");

            if (rules.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No rules have been set.");
                return;
            }

            for (Map.Entry<String, Double> entry : rules.entrySet()) {
                sender.sendMessage(ChatColor.GREEN + entry.getKey() + " ---> " + entry.getValue() + " Strikes");
            }
        }

        private void handleLogCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("fivetriker.log")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to view logs.");
                return;
            }
            sender.sendMessage(ChatColor.YELLOW + "Recent Moderation Actions:");
            logs.stream().limit(10).forEach(log -> sender.sendMessage(ChatColor.GREEN + log));
        }

        private void handleListCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("fivetriker.list")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to see striked players.");
                return;
            }
            sender.sendMessage(ChatColor.YELLOW + "Players with strikes:");
            playerStrikes.forEach((uuid, strikes) -> {
                if (strikes > 0) {
                    String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                    sender.sendMessage(ChatColor.GREEN + playerName + ": " + strikes + " strikes");
                }
            });
        }

        private UUID fetchPlayerUUID(String playerName) throws Exception {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JSONObject response = (JSONObject) new JSONParser().parse(reader);
                reader.close();
                String uuidStr = (String) response.get("id");
                return UUID.fromString(uuidStr.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            }
            // Fallback for Bedrock players
            return Bukkit.getOfflinePlayer(playerName).getUniqueId();
        }
    }
}