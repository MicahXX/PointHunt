package me.micahcode.pointHunt.handlers;
import me.micahcode.pointHunt.util.Msg;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HuntCommand implements CommandExecutor, TabCompleter {

    private final PointHuntListener listener;
    private final PointHunt plugin;

    public HuntCommand(PointHuntListener listener, PointHunt plugin) {
        this.listener = listener;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use this command!");
                return true;
            }

            int points = listener.getPoints(player);

            player.sendMessage(Msg.c("§6§lRank Hunt"));
            player.sendMessage(Msg.c("§7You currently have §a" + points + " §7points!"));

            sendActionBar(player, "§6§lPoints: §a" + points);
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can view the leaderboard GUI!");
                return true;
            }

            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
            }

            openLeaderboard(player, page);
            return true;
        }

        if (args[0].equalsIgnoreCase("setPoints")) {
            if (!sender.hasPermission("pointhunt.setPoints")) {
                sender.sendMessage("§cYou don’t have permission to set points!");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /hunt setPoints <player> <points>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int points;
            try {
                points = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cPoints must be a number!");
                return true;
            }

            // Update in-memory
            listener.getAllPoints().put(target.getUniqueId(), points);
            plugin.getConfig().set("points." + target.getUniqueId(), points);
            plugin.saveConfig();

            sender.sendMessage(Component.text("§aSet " + (target.getName() != null ? target.getName() : target.getUniqueId().toString()) + "’s points to §e" + points + "§a!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("resetPoints")) {
            if (!sender.hasPermission("pointhunt.resetPoints")) {
                sender.sendMessage("§cYou don’t have permission to reset the leaderboard!");
                return true;
            }

            listener.getAllPoints().clear();
            // Clear saved points in config so save/load won't restore them
            plugin.getConfig().set("points", null);
            plugin.saveConfig();

            Bukkit.broadcast(Component.text("§c§lThe Point Hunt leaderboard has been reset!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("setTimer")) {
            if (!sender.hasPermission("pointhunt.setTimer")) {
                sender.sendMessage("§cYou don’t have permission to set the timer!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§cUsage: /hunt setTimer <time>");
                sender.sendMessage("§7Examples: /hunt setTimer 1h | /hunt setTimer 30m | /hunt setTimer 90s");
                return true;
            }

            String timeArg = args[1].toLowerCase();
            long seconds;

            try {
                String num = timeArg.replaceAll("[^0-9]", "");
                if (timeArg.endsWith("h")) {
                    seconds = Long.parseLong(num) * 3600;
                } else if (timeArg.endsWith("m")) {
                    seconds = Long.parseLong(num) * 60;
                } else {
                    seconds = Long.parseLong(num); // default: seconds
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid time format! Use 1h, 30m, 90s, etc.");
                return true;
            }

            plugin.setTimerSeconds(seconds);
            sender.sendMessage(Component.text("§aTimer set for §e" + plugin.formatTime(seconds) + "§a!"));
            Bukkit.broadcast(Component.text("§6⏳ The Point Hunt will end in §e" + plugin.formatTime(seconds) + "§6!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("cancelTimer")) {
            if (!sender.hasPermission("pointhunt.cancelTimer")) {
                sender.sendMessage("§cYou don’t have permission to stop the timer!");
                return true;
            }
            plugin.setTimerSeconds(0);
            sender.sendMessage(Component.text("§aThe Rank Hunt timer has been stopped!"));
            Bukkit.broadcast(Component.text("§c⏳ The point hunt event timer was stopped by an admin."));
            return true;
        }

        if (args[0].equalsIgnoreCase("pauseTimer")) {
            if (!sender.hasPermission("pointhunt.setTimer")) {
                sender.sendMessage("§cYou don’t have permission to pause the timer!");
                return true;
            }
            plugin.pauseTimer();
            Bukkit.broadcast(Component.text("§e⏸ The Rank Hunt timer has been paused!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("resumeTimer")) {
            if (!sender.hasPermission("pointhunt.resumeTimer")) {
                sender.sendMessage("§cYou don’t have permission to resume the timer!");
                return true;
            }
            plugin.resumeTimer();
            Bukkit.broadcast(Component.text("§e⏸ The Rank Hunt timer has been resumed!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pointhunt.admin.reload")) {
                sender.sendMessage("§cYou don't have permission to reload the configuration!");
                return true;
            }

            plugin.reloadConfig();
            listener.reloadMappingsFromConfig();
            sender.sendMessage(Component.text("§aPointHunt configuration reloaded."));
            return true;
        }

        sender.sendMessage("§cUsage: /hunt [top <page>|setPoints <player> <points>|resetPoints|setTimer <time>|cancelTimer|pauseTimer|resumeTimer|reload]");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();

            subs.add("top");
            if (sender.hasPermission("pointhunt.setPoints")) subs.add("setPoints");
            if (sender.hasPermission("pointhunt.resetPoints")) subs.add("resetPoints");
            if (sender.hasPermission("pointhunt.setTimer")) {
                subs.add("setTimer");
                subs.add("pauseTimer");
                subs.add("resumeTimer");
            }
            if (sender.hasPermission("pointhunt.cancelTimer")) subs.add("cancelTimer");
            if (sender.hasPermission("pointhunt.admin.reload")) subs.add("reload");
            return subs;
        }
        return Collections.emptyList();
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(Msg.c(message));
    }

    // /top command
    public void openLeaderboard(Player player, int page) {
        List<Map.Entry<UUID, Integer>> allEntries = listener.getAllPoints().entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();

        int pageSize = 45;
        int totalPages = (int) Math.ceil(allEntries.size() / (double) pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        Inventory gui = Bukkit.createInventory(
                null,
                54,
                Msg.c("§6Point Hunt (Page " + page + "/" + totalPages + ")")
        );

        int startIndex = (page - 1) * pageSize;
        int slot = 0;
        for (int i = startIndex; i < Math.min(startIndex + pageSize, allEntries.size()); i++) {
            Map.Entry<UUID, Integer> entry = allEntries.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);
                meta.displayName(Msg.c("§b" + name + " §7(#" + (i + 1) + ")"));
                meta.lore(List.of(Msg.c("§7Points: §a" + entry.getValue())));
                head.setItemMeta(meta);
            }
            gui.setItem(slot, head);
            slot++;
        }

        // Next Page
        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            var meta = next.getItemMeta();
            meta.displayName(Msg.c("§aNext Page ▶"));
            next.setItemMeta(meta);
            gui.setItem(53, next);
        }

        // Previous Page
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            var meta = prev.getItemMeta();
            meta.displayName(Msg.c("§a◀ Previous Page"));
            prev.setItemMeta(meta);
            gui.setItem(45, prev);
        }

        // Close Button
        ItemStack close = new ItemStack(Material.BARRIER);
        var closeMeta = close.getItemMeta();
        closeMeta.displayName(Msg.c("§cClose"));
        close.setItemMeta(closeMeta);
        gui.setItem(49, close);

        player.openInventory(gui);
    }
}