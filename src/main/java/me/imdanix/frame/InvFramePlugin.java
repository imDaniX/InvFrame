package me.imdanix.frame;

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.Consumer;

public final class InvFramePlugin extends JavaPlugin implements Listener {
    private final NamespacedKey invisKey = new NamespacedKey(this, "invisible");
    private final ItemStack invisFrameItem = editStack(new ItemStack(Material.ITEM_FRAME), (item) -> item.editMeta(meta -> {
        meta.getPersistentDataContainer().set(invisKey, PersistentDataType.BYTE, (byte) 1);
        meta.lore(Collections.singletonList(Component.translatable("effect.minecraft.invisibility").color(NamedTextColor.GRAY)));
    }));
    private final ItemStack invisFrameItemGlow = editStack(invisFrameItem.clone(), (item) -> item.setType(Material.GLOW_ITEM_FRAME));

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            player.getInventory().addItem(invisFrameItem);
            player.getInventory().addItem(invisFrameItemGlow);
            player.sendMessage(Component.text("Gave you invisible item frames").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Cannot be executed from the console").color(NamedTextColor.RED));
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFramePlace(HangingPlaceEvent event) {
        ItemStack item = event.getItemStack();
        if (item != null
                && (item.getType() == Material.ITEM_FRAME || item.getType() == Material.GLOW_ITEM_FRAME)
                && item.hasItemMeta()
                && isInvFrame(item.getItemMeta())) {
            event.getEntity().getPersistentDataContainer().set(invisKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemToFrame(PlayerItemFrameChangeEvent event) {
        ItemFrame frame = event.getItemFrame();
        if (!isInvFrame(frame)) return;
        if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE) {
            frame.setVisible(false);
        } else if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE) {
            frame.setVisible(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemFromFrame(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame frame && isInvFrame(frame)) {
            frame.setVisible(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFrameBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame frame && isInvFrame(frame)) {
            event.setCancelled(true);
            frame.remove();
            Location location = center(frame.getLocation());
            World world = location.getWorld();
            if (frame instanceof GlowItemFrame) {
                world.dropItemNaturally(location, invisFrameItemGlow);
                world.playSound(location, Sound.ENTITY_GLOW_ITEM_FRAME_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
            } else {
                world.dropItemNaturally(location, invisFrameItem);
                world.playSound(location, Sound.ENTITY_ITEM_FRAME_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
            if (!frame.getItem().getType().isAir()) world.dropItemNaturally(location, frame.getItem());
        }
    }
    
    private boolean isInvFrame(PersistentDataHolder dataHolder) {
        return dataHolder.getPersistentDataContainer().has(invisKey);
    }

    // I'm lazy
    private static ItemStack editStack(ItemStack item, Consumer<ItemStack> action) {
        action.accept(item);
        return item;
    }

    private static Location center(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5);
    }
}
