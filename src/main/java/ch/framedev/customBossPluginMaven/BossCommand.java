package ch.framedev.customBossPluginMaven;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BossCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX =
            ChatColor.DARK_GRAY + "["
                    + ChatColor.DARK_RED + "Boss"
                    + ChatColor.DARK_GRAY + "] "
                    + ChatColor.RESET;

    private final BossManager bossManager;

    public BossCommand(BossManager bossManager) {
        this.bossManager = Objects.requireNonNull(
                bossManager,
                "BossManager cannot be null"
        );
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("customboss.admin")) {
            sendError(sender, "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> handleCreate(sender, args);
                case "edit" -> handleEdit(sender, label, args);
                case "enable" -> handleFeatureToggle(sender, args, true);
                case "disable" -> handleFeatureToggle(sender, args, false);
                case "spawn" -> handleSpawn(sender, args);
                case "spawnat" -> handleSpawnAt(sender, args);
                case "addloot" -> handleLegacyAddLoot(sender, args);
                case "clearloot" -> handleLegacyClearLoot(sender, args);
                case "delete" -> handleDelete(sender, args);
                case "list" -> handleList(sender);
                case "info" -> handleInfo(sender, args);
                case "save" -> handleSave(sender);
                case "reload" -> handleReload(sender);
                default -> sendHelp(sender, label);
            }
        } catch (IllegalArgumentException exception) {
            sendError(sender, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(
                    sender,
                    "An error occurred: "
                            + exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }

        return true;
    }

    private void handleCreate(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 5) {
            sendError(
                    sender,
                    "Usage: /boss create <id> "
                            + "<entity-type> <health> <display-name>"
            );
            return;
        }

        String id = args[1];
        EntityType entityType = parseLivingEntityType(args[2]);
        double health = parsePositiveDouble(args[3], "health");

        String name = colorize(
                String.join(
                        " ",
                        Arrays.copyOfRange(args, 4, args.length)
                )
        );

        CustomBoss boss = bossManager.createBoss(
                id,
                name,
                health,
                entityType
        );

        sendSuccess(
                sender,
                "Created boss "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void handleEdit(
            CommandSender sender,
            String label,
            String[] args
    ) {
        if (args.length < 3) {
            sendEditHelp(sender, label);
            return;
        }

        CustomBoss boss = getRequiredBoss(args[1]);
        String editType = args[2].toLowerCase(Locale.ROOT);

        switch (editType) {
            case "name" -> editName(sender, boss, args);
            case "health" -> editHealth(sender, boss, args);
            case "type", "entity", "entitytype" ->
                    editEntityType(sender, boss, args);
            case "bossbar" -> editBossBar(sender, boss, args);
            case "actionbar" -> editActionBar(sender, boss, args);
            case "potion", "potioneffect", "effects" ->
                    editPotionEffects(sender, label, boss, args);
            case "loot", "loottable" ->
                    editLoot(sender, label, boss, args);
            default -> sendEditHelp(sender, label);
        }
    }

    private void editName(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length < 4) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> name <new name>"
            );
            return;
        }

        String newName = colorize(
                String.join(
                        " ",
                        Arrays.copyOfRange(args, 3, args.length)
                )
        );

        boss.setName(newName);
        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Changed the name of "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + " to "
                        + ChatColor.RESET
                        + newName
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void editHealth(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length != 4) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> health <amount>"
            );
            return;
        }

        double health = parsePositiveDouble(args[3], "health");

        boss.setHealth(health);
        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Changed the health of "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + " to "
                        + ChatColor.YELLOW
                        + health
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void editEntityType(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length != 4) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> type <entity-type>"
            );
            return;
        }

        EntityType entityType = parseLivingEntityType(args[3]);

        boss.setEntityType(entityType);
        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Changed the entity type of "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + " to "
                        + ChatColor.YELLOW
                        + entityType.name()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void editBossBar(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length != 4) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> bossbar <true|false>"
            );
            return;
        }

        boolean enabled = parseBoolean(args[3]);

        setFeatureEnabled(sender, boss, BossFeature.BOSS_BAR, enabled);
    }

    private void editActionBar(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length != 4) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> actionbar <true|false>"
            );
            return;
        }

        boolean enabled = parseBoolean(args[3]);

        setFeatureEnabled(sender, boss, BossFeature.ACTION_BAR, enabled);
    }

    private void handleFeatureToggle(
            CommandSender sender,
            String[] args,
            boolean enabled
    ) {
        if (args.length != 3) {
            String action = enabled
                    ? "enable"
                    : "disable";

            sendError(
                    sender,
                    "Usage: /boss "
                            + action
                            + " <id> <bossbar|actionbar>"
            );
            return;
        }

        CustomBoss boss = getRequiredBoss(args[1]);
        BossFeature feature = parseBossFeature(args[2]);

        setFeatureEnabled(sender, boss, feature, enabled);
    }

    private void setFeatureEnabled(
            CommandSender sender,
            CustomBoss boss,
            BossFeature feature,
            boolean enabled
    ) {
        switch (feature) {
            case BOSS_BAR -> boss.setBossBar(enabled);
            case ACTION_BAR -> boss.setActionBar(enabled);
        }

        bossManager.saveBosses();

        sendSuccess(
                sender,
                formatBossFeature(feature)
                        + " for "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + " has been "
                        + ChatColor.YELLOW
                        + (enabled ? "enabled" : "disabled")
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void editPotionEffects(
            CommandSender sender,
            String label,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length < 4) {
            sendPotionHelp(sender, label);
            return;
        }

        String action = args[3].toLowerCase(Locale.ROOT);

        switch (action) {
            case "add" -> addPotionEffect(sender, boss, args);
            case "remove" -> removePotionEffect(sender, boss, args);
            case "clear" -> clearPotionEffects(sender, boss);
            case "list" -> listPotionEffects(sender, boss);
            default -> sendPotionHelp(sender, label);
        }
    }

    private void addPotionEffect(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length < 7) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> potion add "
                            + "<type> <seconds|-1> <level> "
                            + "[ambient] [particles] [icon]"
            );
            return;
        }

        PotionEffectType type = parsePotionEffectType(args[4]);

        int seconds = parseInteger(args[5], "duration");
        int level = parseInteger(args[6], "level");

        if (seconds == 0 || seconds < -1) {
            throw new IllegalArgumentException(
                    "Potion duration must be greater than zero or -1."
            );
        }

        if (level <= 0) {
            throw new IllegalArgumentException(
                    "Potion level must be greater than zero."
            );
        }

        boolean ambient = args.length >= 8
                && parseBoolean(args[7]);

        boolean particles = args.length < 9
                || parseBoolean(args[8]);

        boolean icon = args.length < 10
                || parseBoolean(args[9]);

        int durationTicks;

        if (seconds == -1) {
            durationTicks = Integer.MAX_VALUE;
        } else {
            try {
                durationTicks = Math.multiplyExact(seconds, 20);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "The potion duration is too large."
                );
            }
        }

        int amplifier = level - 1;

        boss.removePotionEffect(type);

        boss.addPotionEffect(
                new PotionEffect(
                        type,
                        durationTicks,
                        amplifier,
                        ambient,
                        particles,
                        icon
                )
        );

        bossManager.saveBosses();

        String durationText = seconds == -1
                ? "infinite"
                : seconds + " seconds";

        sendSuccess(
                sender,
                "Added "
                        + ChatColor.YELLOW
                        + formatPotionName(type)
                        + " "
                        + level
                        + ChatColor.GREEN
                        + " for "
                        + ChatColor.YELLOW
                        + durationText
                        + ChatColor.GREEN
                        + " to "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void removePotionEffect(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length != 5) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> potion remove <type>"
            );
            return;
        }

        PotionEffectType type = parsePotionEffectType(args[4]);

        if (!boss.removePotionEffect(type)) {
            sendError(
                    sender,
                    "Boss '"
                            + boss.getId()
                            + "' does not have the effect "
                            + formatPotionName(type)
                            + "."
            );
            return;
        }

        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Removed "
                        + ChatColor.YELLOW
                        + formatPotionName(type)
                        + ChatColor.GREEN
                        + " from "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void clearPotionEffects(
            CommandSender sender,
            CustomBoss boss
    ) {
        int effectCount = boss.getPotionEffects().size();

        boss.clearPotionEffects();
        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Removed "
                        + ChatColor.YELLOW
                        + effectCount
                        + ChatColor.GREEN
                        + " potion effect(s) from "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void listPotionEffects(
            CommandSender sender,
            CustomBoss boss
    ) {
        List<PotionEffect> effects = boss.getPotionEffects();

        if (effects.isEmpty()) {
            sender.sendMessage(
                    PREFIX
                            + ChatColor.YELLOW
                            + "Boss '"
                            + boss.getId()
                            + "' has no potion effects."
            );
            return;
        }

        sender.sendMessage(
                ChatColor.DARK_RED
                        + "Potion effects for "
                        + boss.getId()
                        + ":"
        );

        for (PotionEffect effect : effects) {
            int level = effect.getAmplifier() + 1;

            String duration;

            if (effect.getDuration() >= Integer.MAX_VALUE / 2) {
                duration = "infinite";
            } else {
                duration = formatSeconds(effect.getDuration());
            }

            sender.sendMessage(
                    ChatColor.DARK_GRAY
                            + "- "
                            + ChatColor.YELLOW
                            + formatPotionName(effect.getType())
                            + " "
                            + level
                            + ChatColor.GRAY
                            + " | Duration: "
                            + duration
                            + " | Ambient: "
                            + effect.isAmbient()
                            + " | Particles: "
                            + effect.hasParticles()
                            + " | Icon: "
                            + effect.hasIcon()
            );
        }
    }

    private void editLoot(
            CommandSender sender,
            String label,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length < 4) {
            sendLootHelp(sender, label);
            return;
        }

        String action = args[3].toLowerCase(Locale.ROOT);

        switch (action) {
            case "addhand" -> addLootFromHand(sender, boss);
            case "add" -> addLootFromMaterial(sender, boss, args);
            case "remove" -> removeLoot(sender, boss, args);
            case "clear" -> clearLoot(sender, boss);
            case "list" -> listLoot(sender, boss);
            default -> sendLootHelp(sender, label);
        }
    }

    private void addLootFromHand(
            CommandSender sender,
            CustomBoss boss
    ) {
        if (!(sender instanceof Player player)) {
            sendError(
                    sender,
                    "Only players can add an item from their hand. "
                            + "Use '/boss edit <id> loot add "
                            + "<material> [amount]' from command blocks."
            );
            return;
        }

        ItemStack heldItem =
                player.getInventory().getItemInMainHand();

        if (heldItem.getType().isAir()) {
            sendError(
                    sender,
                    "You must hold an item in your main hand."
            );
            return;
        }

        boss.addLoot(heldItem.clone());
        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Added "
                        + ChatColor.YELLOW
                        + heldItem.getAmount()
                        + "x "
                        + formatEnumName(heldItem.getType().name())
                        + ChatColor.GREEN
                        + " to "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void addLootFromMaterial(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length < 5 || args.length > 6) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> loot add "
                            + "<material> [amount]"
            );
            return;
        }

        Material material = Material.matchMaterial(args[4]);

        if (material == null || material.isAir()) {
            throw new IllegalArgumentException(
                    "Unknown or invalid material: " + args[4]
            );
        }

        if (!material.isItem()) {
            throw new IllegalArgumentException(
                    material.name() + " cannot exist as an item."
            );
        }

        int amount = args.length == 6
                ? parseInteger(args[5], "amount")
                : 1;

        if (amount <= 0 || amount > material.getMaxStackSize()) {
            throw new IllegalArgumentException(
                    "Amount must be between 1 and "
                            + material.getMaxStackSize()
                            + "."
            );
        }

        ItemStack itemStack = new ItemStack(material, amount);

        boss.addLoot(itemStack);
        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Added "
                        + ChatColor.YELLOW
                        + amount
                        + "x "
                        + formatEnumName(material.name())
                        + ChatColor.GREEN
                        + " to "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void removeLoot(
            CommandSender sender,
            CustomBoss boss,
            String[] args
    ) {
        if (args.length != 5) {
            sendError(
                    sender,
                    "Usage: /boss edit <id> loot remove <index>"
            );
            return;
        }

        int displayedIndex = parseInteger(args[4], "loot index");

        if (displayedIndex <= 0) {
            throw new IllegalArgumentException(
                    "Loot index must be greater than zero."
            );
        }

        int internalIndex = displayedIndex - 1;

        if (!boss.removeLoot(internalIndex)) {
            sendError(
                    sender,
                    "Loot entry " + displayedIndex + " does not exist."
            );
            return;
        }

        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Removed loot entry "
                        + ChatColor.YELLOW
                        + displayedIndex
                        + ChatColor.GREEN
                        + " from "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void clearLoot(
            CommandSender sender,
            CustomBoss boss
    ) {
        int lootCount = boss.getLootTable().size();

        boss.clearLootTable();
        bossManager.saveBosses();

        sendSuccess(
                sender,
                "Removed "
                        + ChatColor.YELLOW
                        + lootCount
                        + ChatColor.GREEN
                        + " loot entries from "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void listLoot(
            CommandSender sender,
            CustomBoss boss
    ) {
        List<ItemStack> lootTable = boss.getLootTable();

        if (lootTable.isEmpty()) {
            sender.sendMessage(
                    PREFIX
                            + ChatColor.YELLOW
                            + "Boss '"
                            + boss.getId()
                            + "' has no loot."
            );
            return;
        }

        sender.sendMessage(
                ChatColor.DARK_RED
                        + "Loot table for "
                        + boss.getId()
                        + ":"
        );

        for (int index = 0; index < lootTable.size(); index++) {
            ItemStack item = lootTable.get(index);

            sender.sendMessage(
                    ChatColor.DARK_GRAY
                            + String.valueOf(index + 1)
                            + ". "
                            + ChatColor.YELLOW
                            + item.getAmount()
                            + "x "
                            + formatEnumName(item.getType().name())
            );
        }
    }

    private void handleSpawn(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendError(sender, "Usage: /boss spawn <id>");
            return;
        }

        CustomBoss boss = getRequiredBoss(args[1]);
        Location spawnLocation = getSenderLocation(sender);

        if (spawnLocation == null) {
            sendError(
                    sender,
                    "This sender has no location. Use "
                            + "/boss spawnat <id> <world> <x> <y> <z>."
            );
            return;
        }

        LivingEntity entity = bossManager.spawnBoss(
                boss.getId(),
                spawnLocation
        );

        if (entity == null) {
            sendError(sender, "The boss could not be spawned.");
            return;
        }

        sendSuccess(
                sender,
                "Spawned "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + " at "
                        + ChatColor.YELLOW
                        + formatLocation(spawnLocation)
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void handleSpawnAt(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 6) {
            sendError(
                    sender,
                    "Usage: /boss spawnat "
                            + "<id> <world> <x> <y> <z>"
            );
            return;
        }

        CustomBoss boss = getRequiredBoss(args[1]);

        World world = Bukkit.getWorld(args[2]);

        if (world == null) {
            sendError(
                    sender,
                    "World '" + args[2] + "' does not exist."
            );
            return;
        }

        Location senderLocation = getSenderLocation(sender);

        double x = parseCoordinate(
                args[3],
                senderLocation,
                Axis.X
        );

        double y = parseCoordinate(
                args[4],
                senderLocation,
                Axis.Y
        );

        double z = parseCoordinate(
                args[5],
                senderLocation,
                Axis.Z
        );

        Location spawnLocation = new Location(world, x, y, z);

        LivingEntity entity = bossManager.spawnBoss(
                boss.getId(),
                spawnLocation
        );

        if (entity == null) {
            sendError(sender, "The boss could not be spawned.");
            return;
        }

        sendSuccess(
                sender,
                "Spawned "
                        + ChatColor.YELLOW
                        + boss.getId()
                        + ChatColor.GREEN
                        + " at "
                        + ChatColor.YELLOW
                        + formatLocation(spawnLocation)
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void handleLegacyAddLoot(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendError(sender, "Usage: /boss addloot <id>");
            return;
        }

        CustomBoss boss = getRequiredBoss(args[1]);
        addLootFromHand(sender, boss);
    }

    private void handleLegacyClearLoot(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendError(sender, "Usage: /boss clearloot <id>");
            return;
        }

        CustomBoss boss = getRequiredBoss(args[1]);
        clearLoot(sender, boss);
    }

    private void handleDelete(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendError(sender, "Usage: /boss delete <id>");
            return;
        }

        if (!bossManager.deleteBoss(args[1])) {
            sendError(
                    sender,
                    "Boss '" + args[1] + "' does not exist."
            );
            return;
        }

        sendSuccess(
                sender,
                "Deleted boss "
                        + ChatColor.YELLOW
                        + args[1]
                        + ChatColor.GREEN
                        + "."
        );
    }

    private void handleList(CommandSender sender) {
        Collection<CustomBoss> bosses = bossManager.getBosses();

        if (bosses.isEmpty()) {
            sender.sendMessage(
                    PREFIX
                            + ChatColor.YELLOW
                            + "No bosses have been created."
            );
            return;
        }

        sender.sendMessage(
                ChatColor.DARK_RED + "Custom bosses:"
        );

        for (CustomBoss boss : bosses) {
            sender.sendMessage(
                    ChatColor.DARK_GRAY
                            + "- "
                            + ChatColor.YELLOW
                            + boss.getId()
                            + ChatColor.GRAY
                            + " | "
                            + boss.getEntityType().name()
                            + " | "
                            + boss.getHealth()
                            + " HP"
            );
        }
    }

    private void handleInfo(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendError(sender, "Usage: /boss info <id>");
            return;
        }

        CustomBoss boss = getRequiredBoss(args[1]);

        sender.sendMessage(
                ChatColor.DARK_RED + "Boss information"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "ID: "
                        + ChatColor.WHITE
                        + boss.getId()
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Name: "
                        + ChatColor.WHITE
                        + boss.getName()
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Type: "
                        + ChatColor.WHITE
                        + boss.getEntityType().name()
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Health: "
                        + ChatColor.WHITE
                        + boss.getHealth()
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Loot entries: "
                        + ChatColor.WHITE
                        + boss.getLootTable().size()
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Potion effects: "
                        + ChatColor.WHITE
                        + boss.getPotionEffects().size()
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Boss bar: "
                        + ChatColor.WHITE
                        + boss.isBossBar()
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Action bar: "
                        + ChatColor.WHITE
                        + boss.isActionBar()
        );
    }

    private void handleSave(CommandSender sender) {
        bossManager.saveBosses();
        sendSuccess(sender, "Bosses have been saved.");
    }

    private void handleReload(CommandSender sender) {
        bossManager.loadBosses();
        sendSuccess(sender, "Bosses have been reloaded.");
    }

    private CustomBoss getRequiredBoss(String id) {
        return bossManager.getBoss(id).orElseThrow(
                () -> new IllegalArgumentException(
                        "Boss '" + id + "' does not exist."
                )
        );
    }

    private EntityType parseLivingEntityType(String input) {
        EntityType entityType;

        try {
            entityType = EntityType.valueOf(
                    input.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown entity type: " + input
            );
        }

        if (!entityType.isAlive()) {
            throw new IllegalArgumentException(
                    entityType.name()
                            + " is not a living entity type."
            );
        }

        return entityType;
    }

    private PotionEffectType parsePotionEffectType(String input) {
        String normalized = input
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_');

        NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);

        if (key == null) {
            throw new IllegalArgumentException(
                    "Invalid potion effect type: " + input
            );
        }

        PotionEffectType type = Registry.EFFECT.get(key);

        if (type == null) {
            throw new IllegalArgumentException(
                    "Unknown potion effect type: " + input
            );
        }

        return type;
    }

    private double parsePositiveDouble(
            String input,
            String fieldName
    ) {
        final double value;

        try {
            value = Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "'" + input
                            + "' is not a valid "
                            + fieldName
                            + " value."
            );
        }

        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be a finite number greater than zero."
            );
        }

        return value;
    }

    private int parseInteger(
            String input,
            String fieldName
    ) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "'" + input
                            + "' is not a valid "
                            + fieldName
                            + " value."
            );
        }
    }

    private boolean parseBoolean(String input) {
        if (input.equalsIgnoreCase("true")
                || input.equalsIgnoreCase("on")
                || input.equalsIgnoreCase("yes")
                || input.equalsIgnoreCase("enable")
                || input.equalsIgnoreCase("enabled")) {
            return true;
        }

        if (input.equalsIgnoreCase("false")
                || input.equalsIgnoreCase("off")
                || input.equalsIgnoreCase("no")
                || input.equalsIgnoreCase("disable")
                || input.equalsIgnoreCase("disabled")) {
            return false;
        }

        throw new IllegalArgumentException(
                "'" + input + "' must be true or false."
        );
    }

    private BossFeature parseBossFeature(String input) {
        String normalized = input
                .toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");

        return switch (normalized) {
            case "bossbar", "bar" -> BossFeature.BOSS_BAR;
            case "actionbar", "action" -> BossFeature.ACTION_BAR;
            default -> throw new IllegalArgumentException(
                    "Unknown boss feature: " + input
            );
        };
    }

    private String formatBossFeature(BossFeature feature) {
        return switch (feature) {
            case BOSS_BAR -> "Boss bar";
            case ACTION_BAR -> "Action bar";
        };
    }

    private Location getSenderLocation(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getLocation();
        }

        if (sender instanceof BlockCommandSender blockSender) {
            return blockSender.getBlock()
                    .getLocation()
                    .add(0.5, 1.0, 0.5);
        }

        return null;
    }

    private double parseCoordinate(
            String input,
            Location baseLocation,
            Axis axis
    ) {
        if (!input.startsWith("~")) {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "'" + input + "' is not a valid coordinate."
                );
            }
        }

        if (baseLocation == null) {
            throw new IllegalArgumentException(
                    "Relative coordinates cannot be used "
                            + "by this command sender."
            );
        }

        double baseValue = switch (axis) {
            case X -> baseLocation.getX();
            case Y -> baseLocation.getY();
            case Z -> baseLocation.getZ();
        };

        String offsetText = input.substring(1);

        if (offsetText.isBlank()) {
            return baseValue;
        }

        try {
            return baseValue + Double.parseDouble(offsetText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "'" + input
                            + "' is not a valid relative coordinate."
            );
        }
    }

    private String formatLocation(Location location) {
        World world = location.getWorld();

        String worldName = world == null
                ? "unknown"
                : world.getName();

        return worldName
                + " "
                + formatCoordinate(location.getX())
                + " "
                + formatCoordinate(location.getY())
                + " "
                + formatCoordinate(location.getZ());
    }

    private String formatCoordinate(double coordinate) {
        return String.format(Locale.US, "%.2f", coordinate);
    }

    private String formatSeconds(int ticks) {
        double seconds = ticks / 20.0;

        if (seconds == Math.floor(seconds)) {
            return (long) seconds + " seconds";
        }

        return String.format(
                Locale.US,
                "%.2f seconds",
                seconds
        );
    }

    private String formatPotionName(PotionEffectType type) {
        return formatEnumName(
                type.getKey().getKey()
        );
    }

    private String formatEnumName(String value) {
        String formatted = value
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ');

        if (formatted.isBlank()) {
            return formatted;
        }

        return Character.toUpperCase(formatted.charAt(0))
                + formatted.substring(1);
    }

    @SuppressWarnings("deprecation")
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void sendSuccess(
            CommandSender sender,
            String message
    ) {
        sender.sendMessage(
                PREFIX + ChatColor.GREEN + message
        );
    }

    private void sendError(
            CommandSender sender,
            String message
    ) {
        sender.sendMessage(
                PREFIX + ChatColor.RED + message
        );
    }

    private void sendHelp(
            CommandSender sender,
            String label
    ) {
        sender.sendMessage(
                ChatColor.DARK_RED + "Custom boss commands"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " create <id> <entity> <health> <name>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> ..."
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " enable <id> <bossbar|actionbar>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " disable <id> <bossbar|actionbar>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " spawn <id>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " spawnat <id> <world> <x> <y> <z>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " delete <id>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " info <id>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " list"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " save"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " reload"
        );
    }

    private void sendEditHelp(
            CommandSender sender,
            String label
    ) {
        sender.sendMessage(
                ChatColor.DARK_RED + "Boss editing commands"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> name <name>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> health <amount>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> type <entity>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> bossbar <true|false>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> actionbar <true|false>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> potion ..."
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> loot ..."
        );
    }

    private void sendPotionHelp(
            CommandSender sender,
            String label
    ) {
        sender.sendMessage(
                ChatColor.DARK_RED + "Potion editing commands"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> potion add "
                        + "<type> <seconds|-1> <level> "
                        + "[ambient] [particles] [icon]"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> potion remove <type>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> potion clear"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> potion list"
        );
    }

    private void sendLootHelp(
            CommandSender sender,
            String label
    ) {
        sender.sendMessage(
                ChatColor.DARK_RED + "Loot editing commands"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> loot addhand"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> loot add <material> [amount]"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> loot remove <index>"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> loot clear"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                        + "/"
                        + label
                        + " edit <id> loot list"
        );
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("customboss.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(
                    List.of(
                            "create",
                            "edit",
                            "enable",
                            "disable",
                            "spawn",
                            "spawnat",
                            "addloot",
                            "clearloot",
                            "delete",
                            "list",
                            "info",
                            "save",
                            "reload"
                    ),
                    args[0]
            );
        }

        if (args.length == 2
                && requiresBossId(args[0])) {
            return filter(
                    getBossIds(),
                    args[1]
            );
        }

        if (args.length == 3
                && (
                args[0].equalsIgnoreCase("enable")
                        || args[0].equalsIgnoreCase("disable")
        )) {
            return filter(
                    getBossFeatureNames(),
                    args[2]
            );
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("create")) {
            return filter(
                    getLivingEntityTypeNames(),
                    args[2]
            );
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("spawnat")) {
            return filter(
                    Bukkit.getWorlds()
                            .stream()
                            .map(World::getName)
                            .toList(),
                    args[2]
            );
        }

        if (args.length >= 4
                && args.length <= 6
                && args[0].equalsIgnoreCase("spawnat")) {
            return filter(
                    List.of("~", "~1", "~-1"),
                    args[args.length - 1]
            );
        }

        if (!args[0].equalsIgnoreCase("edit")) {
            return List.of();
        }

        if (args.length == 3) {
            return filter(
                    List.of(
                            "name",
                            "health",
                            "type",
                            "bossbar",
                            "actionbar",
                            "potion",
                            "loot"
                    ),
                    args[2]
            );
        }

        if (args.length == 4) {
            if (args[2].equalsIgnoreCase("type")) {
                return filter(
                        getLivingEntityTypeNames(),
                        args[3]
                );
            }

            if (args[2].equalsIgnoreCase("bossbar")
                    || args[2].equalsIgnoreCase("actionbar")) {
                return filter(
                        List.of("true", "false"),
                        args[3]
                );
            }

            if (args[2].equalsIgnoreCase("potion")) {
                return filter(
                        List.of(
                                "add",
                                "remove",
                                "clear",
                                "list"
                        ),
                        args[3]
                );
            }

            if (args[2].equalsIgnoreCase("loot")) {
                return filter(
                        List.of(
                                "addhand",
                                "add",
                                "remove",
                                "clear",
                                "list"
                        ),
                        args[3]
                );
            }
        }

        if (args.length == 5
                && args[2].equalsIgnoreCase("potion")
                && (
                args[3].equalsIgnoreCase("add")
                        || args[3].equalsIgnoreCase("remove")
        )) {
            return filter(
                    getPotionEffectNames(),
                    args[4]
            );
        }

        if (args.length == 5
                && args[2].equalsIgnoreCase("loot")
                && args[3].equalsIgnoreCase("add")) {
            return filter(
                    getItemMaterialNames(),
                    args[4]
            );
        }

        if (args.length == 6
                && args[2].equalsIgnoreCase("potion")
                && args[3].equalsIgnoreCase("add")) {
            return filter(
                    List.of("30", "60", "120", "300", "-1"),
                    args[5]
            );
        }

        if (args.length == 7
                && args[2].equalsIgnoreCase("potion")
                && args[3].equalsIgnoreCase("add")) {
            return filter(
                    List.of("1", "2", "3", "4", "5"),
                    args[6]
            );
        }

        if (args.length >= 8
                && args.length <= 10
                && args[2].equalsIgnoreCase("potion")
                && args[3].equalsIgnoreCase("add")) {
            return filter(
                    List.of("true", "false"),
                    args[args.length - 1]
            );
        }

        return List.of();
    }

    private boolean requiresBossId(String subCommand) {
        return List.of(
                "edit",
                "enable",
                "disable",
                "spawn",
                "spawnat",
                "addloot",
                "clearloot",
                "delete",
                "info"
        ).contains(subCommand.toLowerCase(Locale.ROOT));
    }

    private List<String> getBossIds() {
        return bossManager.getBosses()
                .stream()
                .map(CustomBoss::getId)
                .toList();
    }

    private List<String> getLivingEntityTypeNames() {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .map(EntityType::name)
                .toList();
    }

    private List<String> getPotionEffectNames() {
        List<String> effectNames = new ArrayList<>();

        for (PotionEffectType type : Registry.EFFECT) {
            effectNames.add(type.getKey().getKey());
        }

        return effectNames;
    }

    private List<String> getItemMaterialNames() {
        return Arrays.stream(Material.values())
                .filter(Material::isItem)
                .filter(material -> !material.isAir())
                .map(Material::name)
                .toList();
    }

    private List<String> getBossFeatureNames() {
        return List.of(
                "bossbar",
                "actionbar"
        );
    }

    private List<String> filter(
            Collection<String> values,
            String input
    ) {
        String normalizedInput =
                input.toLowerCase(Locale.ROOT);

        return values.stream()
                .filter(Objects::nonNull)
                .filter(
                        value -> value
                                .toLowerCase(Locale.ROOT)
                                .startsWith(normalizedInput)
                )
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    private enum BossFeature {
        BOSS_BAR,
        ACTION_BAR
    }
}
