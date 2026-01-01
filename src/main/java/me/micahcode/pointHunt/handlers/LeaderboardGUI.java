package me.micahcode.pointHunt.handlers;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class LeaderboardGUI implements Listener {

    private final HuntCommand huntCommand;

    public LeaderboardGUI(HuntCommand huntCommand) {
        this.huntCommand = huntCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (!title.startsWith("Point Hunt")) return;

        event.setCancelled(true);

        if (event.isShiftClick() || event.getClick().isKeyboardClick() || event.getClick().isCreativeAction()) return;

        int slot = event.getRawSlot();

        int currentPage = 1;
        try {
            String[] parts = title.split("Page ");
            if (parts.length > 1) {
                String[] nums = parts[1].replace(")", "").split("/");
                currentPage = Integer.parseInt(nums[0]);
            }
        } catch (Exception ignored) {}

        switch (slot) {
            case 45 -> huntCommand.openLeaderboard(player, currentPage - 1);
            case 53 -> huntCommand.openLeaderboard(player, currentPage + 1);
            case 49 -> player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (title.startsWith("Point Hunt")) {
            event.setCancelled(true);
        }
    }
}