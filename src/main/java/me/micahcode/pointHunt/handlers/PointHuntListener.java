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
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDeathEvent;

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
    }

    public PointHuntListener() {
        // Default mob points
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
        mobPoints.put(EntityType.BREEZE, 20);

        // High difficulty mobs
        mobPoints.put(EntityType.PIGLIN_BRUTE, 40);
        mobPoints.put(EntityType.SHULKER, 25);
        mobPoints.put(EntityType.ILLUSIONER, 25);

        // Bosses
        mobPoints.put(EntityType.ELDER_GUARDIAN, 100);
        mobPoints.put(EntityType.WITHER, 250);
        mobPoints.put(EntityType.WARDEN, 375);
        mobPoints.put(EntityType.ENDER_DRAGON, 500);

        // ores
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

        UUID id = player.getUniqueId();
        int current = playerPoints.getOrDefault(id, 0);
        int updated = current + points;
        playerPoints.put(id, updated);

        player.sendMessage("§3" + objective + " §8» §6(§a+" + points + " pts§6)");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Ignore creative players
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Material type = event.getBlock().getType();

        // checks if Player has silktouch
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
            player.sendMessage(
                    net.kyori.adventure.text.Component.text(
                            "You don't earn points while mining with Silk Touch.",
                            net.kyori.adventure.text.format.NamedTextColor.DARK_RED
                    )
            );
            return;
        }

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
