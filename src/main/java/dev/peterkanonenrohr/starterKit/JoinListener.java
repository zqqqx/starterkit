package dev.peterkanonenrohr.starterKit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class JoinListener implements Listener {

    private final StarterKit plugin;

    public JoinListener(StarterKit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("starterkit.use")) return;
        if (plugin.hasReceivedKit(player.getUniqueId())) return;
        if (plugin.getKitItems() == null || plugin.getKitItems().length == 0) return;

        plugin.giveKit(player);
        plugin.markAsReceived(player.getUniqueId());
        player.sendMessage(plugin.getMessage("starterkit_received"));
    }
}
