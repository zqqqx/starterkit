package dev.peterkanonenrohr.starterKit;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.event.Listener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class StarterKit extends JavaPlugin implements Listener, TabExecutor {

    private ItemStack[] kitItems;
    private Set<UUID> playersReceivedKit = new HashSet<>();
    private File playersFile;
    private Gson gson;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        gson = new Gson();

        playersFile = new File(getDataFolder(), "players.json");
        if (!playersFile.exists()) {
            try {
                getDataFolder().mkdirs();
                playersFile.createNewFile();
                Files.write(playersFile.toPath(), "[]".getBytes());
            } catch (IOException e) {
                getLogger().severe("Konnte players.json nicht erstellen!");
                e.printStackTrace();
            }
        }
        loadPlayersReceivedKit();

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        this.getCommand("starterkit").setExecutor(this);
        this.getCommand("starterkit").setTabCompleter(this);

        loadKitFromConfig();
    }

    @Override
    public void onDisable() {
        savePlayersReceivedKit();
    }

    private void loadPlayersReceivedKit() {
        try {
            String json = Files.readString(playersFile.toPath());
            List<String> list = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
            if (list != null) {
                for (String uuidStr : list) {
                    playersReceivedKit.add(UUID.fromString(uuidStr));
                }
            }
        } catch (IOException e) {
            getLogger().severe("Fehler beim Laden der Spieler-JSON");
            e.printStackTrace();
        }
    }

    private void savePlayersReceivedKit() {
        try {
            List<String> list = playersReceivedKit.stream().map(UUID::toString).collect(Collectors.toList());
            String json = gson.toJson(list);
            Files.write(playersFile.toPath(), json.getBytes());
        } catch (IOException e) {
            getLogger().severe("Fehler beim Speichern der Spieler-JSON");
            e.printStackTrace();
        }
    }

    public boolean hasReceivedKit(UUID uuid) {
        return playersReceivedKit.contains(uuid);
    }

    public void markAsReceived(UUID uuid) {
        playersReceivedKit.add(uuid);
        savePlayersReceivedKit();
    }

    public ItemStack[] getKitItems() {
        return kitItems;
    }

    private void loadKitFromConfig() {
        FileConfiguration config = getConfig();
        List<ItemStack> items = (List<ItemStack>) config.getList("kitItems");
        if (items != null) {
            kitItems = items.toArray(new ItemStack[0]);
            getLogger().info("Starterkit geladen, " + kitItems.length + " Items.");
        } else {
            kitItems = new ItemStack[0];
            getLogger().info("Kein Starterkit gesetzt.");
        }
    }

    private void saveKitToConfig(ItemStack[] items) {
        List<ItemStack> list = Arrays.asList(items);
        getConfig().set("kitItems", list);
        saveConfig();
        kitItems = items;
    }

    public void giveKit(Player player) {
        if (kitItems == null) return;
        player.getInventory().addItem(kitItems);
    }

    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "Nachricht nicht gesetzt!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length < 1) {
            sender.sendMessage(getMessage("only_players_with_args"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("usage_no_args"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("set")) {
            if (!sender.hasPermission("starterkit.admin")) {
                sender.sendMessage(getMessage("no_permission"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("only_players"));
                return true;
            }
            Player p = (Player) sender;
            ItemStack[] items = p.getInventory().getContents();
            List<ItemStack> filtered = new ArrayList<>();
            for (ItemStack item : items) {
                if (item != null && item.getType().isItem()) {
                    filtered.add(item);
                }
            }
            saveKitToConfig(filtered.toArray(new ItemStack[0]));
            sender.sendMessage(getMessage("kit_saved"));
            return true;
        } else if (sub.equals("give")) {
            if (!sender.hasPermission("starterkit.admin")) {
                sender.sendMessage(getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(getMessage("usage_give_args"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(getMessage("player_not_found"));
                return true;
            }
            if (kitItems == null || kitItems.length == 0) {
                sender.sendMessage(getMessage("no_kit_set"));
                return true;
            }
            giveKit(target);
            markAsReceived(target.getUniqueId());
            sender.sendMessage(getMessage("starterkit_given_sender").replace("%player%", target.getName()));
            target.sendMessage(getMessage("starterkit_received"));
            return true;
        } else {
            sender.sendMessage(getMessage("unknown_subcommand"));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
