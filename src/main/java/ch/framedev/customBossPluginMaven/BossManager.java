package ch.framedev.customBossPluginMaven;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class BossManager implements Listener {

    private static final double DISPLAY_RADIUS = 48.0;
    private static final double DISPLAY_RADIUS_SQUARED =
            DISPLAY_RADIUS * DISPLAY_RADIUS;

    private final JavaPlugin plugin;
    private final NamespacedKey bossKey;
    private final File bossesFile;

    private final Map<String, CustomBoss> bosses = new LinkedHashMap<>();
    private final Map<String, BaseBoss> registeredBosses =
            new LinkedHashMap<>();
    private final Map<UUID, ActiveBoss> activeBosses = new HashMap<>();

    private BukkitTask displayTask;

    public BossManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.bossKey = new NamespacedKey(plugin, "custom_boss_id");
        this.bossesFile = new File(plugin.getDataFolder(), "bosses.yml");
    }

    public void initialize() {
        if (!plugin.getDataFolder().exists()
                && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning(
                    "Could not create the plugin data directory."
            );
        }

        loadBosses();

        plugin.getServer()
                .getPluginManager()
                .registerEvents(this, plugin);

        trackLoadedBossEntities();
        startDisplayTask();
    }

    public CustomBoss createBoss(
            String id,
            String displayName,
            double health,
            EntityType entityType
    ) {
        String normalizedId = normalizeId(id);

        if (bosses.containsKey(normalizedId)) {
            throw new IllegalArgumentException(
                    "A boss with ID '" + normalizedId + "' already exists"
            );
        }

        CustomBoss boss = new CustomBoss(
                bossKey,
                normalizedId,
                displayName,
                health,
                entityType,
                false,
                false,
                new ArrayList<>(),
                new ArrayList<>()
        );

        bosses.put(normalizedId, boss);
        saveBosses();

        return boss;
    }

    public CustomBoss registerBoss(BaseBoss baseBoss) {
        Objects.requireNonNull(baseBoss, "BaseBoss cannot be null");

        String normalizedId = normalizeId(baseBoss.getRegistryId());

        CustomBoss boss = new CustomBoss(
                bossKey,
                normalizedId,
                baseBoss.getColoredDisplayName(),
                baseBoss.getHealth(),
                baseBoss.getEntityType(),
                baseBoss.isBossBar(),
                baseBoss.isActionBar(),
                new ArrayList<>(baseBoss.getLootTable()),
                new ArrayList<>(baseBoss.getPotionEffects())
        );

        boss.copySettingsFrom(baseBoss);
        bosses.put(normalizedId, boss);
        registeredBosses.put(normalizedId, baseBoss);
        saveBosses();

        return boss;
    }

    public LivingEntity spawnBoss(BaseBoss baseBoss, Location location) {
        Objects.requireNonNull(baseBoss, "BaseBoss cannot be null");

        return spawnBoss(baseBoss.getRegistryId(), location);
    }

    public boolean unregisterBoss(BaseBoss baseBoss) {
        Objects.requireNonNull(baseBoss, "BaseBoss cannot be null");

        return unregisterBoss(baseBoss.getRegistryId());
    }

    public boolean unregisterBoss(String id) {
        String normalizedId = normalizeId(id);
        boolean removedApiBoss = registeredBosses.remove(normalizedId) != null;
        boolean removedCustomBoss = deleteBoss(normalizedId);

        return removedApiBoss || removedCustomBoss;
    }

    public boolean deleteBoss(String id) {
        String normalizedId = normalizeId(id);
        CustomBoss removed = bosses.remove(normalizedId);

        if (removed == null) {
            return false;
        }

        registeredBosses.remove(normalizedId);
        removeActiveBosses(removed.getId());
        saveBosses();
        return true;
    }

    public Optional<CustomBoss> getBoss(String id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(bosses.get(normalizeId(id)));
    }

    public Collection<CustomBoss> getBosses() {
        return List.copyOf(bosses.values());
    }

    public LivingEntity spawnBoss(String id, Location location) {
        CustomBoss boss = getBoss(id).orElse(null);

        if (boss == null) {
            return null;
        }

        LivingEntity entity = boss.spawnBoss(location);

        if (entity != null) {
            BaseBoss registeredBoss = registeredBosses.get(boss.getId());

            if (registeredBoss != null) {
                runBossHook(
                        registeredBoss,
                        () -> registeredBoss.applyToEntity(entity)
                );
                runBossHook(
                        registeredBoss,
                        () -> registeredBoss.onSpawn(entity)
                );
            }

            trackActiveBoss(entity, boss);
        }

        return entity;
    }

    public void saveBosses() {
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection bossesSection =
                configuration.createSection("bosses");

        for (CustomBoss boss : bosses.values()) {
            String path = boss.getId();

            bossesSection.set(path + ".name", boss.getName());
            bossesSection.set(path + ".health", boss.getHealth());
            bossesSection.set(
                    path + ".entity-type",
                    boss.getEntityType().name()
            );
            bossesSection.set(path + ".boss-bar", boss.isBossBar());
            bossesSection.set(path + ".action-bar", boss.isActionBar());
            bossesSection.set(path + ".loot-table", boss.getLootTable());
            bossesSection.set(
                    path + ".potion-effects",
                    boss.getPotionEffects()
            );
            bossesSection.set(
                    path + ".custom-name-visible",
                    boss.isCustomNameVisible()
            );
            bossesSection.set(path + ".persistent", boss.isPersistent());
            bossesSection.set(
                    path + ".remove-when-far-away",
                    boss.isRemoveWhenFarAway()
            );
            bossesSection.set(path + ".glowing", boss.isGlowing());
            bossesSection.set(path + ".invulnerable", boss.isInvulnerable());
            bossesSection.set(path + ".silent", boss.isSilent());
            bossesSection.set(path + ".ai", boss.hasAi());
            bossesSection.set(
                    path + ".can-pickup-items",
                    boss.canPickupItems()
            );
            bossesSection.set(path + ".equipment.helmet", boss.getHelmet());
            bossesSection.set(
                    path + ".equipment.chestplate",
                    boss.getChestplate()
            );
            bossesSection.set(
                    path + ".equipment.leggings",
                    boss.getLeggings()
            );
            bossesSection.set(path + ".equipment.boots", boss.getBoots());
            bossesSection.set(
                    path + ".equipment.main-hand",
                    boss.getMainHand()
            );
            bossesSection.set(
                    path + ".equipment.off-hand",
                    boss.getOffHand()
            );
            bossesSection.set(
                    path + ".dropped-experience",
                    boss.getDroppedExperience()
            );
        }

        try {
            configuration.save(bossesFile);
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not save bosses.yml",
                    exception
            );
        }
    }

    public void loadBosses() {
        bosses.clear();

        if (!bossesFile.exists()) {
            saveBosses();
            refreshActiveBosses();
            return;
        }

        YamlConfiguration configuration =
                YamlConfiguration.loadConfiguration(bossesFile);

        ConfigurationSection bossesSection =
                configuration.getConfigurationSection("bosses");

        if (bossesSection == null) {
            refreshActiveBosses();
            return;
        }

        for (String id : bossesSection.getKeys(false)) {
            String path = id + ".";

            try {
                String name = bossesSection.getString(
                        path + "name",
                        id
                );

                double health = bossesSection.getDouble(
                        path + "health",
                        20.0
                );

                String entityTypeName = bossesSection.getString(
                        path + "entity-type",
                        EntityType.ZOMBIE.name()
                );

                EntityType entityType = EntityType.valueOf(
                        entityTypeName.toUpperCase(Locale.ROOT)
                );

                boolean bossBar = bossesSection.getBoolean(
                        path + "boss-bar",
                        false
                );

                boolean actionBar = bossesSection.getBoolean(
                        path + "action-bar",
                        false
                );

                List<ItemStack> lootTable = loadItemStacks(
                        bossesSection,
                        path + "loot-table"
                );

                List<PotionEffect> potionEffects = loadPotionEffects(
                        bossesSection,
                        path + "potion-effects"
                );

                CustomBoss boss = new CustomBoss(
                        bossKey,
                        id,
                        name,
                        health,
                        entityType,
                        bossBar,
                        actionBar,
                        lootTable,
                        potionEffects
                );

                boss.setCustomNameVisible(
                        bossesSection.getBoolean(
                                path + "custom-name-visible",
                                true
                        )
                );
                boss.setPersistent(
                        bossesSection.getBoolean(
                                path + "persistent",
                                true
                        )
                );
                boss.setRemoveWhenFarAway(
                        bossesSection.getBoolean(
                                path + "remove-when-far-away",
                                false
                        )
                );
                boss.setGlowing(
                        bossesSection.getBoolean(path + "glowing", false)
                );
                boss.setInvulnerable(
                        bossesSection.getBoolean(
                                path + "invulnerable",
                                false
                        )
                );
                boss.setSilent(
                        bossesSection.getBoolean(path + "silent", false)
                );
                boss.setAi(
                        bossesSection.getBoolean(path + "ai", true)
                );
                boss.setCanPickupItems(
                        bossesSection.getBoolean(
                                path + "can-pickup-items",
                                false
                        )
                );
                boss.setHelmet(
                        bossesSection.getItemStack(path + "equipment.helmet")
                );
                boss.setChestplate(
                        bossesSection.getItemStack(
                                path + "equipment.chestplate"
                        )
                );
                boss.setLeggings(
                        bossesSection.getItemStack(
                                path + "equipment.leggings"
                        )
                );
                boss.setBoots(
                        bossesSection.getItemStack(path + "equipment.boots")
                );
                boss.setMainHand(
                        bossesSection.getItemStack(
                                path + "equipment.main-hand"
                        )
                );
                boss.setOffHand(
                        bossesSection.getItemStack(
                                path + "equipment.off-hand"
                        )
                );
                boss.setDroppedExperience(
                        bossesSection.getInt(
                                path + "dropped-experience",
                                -1
                        )
                );

                bosses.put(boss.getId(), boss);
            } catch (Exception exception) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "Could not load boss '" + id + "'",
                        exception
                );
            }
        }

        plugin.getLogger().info(
                "Loaded " + bosses.size() + " custom bosses."
        );

        refreshActiveBosses();
    }

    public void shutdown() {
        if (displayTask != null) {
            displayTask.cancel();
            displayTask = null;
        }

        for (ActiveBoss activeBoss : activeBosses.values()) {
            activeBoss.clearBossBar();
        }

        activeBosses.clear();
    }

    private List<ItemStack> loadItemStacks(
            ConfigurationSection section,
            String path
    ) {
        List<?> rawList = section.getList(path, List.of());
        List<ItemStack> items = new ArrayList<>();

        for (Object object : rawList) {
            if (object instanceof ItemStack itemStack) {
                items.add(itemStack.clone());
            }
        }

        return items;
    }

    private List<PotionEffect> loadPotionEffects(
            ConfigurationSection section,
            String path
    ) {
        List<?> rawList = section.getList(path, List.of());
        List<PotionEffect> effects = new ArrayList<>();

        for (Object object : rawList) {
            if (object instanceof PotionEffect potionEffect) {
                effects.add(potionEffect);
            }
        }

        return effects;
    }

    private String normalizeId(String id) {
        return Objects.requireNonNull(id, "Boss ID cannot be null")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void trackLoadedBossEntities() {
        int trackedBosses = 0;

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                String bossId = getEntityBossId(entity);

                if (bossId == null) {
                    continue;
                }

                CustomBoss boss = bosses.get(normalizeId(bossId));

                if (boss == null) {
                    continue;
                }

                trackActiveBoss(entity, boss);
                trackedBosses++;
            }
        }

        if (trackedBosses > 0) {
            plugin.getLogger().info(
                    "Tracking " + trackedBosses + " active custom bosses."
            );
        }
    }

    private void startDisplayTask() {
        if (displayTask != null) {
            displayTask.cancel();
        }

        displayTask = plugin.getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        this::updateActiveBossDisplays,
                        0L,
                        10L
                );
    }

    private void trackActiveBoss(LivingEntity entity, CustomBoss boss) {
        activeBosses.put(
                entity.getUniqueId(),
                new ActiveBoss(entity, boss)
        );
    }

    private void trackActiveBossIfConfigured(LivingEntity entity) {
        String bossId = getEntityBossId(entity);

        if (bossId == null) {
            return;
        }

        CustomBoss boss = bosses.get(normalizeId(bossId));

        if (boss != null) {
            BaseBoss registeredBoss = registeredBosses.get(boss.getId());

            if (registeredBoss != null) {
                runBossHook(
                        registeredBoss,
                        () -> registeredBoss.applyToEntity(entity)
                );
            }

            trackActiveBoss(entity, boss);
        }
    }

    private void refreshActiveBosses() {
        Iterator<Map.Entry<UUID, ActiveBoss>> iterator =
                activeBosses.entrySet().iterator();

        while (iterator.hasNext()) {
            ActiveBoss activeBoss = iterator.next().getValue();
            String bossId = getEntityBossId(activeBoss.getEntity());

            CustomBoss boss = bossId == null
                    ? null
                    : bosses.get(normalizeId(bossId));

            if (boss == null) {
                activeBoss.clearBossBar();
                iterator.remove();
                continue;
            }

            activeBoss.setBoss(boss);
        }
    }

    private void removeActiveBosses(String bossId) {
        Iterator<Map.Entry<UUID, ActiveBoss>> iterator =
                activeBosses.entrySet().iterator();

        while (iterator.hasNext()) {
            ActiveBoss activeBoss = iterator.next().getValue();

            if (!activeBoss.getBoss().getId().equalsIgnoreCase(bossId)) {
                continue;
            }

            activeBoss.clearBossBar();
            iterator.remove();
        }
    }

    private void removeActiveBoss(UUID entityId) {
        ActiveBoss activeBoss = activeBosses.remove(entityId);

        if (activeBoss != null) {
            activeBoss.clearBossBar();
        }
    }

    private void updateActiveBossDisplays() {
        Iterator<Map.Entry<UUID, ActiveBoss>> iterator =
                activeBosses.entrySet().iterator();

        while (iterator.hasNext()) {
            ActiveBoss activeBoss = iterator.next().getValue();
            LivingEntity entity = activeBoss.getEntity();

            if (!entity.isValid() || entity.isDead()) {
                activeBoss.clearBossBar();
                iterator.remove();
                continue;
            }

            String bossId = getEntityBossId(entity);
            CustomBoss boss = bossId == null
                    ? null
                    : bosses.get(normalizeId(bossId));

            if (boss == null) {
                activeBoss.clearBossBar();
                iterator.remove();
                continue;
            }

            activeBoss.setBoss(boss);

            if (boss.isBossBar()) {
                updateBossBar(activeBoss);
            } else {
                activeBoss.clearBossBar();
            }

            if (boss.isActionBar()) {
                sendActionBar(entity, boss);
            }

            BaseBoss registeredBoss = registeredBosses.get(boss.getId());

            if (registeredBoss != null) {
                runBossHook(
                        registeredBoss,
                        () -> registeredBoss.onTick(entity)
                );
            }
        }
    }

    private void updateBossBar(ActiveBoss activeBoss) {
        LivingEntity entity = activeBoss.getEntity();
        CustomBoss boss = activeBoss.getBoss();
        BossBar bossBar = activeBoss.getOrCreateBossBar();
        List<Player> nearbyPlayers = getNearbyPlayers(entity);

        bossBar.setTitle(buildHealthMessage(entity, boss));
        bossBar.setProgress(getHealthProgress(entity, boss));
        bossBar.setVisible(true);

        for (Player player : new ArrayList<>(bossBar.getPlayers())) {
            if (!nearbyPlayers.contains(player)) {
                bossBar.removePlayer(player);
            }
        }

        for (Player player : nearbyPlayers) {
            bossBar.addPlayer(player);
        }
    }

    private void sendActionBar(LivingEntity entity, CustomBoss boss) {
        String message = buildHealthMessage(entity, boss);

        for (Player player : getNearbyPlayers(entity)) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(message)
            );
        }
    }

    private List<Player> getNearbyPlayers(LivingEntity entity) {
        World world = entity.getWorld();
        List<Player> nearbyPlayers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }

            if (player.getLocation().distanceSquared(entity.getLocation())
                    <= DISPLAY_RADIUS_SQUARED) {
                nearbyPlayers.add(player);
            }
        }

        return nearbyPlayers;
    }

    private String buildHealthMessage(
            LivingEntity entity,
            CustomBoss boss
    ) {
        return boss.getName()
                + ChatColor.DARK_GRAY
                + " | "
                + ChatColor.RED
                + formatHealth(entity.getHealth())
                + ChatColor.GRAY
                + "/"
                + formatHealth(getMaxHealth(entity, boss))
                + " HP";
    }

    private double getHealthProgress(
            LivingEntity entity,
            CustomBoss boss
    ) {
        double maxHealth = getMaxHealth(entity, boss);

        if (maxHealth <= 0) {
            return 0.0;
        }

        return Math.clamp(
                entity.getHealth() / maxHealth
                ,
                0.0,
                1.0);
    }

    private double getMaxHealth(
            LivingEntity entity,
            CustomBoss boss
    ) {
        AttributeInstance maxHealth =
                entity.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth == null) {
            return boss.getHealth();
        }

        return maxHealth.getValue();
    }

    private String formatHealth(double health) {
        if (health == Math.floor(health)) {
            return String.valueOf((long) health);
        }

        return String.format(Locale.US, "%.1f", health);
    }

    private String getEntityBossId(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(
                bossKey,
                PersistentDataType.STRING
        );
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                trackActiveBossIfConfigured(livingEntity);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        String bossId = getEntityBossId(entity);

        if (bossId == null) {
            return;
        }

        CustomBoss boss = bosses.get(normalizeId(bossId));

        if (boss == null) {
            return;
        }

        event.getDrops().clear();

        for (ItemStack item : boss.getLootTable()) {
            event.getDrops().add(item.clone());
        }

        if (boss.hasDroppedExperienceOverride()) {
            event.setDroppedExp(boss.getDroppedExperience());
        }

        BaseBoss registeredBoss = registeredBosses.get(boss.getId());

        if (registeredBoss != null) {
            runBossHook(
                    registeredBoss,
                    () -> registeredBoss.onDeath(event)
            );
        }

        removeActiveBoss(entity.getUniqueId());
    }

    private void runBossHook(BaseBoss boss, Runnable hook) {
        try {
            hook.run();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Custom boss hook failed for " + boss.getRegistryId(),
                    exception
            );
        }
    }

    private static final class ActiveBoss {

        private final LivingEntity entity;

        private CustomBoss boss;
        private BossBar bossBar;

        private ActiveBoss(LivingEntity entity, CustomBoss boss) {
            this.entity = entity;
            this.boss = boss;
        }

        private LivingEntity getEntity() {
            return entity;
        }

        private CustomBoss getBoss() {
            return boss;
        }

        private void setBoss(CustomBoss boss) {
            this.boss = boss;
        }

        private BossBar getOrCreateBossBar() {
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(
                        "",
                        BarColor.RED,
                        BarStyle.SOLID
                );
            }

            return bossBar;
        }

        private void clearBossBar() {
            if (bossBar == null) {
                return;
            }

            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }
}
