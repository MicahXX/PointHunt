package me.micahcode.pointHunt.handlers;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointHuntListener implements Listener {

    private final Map<UUID, Integer> playerPoints = new HashMap<>();
    private final Map<EntityType, Integer> mobPoints = new HashMap<>();
    private final Map<Material, Integer> blockPoints = new HashMap<>();

    private PointHunt plugin;

    public void setPlugin(PointHunt plugin) {
        this.plugin = plugin;
        loadMappingsFromConfig();
    }

    public PointHuntListener() {
        // Default mob points (safe baseline for all living entities)
        for (EntityType type : EntityType.values()) {
            if (type.getEntityClass() != null && LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
                mobPoints.put(type, 1);
            }
        }

        // easy mobs
        mobPoints.put(EntityType.ZOMBIE, 2);
        mobPoints.put(EntityType.ZOMBIE_VILLAGER, 3);
        mobPoints.put(EntityType.HUSK, 3);
        mobPoints.put(EntityType.DROWNED, 3);
        mobPoints.put(EntityType.SKELETON, 3);
        mobPoints.put(EntityType.STRAY, 3);
        mobPoints.put(EntityType.SPIDER, 2);
        mobPoints.put(EntityType.CAVE_SPIDER, 4);
        mobPoints.put(EntityType.SILVERFISH, 2);
        mobPoints.put(EntityType.ENDERMITE, 4);
        mobPoints.put(EntityType.SLIME, 3);
        mobPoints.put(EntityType.PHANTOM, 5);
        mobPoints.put(EntityType.ZOMBIE_HORSE, 2);
        mobPoints.put(EntityType.SKELETON_HORSE, 2);
        mobPoints.put(EntityType.ZOMBIFIED_PIGLIN, 3);

        // Mid difficulty
        mobPoints.put(EntityType.CREEPER, 6);
        mobPoints.put(EntityType.ENDERMAN, 15);
        mobPoints.put(EntityType.BLAZE, 15);
        mobPoints.put(EntityType.MAGMA_CUBE, 8);
        mobPoints.put(EntityType.GUARDIAN, 12);
        mobPoints.put(EntityType.HOGLIN, 9);
        mobPoints.put(EntityType.ZOGLIN, 9);
        mobPoints.put(EntityType.PIGLIN, 2);
        mobPoints.put(EntityType.PILLAGER, 8);
        mobPoints.put(EntityType.VINDICATOR, 12);
        mobPoints.put(EntityType.EVOKER, 30);
        mobPoints.put(EntityType.VEX, 20);
        mobPoints.put(EntityType.RAVAGER, 35);
        mobPoints.put(EntityType.WITHER_SKELETON, 28);
        mobPoints.put(EntityType.GHAST, 20);

        // High difficulty mobs
        mobPoints.put(EntityType.PIGLIN_BRUTE, 40);
        mobPoints.put(EntityType.SHULKER, 25);
        mobPoints.put(EntityType.ILLUSIONER, 25);

        // Bosses
        mobPoints.put(EntityType.ELDER_GUARDIAN, 100);
        mobPoints.put(EntityType.WITHER, 250);
        mobPoints.put(EntityType.WARDEN, 375);
        mobPoints.put(EntityType.ENDER_DRAGON, 500);

        // ores (defaults)
        blockPoints.put(Material.COAL_ORE, 1);
        blockPoints.put(Material.DEEPSLATE_COAL_ORE, 1);
        blockPoints.put(Material.IRON_ORE, 2);
        blockPoints.put(Material.DEEPSLATE_IRON_ORE, 2);
        blockPoints.put(Material.COPPER_ORE, 2);
        blockPoints.put(Material.DEEPSLATE_COPPER_ORE, 2);
        blockPoints.put(Material.GOLD_ORE, 4);
        blockPoints.put(Material.DEEPSLATE_GOLD_ORE, 4);
        blockPoints.put(Material.REDSTONE_ORE, 2);
        blockPoints.put(Material.DEEPSLATE_REDSTONE_ORE, 2);
        blockPoints.put(Material.LAPIS_ORE, 3);
        blockPoints.put(Material.DEEPSLATE_LAPIS_ORE, 3);
        blockPoints.put(Material.DIAMOND_ORE, 12);
        blockPoints.put(Material.DEEPSLATE_DIAMOND_ORE, 12);
        blockPoints.put(Material.EMERALD_ORE, 12);
        blockPoints.put(Material.DEEPSLATE_EMERALD_ORE, 12);
        blockPoints.put(Material.NETHER_QUARTZ_ORE, 1);
        blockPoints.put(Material.NETHER_GOLD_ORE, 1);
        blockPoints.put(Material.ANCIENT_DEBRIS, 30);
    }
    private void loadMappingsFromConfig() {
        if (plugin == null) return;
        FileConfiguration cfg = plugin.getConfig();

        boolean merge = true;
        if (cfg.isConfigurationSection("settings")) {
            merge = cfg.getBoolean("settings.mappings-merge", true);
        }

        // MOB POINTS
        if (cfg.isConfigurationSection("mob-points")) {
            ConfigurationSection section = cfg.getConfigurationSection("mob-points");
            if (!merge) {
                mobPoints.clear();
            }
            assert section != null;
            for (String key : section.getKeys(false)) {
                try {
                    EntityType et = EntityType.valueOf(key.toUpperCase());
                    int pts = section.getInt(key);
                    mobPoints.put(et, pts);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("PointHunt: invalid entity type in mob-points: " + key);
                }
            }
            plugin.getLogger().info("PointHunt: loaded mob-points from config (" + (merge ? "merged" : "replaced") + ")");
        }

        // BLOCK POINTS
        if (cfg.isConfigurationSection("block-points")) {
            ConfigurationSection section = cfg.getConfigurationSection("block-points");
            if (!merge) {
                blockPoints.clear();
            }
            assert section != null;
            for (String key : section.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    int pts = section.getInt(key);
                    blockPoints.put(mat, pts);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("PointHunt: invalid material in block-points: " + key);
                }
            }
            plugin.getLogger().info("PointHunt: loaded block-points from config (" + (merge ? "merged" : "replaced") + ")");
        }
    }

    // Public method to call after a reload
    public void reloadMappingsFromConfig() {
        loadMappingsFromConfig();
    }

    private String formatName(String raw) {
        String lower = raw.toLowerCase().replace("_", " ");
        String[] words = lower.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void awardPoints(Player player, String objective, int points) {
        if (plugin == null || plugin.getRemainingSeconds() <= 0 || !plugin.isTimerRunning()) {
            return;
        }

        // check global multiplier
        double multiplier = 1.0;
        if (plugin.getConfig().isConfigurationSection("settings")) {
            multiplier = plugin.getConfig().getDouble("settings.multiplier", 1.0);
        }
        int awarded = (int) Math.round(points * multiplier);

        UUID id = player.getUniqueId();
        int current = playerPoints.getOrDefault(id, 0);
        int updated = current + awarded;
        playerPoints.put(id, updated);

        player.sendMessage("§3" + objective + " §8» §6(§a+" + awarded + " pts§6)");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Ignore creative players
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Material type = event.getBlock().getType();

        // optionally ignore silk-touch
        boolean ignoreSilk = plugin.getConfig().getBoolean("settings.ignore-silk-touch", false);
        if (!ignoreSilk && player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
            if (plugin.getConfig().getBoolean("settings.silk-touch-message", true)) {
                player.sendMessage(
                        net.kyori.adventure.text.Component.text(
                                "You don't earn points while mining with Silk Touch.",
                                net.kyori.adventure.text.format.NamedTextColor.DARK_RED
                        )
                );
            }
            return;
        }

        // check if block awarding is enabled
        if (!plugin.getConfig().getBoolean("settings.enable-block-points", true)) return;

        // awards points
        if (blockPoints.containsKey(type)) {
            int points = blockPoints.get(type);
            String name = formatName(type.name());

            awardPoints(player, name + " mined", points);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer == null) return;
        if (killer.getGameMode() == GameMode.CREATIVE) return;

        // check if mob awarding is enabled
        if (!plugin.getConfig().getBoolean("settings.enable-mob-points", true)) return;

        EntityType type = event.getEntityType();
        int points = mobPoints.getOrDefault(type, 1);
        String name = formatName(type.name());

        awardPoints(killer, name + " slain", points);
    }

    public int getPoints(Player player) {
        return playerPoints.getOrDefault(player.getUniqueId(), 0);
    }

    public Map<UUID, Integer> getAllPoints() {
        return playerPoints;
    }
}