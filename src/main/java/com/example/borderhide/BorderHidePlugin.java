package com.example.borderhide;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BorderHidePlugin extends JavaPlugin implements TabCompleter {

    private boolean enabled = true;
    private int bufferChunks = 1;

    @Override
    public void onEnable() {
        enabled = getConfig().getBoolean("enabled", true);
        bufferChunks = getConfig().getInt("buffer-chunks", 1);

        if (getCommand("borderhide") != null) {
            getCommand("borderhide").setExecutor(this);
            getCommand("borderhide").setTabCompleter(this);
        }

        PacketEvents.getAPI().getEventManager().registerListener(
            new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
                @Override
                public void onPacketSend(PacketSendEvent event) {
                    if (!enabled) return;
                    if (event.getPacketType() != PacketType.Play.Server.CHUNK_DATA) return;

                    Player player = null;
                    if (event.getPlayer() instanceof Player p) {
                        player = p;
                    } else if (event.getUser() != null && event.getUser().getUUID() != null) {
                        player = Bukkit.getPlayer(event.getUser().getUUID());
                    }
                    if (player == null) return;

                    WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
                    double chunkMinX = packet.getColumn().getX() << 4;
                    double chunkMaxX = chunkMinX + 16.0;
                    double chunkMinZ = packet.getColumn().getZ() << 4;
                    double chunkMaxZ = chunkMinZ + 16.0;

                    WorldBorder border = player.getWorld().getWorldBorder();
                    double half = (border.getSize() / 2.0) + (bufferChunks * 16.0);
                    double minX = border.getCenter().getX() - half;
                    double maxX = border.getCenter().getX() + half;
                    double minZ = border.getCenter().getZ() - half;
                    double maxZ = border.getCenter().getZ() + half;

                    if (chunkMaxX <= minX || chunkMinX >= maxX || chunkMaxZ <= minZ || chunkMinZ >= maxZ) {
                        event.setCancelled(true);
                    }
                }
            }
        );

        getLogger().info("BorderHide enabled (active=" + enabled + ", buffer=" + bufferChunks + " chunks).");
    }

    private void updateBuffer(CommandSender sender, int newValue) {
        if (newValue < -16 || newValue > 32) {
            sender.sendMessage(ChatColor.RED + "Buffer must stay between -16 and 32 chunks.");
            return;
        }
        bufferChunks = newValue;
        getConfig().set("buffer-chunks", bufferChunks);
        saveConfig();
        sender.sendMessage(ChatColor.GOLD + "[BorderHide] " + ChatColor.WHITE
            + "Buffer set to " + ChatColor.AQUA + bufferChunks + " chunk(s)" + ChatColor.WHITE
            + " (press F3+A in-game to refresh chunks).");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args.equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.GOLD + "[BorderHide] " + ChatColor.WHITE
                + "Status: " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")
                + ChatColor.WHITE + " | Buffer: " + ChatColor.AQUA + bufferChunks + " chunk(s)");
            return true;
        }

        switch (args.toLowerCase()) {
            case "on", "enable" -> {
                enabled = true;
                getConfig().set("enabled", true);
                saveConfig();
                sender.sendMessage(ChatColor.GOLD + "[BorderHide] " + ChatColor.GREEN + "Enabled.");
            }
            case "off", "disable" -> {
                enabled = false;
                getConfig().set("enabled", false);
                saveConfig();
                sender.sendMessage(ChatColor.GOLD + "[BorderHide] " + ChatColor.RED + "Disabled.");
            }
            case "add", "+" -> updateBuffer(sender, bufferChunks + 1);
            case "sub", "subtract", "-" -> updateBuffer(sender, bufferChunks - 1);
            case "buffer" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /borderhide buffer <chunks>");
                    return true;
                }
                try {
                    updateBuffer(sender, Integer.parseInt(args));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number: " + args);
                }
            }
            default -> sender.sendMessage(ChatColor.RED + "Usage: /borderhide <on|off|add|sub|buffer <chunks>|status>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "add", "sub", "buffer", "status");
        }
        if (args.length == 2 && args.equalsIgnoreCase("buffer")) {
            return List.of("-1", "0", "1", "2");
        }
        return List.of();
    }
}
