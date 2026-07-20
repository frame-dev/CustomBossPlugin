package ch.framedev.customBossPluginMaven;

import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class BaseBoss {

    private final JavaPlugin plugin;
    private final String id;
    private final NamespacedKey key;
    private String displayName;
    private double health;
    private EntityType entityType;
    private boolean bossBar;
    private boolean actionBar;
    private List<ItemStack> lootTable;
    private List<PotionEffect> potionEffects;
    private boolean customNameVisible = true;
    private boolean persistent = true;
    private boolean removeWhenFarAway;
    private boolean glowing;
    private boolean invulnerable;
    private boolean silent;
    private boolean ai = true;
    private boolean canPickupItems;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;
    private int droppedExperience = -1;

    public BaseBoss(
            JavaPlugin plugin,
            String displayName,
            String id
    ) {
        this(
                plugin,
                displayName,
                id,
                20.0,
                EntityType.ZOMBIE
        );
    }

    public BaseBoss(
            JavaPlugin plugin,
            String displayName,
            String id,
            double health,
            EntityType entityType
    ) {
        this(
                plugin,
                displayName,
                id,
                health,
                entityType,
                false,
                false,
                List.of(),
                List.of()
        );
    }

    public BaseBoss(
            JavaPlugin plugin,
            String displayName,
            String id,
            double health,
            EntityType entityType,
            boolean bossBar,
            boolean actionBar,
            Collection<ItemStack> lootTable,
            Collection<PotionEffect> potionEffects
    ) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.id = validateId(id);
        this.displayName = Objects.requireNonNull(
                displayName,
                "Display name cannot be null"
        );
        this.key = new NamespacedKey(plugin, this.id);

        setHealth(health);
        setEntityType(entityType);
        setBossBar(bossBar);
        setActionBar(actionBar);
        setLootTable(lootTable);
        setPotionEffects(potionEffects);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getDisplayName() {
        return displayName;
    }

    @SuppressWarnings("deprecation")
    public String getColoredDisplayName() {
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }

    public void setDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(
                displayName,
                "Display name cannot be null"
        );
    }

    public String getName() {
        return getDisplayName();
    }

    public void setName(String name) {
        setDisplayName(name);
    }

    public String getId() {
        return id;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public String getRegistryId() {
        return key.getNamespace() + ":" + key.getKey();
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        if (!Double.isFinite(health) || health <= 0) {
            throw new IllegalArgumentException(
                    "Health must be a finite number greater than zero"
            );
        }

        this.health = health;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = validateEntityType(entityType);
    }

    public boolean isBossBar() {
        return bossBar;
    }

    public void setBossBar(boolean bossBar) {
        this.bossBar = bossBar;
    }

    public boolean isActionBar() {
        return actionBar;
    }

    public void setActionBar(boolean actionBar) {
        this.actionBar = actionBar;
    }

    public void addLoot(ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "ItemStack cannot be null");

        if (itemStack.getType().isAir()) {
            throw new IllegalArgumentException(
                    "Cannot add air to the loot table"
            );
        }

        lootTable.add(itemStack.clone());
    }

    public boolean removeLoot(int index) {
        if (index < 0 || index >= lootTable.size()) {
            return false;
        }

        lootTable.remove(index);
        return true;
    }

    public void clearLootTable() {
        lootTable.clear();
    }

    public List<ItemStack> getLootTable() {
        return lootTable.stream()
                .map(ItemStack::clone)
                .toList();
    }

    public void setLootTable(Collection<ItemStack> lootTable) {
        this.lootTable = new ArrayList<>();

        if (lootTable == null) {
            return;
        }

        for (ItemStack item : lootTable) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            this.lootTable.add(item.clone());
        }
    }

    public void addPotionEffect(PotionEffect potionEffect) {
        Objects.requireNonNull(potionEffect, "Potion effect cannot be null");
        potionEffects.add(potionEffect);
    }

    public boolean removePotionEffect(PotionEffectType type) {
        Objects.requireNonNull(type, "Potion effect type cannot be null");

        return potionEffects.removeIf(
                potionEffect -> potionEffect.getType().equals(type)
        );
    }

    public void clearPotionEffects() {
        potionEffects.clear();
    }

    public List<PotionEffect> getPotionEffects() {
        return List.copyOf(potionEffects);
    }

    public void setPotionEffects(Collection<PotionEffect> potionEffects) {
        this.potionEffects = new ArrayList<>();

        if (potionEffects == null) {
            return;
        }

        for (PotionEffect potionEffect : potionEffects) {
            if (potionEffect != null) {
                this.potionEffects.add(potionEffect);
            }
        }
    }

    public boolean isCustomNameVisible() {
        return customNameVisible;
    }

    public void setCustomNameVisible(boolean customNameVisible) {
        this.customNameVisible = customNameVisible;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public boolean isRemoveWhenFarAway() {
        return removeWhenFarAway;
    }

    public void setRemoveWhenFarAway(boolean removeWhenFarAway) {
        this.removeWhenFarAway = removeWhenFarAway;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean hasAi() {
        return ai;
    }

    public void setAi(boolean ai) {
        this.ai = ai;
    }

    public boolean canPickupItems() {
        return canPickupItems;
    }

    public void setCanPickupItems(boolean canPickupItems) {
        this.canPickupItems = canPickupItems;
    }

    public ItemStack getHelmet() {
        return cloneItem(helmet);
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = cloneItem(helmet);
    }

    public ItemStack getChestplate() {
        return cloneItem(chestplate);
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = cloneItem(chestplate);
    }

    public ItemStack getLeggings() {
        return cloneItem(leggings);
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = cloneItem(leggings);
    }

    public ItemStack getBoots() {
        return cloneItem(boots);
    }

    public void setBoots(ItemStack boots) {
        this.boots = cloneItem(boots);
    }

    public ItemStack getMainHand() {
        return cloneItem(mainHand);
    }

    public void setMainHand(ItemStack mainHand) {
        this.mainHand = cloneItem(mainHand);
    }

    public ItemStack getOffHand() {
        return cloneItem(offHand);
    }

    public void setOffHand(ItemStack offHand) {
        this.offHand = cloneItem(offHand);
    }

    public void setEquipment(
            ItemStack helmet,
            ItemStack chestplate,
            ItemStack leggings,
            ItemStack boots,
            ItemStack mainHand,
            ItemStack offHand
    ) {
        setHelmet(helmet);
        setChestplate(chestplate);
        setLeggings(leggings);
        setBoots(boots);
        setMainHand(mainHand);
        setOffHand(offHand);
    }

    public boolean hasDroppedExperienceOverride() {
        return droppedExperience >= 0;
    }

    public int getDroppedExperience() {
        return droppedExperience;
    }

    public void setDroppedExperience(int droppedExperience) {
        if (droppedExperience < -1) {
            throw new IllegalArgumentException(
                    "Dropped experience must be zero or greater, or -1"
            );
        }

        this.droppedExperience = droppedExperience;
    }

    public void applyToEntity(LivingEntity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");

        if (!displayName.isBlank()) {
            entity.setCustomName(getColoredDisplayName());
        }

        entity.setCustomNameVisible(customNameVisible);
        entity.setPersistent(persistent);
        entity.setRemoveWhenFarAway(removeWhenFarAway);
        entity.setGlowing(glowing);
        entity.setInvulnerable(invulnerable);
        entity.setSilent(silent);
        entity.setAI(ai);
        entity.setCanPickupItems(canPickupItems);

        EntityEquipment equipment = entity.getEquipment();

        if (equipment == null) {
            return;
        }

        if (helmet != null) {
            equipment.setHelmet(helmet.clone());
        }

        if (chestplate != null) {
            equipment.setChestplate(chestplate.clone());
        }

        if (leggings != null) {
            equipment.setLeggings(leggings.clone());
        }

        if (boots != null) {
            equipment.setBoots(boots.clone());
        }

        if (mainHand != null) {
            equipment.setItemInMainHand(mainHand.clone());
        }

        if (offHand != null) {
            equipment.setItemInOffHand(offHand.clone());
        }
    }

    public void onSpawn(LivingEntity entity) {
    }

    public void onTick(LivingEntity entity) {
    }

    public void onDeath(EntityDeathEvent event) {
    }

    private static String validateId(String id) {
        Objects.requireNonNull(id, "Boss ID cannot be null");

        String normalized = id.trim().toLowerCase(Locale.ROOT);

        if (!normalized.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Boss ID may only contain lowercase letters, "
                            + "numbers, '_' and '-'"
            );
        }

        return normalized;
    }

    private static EntityType validateEntityType(EntityType entityType) {
        Objects.requireNonNull(entityType, "Entity type cannot be null");

        if (!entityType.isAlive()) {
            throw new IllegalArgumentException(
                    entityType + " is not a living entity type"
            );
        }

        return entityType;
    }

    private static ItemStack cloneItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }

        return itemStack.clone();
    }
}
