package me.micahcode.pointHunt.handlers;
import me.micahcode.pointHunt.util.Msg;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PointHunt extends JavaPlugin implements Listener {

    private PointHuntListener listener;

    // Timer/shutdown state
    private long huntTimerEnd = -1; // timestamp when hunt ends, -1 = disabled
    private final Set<Long> announced = new HashSet<>(); // reminders sent
    private boolean shuttingDown = false;
    private boolean locked = false;

    // Pause/Resume state
    private boolean paused = false;
    private long remainingOnPause = -1;

    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        listener = new PointHuntListener();
        listener.setPlugin(this);
        loadPoints();

        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("RankHunt enabled!");

        FileConfiguration cfg = getConfig();
        // action bar and update interval
        boolean actionBarEnabled = cfg.getBoolean("settings.action-bar-enabled", true);
        int updateIntervalTicks = Math.max(1, cfg.getInt("settings.update-interval-ticks", 1));

        // periodic save interval (seconds)
        int saveIntervalSeconds = Math.max(0, cfg.getInt("settings.save-interval-seconds", 300));
        if (saveIntervalSeconds > 0) {
            long periodTicks = saveIntervalSeconds * 20L;
            saveTask = Bukkit.getScheduler().runTaskTimer(this, this::savePoints, periodTicks, periodTicks);
            getLogger().info("PointHunt: scheduled periodic saves every " + saveIntervalSeconds + "s");
        }

        // Update action bar & handle timer
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Map<UUID, Integer> allPoints = listener.getAllPoints();
            List<Map.Entry<UUID, Integer>> leaderboard = allPoints.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .toList();

            long remaining = getRemainingSeconds();
            if (remaining > 0 && !paused) {
                if ((remaining == 3600 || remaining == 600 || remaining == 60) && !announced.contains(remaining)) {
                    announced.add(remaining);
                    Bukkit.broadcast(Msg.c("§6⏳ The point hunt ends in §e" + formatTime(remaining) + "§6!"));
                }

                if (remaining == 5 && !shuttingDown) {
                    shuttingDown = true;
                    announced.clear();

                    final int[] countdown = {5};
                    AtomicReference<BukkitTask> ref = new AtomicReference<>();
                    ref.set(Bukkit.getScheduler().runTaskTimer(this, () -> {
                        if (countdown[0] > 0) {
                            Bukkit.broadcast(Msg.c("§cServer closing in §e" + countdown[0] + "§c..."));
                            countdown[0]--;
                        } else {
                            BukkitTask t = ref.get();
                            if (t != null) t.cancel();

                            // Build end message and show top players
                            int topCount = cfg.getInt("settings.end-top-count", 5);
                            String endTitle = cfg.getString("messages.hunt-ended-title", "§cTHE POINT HUNT HAS ENDED!");

                            for (Player p : Bukkit.getOnlinePlayers()) {
                                StringBuilder msg = new StringBuilder();
                                msg.append(endTitle).append("\n\n");

                                int playerPoints = listener.getPoints(p);
                                int rank = 0;
                                for (int i = 0; i < leaderboard.size(); i++) {
                                    if (leaderboard.get(i).getKey().equals(p.getUniqueId())) {
                                        rank = i + 1;
                                        break;
                                    }
                                }
                                msg.append("§7Your Points: §a").append(playerPoints)
                                        .append(" §7(Rank: §e#").append(rank).append("§7)\n\n");

                                msg.append("§6§lTop ").append(topCount).append(" Players:\n");
                                for (int i = 0; i < Math.min(topCount, leaderboard.size()); i++) {
                                    Map.Entry<UUID, Integer> entry = leaderboard.get(i);
                                    OfflinePlayer off = Bukkit.getOfflinePlayer(entry.getKey());
                                    String name = (off.getName() != null) ? off.getName() : "Unknown";
                                    msg.append("§e#").append(i + 1).append(" §b").append(name)
                                            .append(" §7- §a").append(entry.getValue()).append(" pts\n");
                                }

                                p.kick(Msg.c(msg.toString()));
                            }

                            locked = true;
                            boolean shutdownOnEnd = cfg.getBoolean("settings.shutdown-on-end", true);
                            if (shutdownOnEnd) {
                                Bukkit.getScheduler().runTaskLater(PointHunt.this, Bukkit::shutdown, 20L);
                            } else {
                                Bukkit.broadcast(Msg.c("§aPoint hunt ended (server will remain online)."));
                                locked = false;
                                shuttingDown = false;
                            }
                        }
                    }, 0L, 20L));
                }
            }

            // action bar
            if (actionBarEnabled) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int points = listener.getPoints(player);

                    String rankText = "";
                    if (points > 0) {
                        int rank = 0;
                        for (int i = 0; i < leaderboard.size(); i++) {
                            if (leaderboard.get(i).getKey().equals(player.getUniqueId())) {
                                rank = i + 1;
                                break;
                            }
                        }
                        if (rank > 0) rankText = " §7| §eRank: §b#" + rank;
                    }

                    String timerText;
                    if (paused) {
                        timerText = "§e⏸ Paused §7| ";
                    } else {
                        timerText = (remaining > 0) ? "§c⏳ " + formatTime(remaining) + " §7| " : "";
                    }

                    player.sendActionBar(
                            Msg.c(timerText + "§6§lPoints: §a" + points + rankText)
                    );
                }
            }
        }, 0L, updateIntervalTicks);

        HuntCommand huntCommand = new HuntCommand(listener, this);
        Objects.requireNonNull(this.getCommand("hunt")).setExecutor(huntCommand);
        Objects.requireNonNull(this.getCommand("hunt")).setTabCompleter(huntCommand);

        getServer().getPluginManager().registerEvents(new LeaderboardGUI(huntCommand), this);
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (locked) {
            player.kick(Msg.c("§cThe Rank Hunt has ended!"));
        }
    }

    @Override
    public void onDisable() {
        if (saveTask != null) saveTask.cancel();
        savePoints();
        getLogger().info("RankHunt disabled!");
    }

    private void savePoints() {
        FileConfiguration config = getConfig();
        config.set("points", null);
        for (Map.Entry<UUID, Integer> entry : listener.getAllPoints().entrySet()) {
            config.set("points." + entry.getKey().toString(), entry.getValue());
        }
        saveConfig();
    }

    private void loadPoints() {
        FileConfiguration config = getConfig();
        if (config.isConfigurationSection("points")) {
            Set<String> keys = Objects.requireNonNull(config.getConfigurationSection("points")).getKeys(false);
            for (String uuidStr : keys) {
                UUID uuid = UUID.fromString(uuidStr);
                int points = config.getInt("points." + uuidStr);
                listener.getAllPoints().put(uuid, points);
            }
        }
    }

    // Timer controls
    public void setTimerSeconds(long seconds) {
        if (seconds <= 0) {
            huntTimerEnd = -1;
            announced.clear();
            shuttingDown = false;
            locked = false;
            paused = false;
            remainingOnPause = -1;
        } else {
            huntTimerEnd = System.currentTimeMillis() + seconds * 1000;
            announced.clear();
            shuttingDown = false;
            locked = false;
            paused = false;
            remainingOnPause = -1;
        }
    }

    public long getRemainingSeconds() {
        if (huntTimerEnd <= 0) return -1;
        long remaining = (huntTimerEnd - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) return hours + "h " + minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    // Pause/Resume
    public void pauseTimer() {
        if (!paused && huntTimerEnd > 0) {
            remainingOnPause = getRemainingSeconds();
            paused = true;
            huntTimerEnd = -1;
        }
    }

    public void resumeTimer() {
        if (paused && remainingOnPause > 0) {
            huntTimerEnd = System.currentTimeMillis() + remainingOnPause * 1000;
            paused = false;
            remainingOnPause = -1;
        }
    }

    public boolean isTimerRunning() {
        return huntTimerEnd > 0 && !paused;
    }
}