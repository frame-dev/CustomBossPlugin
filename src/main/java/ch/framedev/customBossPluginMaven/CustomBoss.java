package ch.framedev.customBossPluginMaven;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CustomBoss {

    private final NamespacedKey bossKey;

    private final String id;
    private String name;
    private double health;
    private EntityType entityType;

    private boolean bossBar;
    private boolean actionBar;

    private List<ItemStack> lootTable;
    private List<PotionEffect> potionEffects;

    public CustomBoss(
            NamespacedKey bossKey,
            String id,
            String name,
            double health,
            EntityType entityType,
            boolean bossBar,
            boolean actionBar,
            List<ItemStack> lootTable,
            List<PotionEffect> potionEffects
    ) {
        this.bossKey = Objects.requireNonNull(bossKey, "Boss key cannot be null");
        this.id = validateId(id);
        this.name = Objects.requireNonNull(name, "Boss name cannot be null");
        this.entityType = validateEntityType(entityType);

        setHealth(health);
        setLootTable(lootTable);
        setPotionEffects(potionEffects);

        this.bossBar = bossBar;
        this.actionBar = actionBar;
    }

    public LivingEntity spawnBoss(Location location) {
        Objects.requireNonNull(location, "Location cannot be null");

        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        Entity entity = world.spawnEntity(location, entityType);

        if (!(entity instanceof LivingEntity livingEntity)) {
            entity.remove();
            return null;
        }

        AttributeInstance maxHealth =
                livingEntity.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth == null) {
            livingEntity.remove();

            throw new IllegalStateException(
                    entityType + " does not have the MAX_HEALTH attribute"
            );
        }

        try {
            maxHealth.setBaseValue(health);
            livingEntity.setHealth(health);

            livingEntity.getPersistentDataContainer().set(
                    bossKey,
                    PersistentDataType.STRING,
                    id
            );

            if (!name.isBlank()) {
                livingEntity.setCustomName(name);
                livingEntity.setCustomNameVisible(true);
            }

            if (!potionEffects.isEmpty()) {
                livingEntity.addPotionEffects(potionEffects);
            }

            livingEntity.setRemoveWhenFarAway(false);
            livingEntity.setPersistent(true);

            return livingEntity;
        } catch (RuntimeException exception) {
            livingEntity.remove();
            throw exception;
        }
    }

    public void addLoot(ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "ItemStack cannot be null");

        if (itemStack.getType().isAir()) {
            throw new IllegalArgumentException("Cannot add air to the loot table");
        }

        lootTable.add(itemStack.clone());
    }

    public void clearLootTable() {
        lootTable.clear();
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

    public boolean removeLoot(int index) {
        if (index < 0 || index >= lootTable.size()) {
            return false;
        }

        lootTable.remove(index);
        return true;
    }

    private static String validateId(String id) {
        Objects.requireNonNull(id, "Boss ID cannot be null");

        String normalized = id.trim().toLowerCase();

        if (!normalized.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Boss ID may only contain lowercase letters, numbers, '_' and '-'"
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Boss name cannot be null");
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
}