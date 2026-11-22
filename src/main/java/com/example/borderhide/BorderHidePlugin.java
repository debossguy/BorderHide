package com.example.borderhide;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BorderHidePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        PacketEvents.getAPI().getEventManager().registerListener(
            new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
                @Override
                public void onPacketSend(PacketSendEvent event) {
                    if (event.getPacketType() != PacketType.Play.Server.CHUNK_DATA) return;
                    if (!(event.getPlayer() instanceof Player player)) return;

                    WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
                    int chunkMinX = packet.getColumn().getX() << 4;
                    int chunkMaxX = chunkMinX + 15;
                    int chunkMinZ = packet.getColumn().getZ() << 4;
                    int chunkMaxZ = chunkMinZ + 15;

                    WorldBorder border = player.getWorld().getWorldBorder();
                    double half = border.getSize() / 2.0;
                    double minX = border.getCenter().getX() - half;
                    double maxX = border.getCenter().getX() + half;
                    double minZ = border.getCenter().getZ() - half;
                    double maxZ = border.getCenter().getZ() + half;

                    if (chunkMaxX < minX || chunkMinX > maxX || chunkMaxZ < minZ || chunkMinZ > maxZ) {
                        event.setCancelled(true);
                    }
                }
            }
        );
        getLogger().info("BorderHide enabled: hiding chunks outside the world border.");
    }
}
