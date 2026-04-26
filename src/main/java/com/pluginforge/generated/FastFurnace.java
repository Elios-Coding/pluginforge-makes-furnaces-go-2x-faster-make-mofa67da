package com.pluginforge.generated;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnacesSmeltItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;
import java.util.Map;

public class FastFurnace extends JavaPlugin implements Listener {

    private boolean enabled;
    private int speedMultiplier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        enabled = config.getBoolean("enabled", true);
        speedMultiplier = config.getInt("speed-multiplier", 2);
        if (speedMultiplier < 1) {
            speedMultiplier = 1;
            config.set("speed-multiplier", 1);
            saveConfig();
        }
        if (speedMultiplier > 1000) {
            speedMultiplier = 1000;
            config.set("speed-multiplier", 1000);
            saveConfig();
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FastFurnace has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FastFurnace has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fastfurnace.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /fastfurnace [on|off|number]");
            sender.sendMessage("§eCurrent status: " + (enabled ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage("§eCurrent speed multiplier: §b" + speedMultiplier);
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            enabled = true;
            getConfig().set("enabled", true);
            saveConfig();
            sender.sendMessage("§aFastFurnace has been enabled.");
            return true;
        } else if (args[0].equalsIgnoreCase("off")) {
            enabled = false;
            getConfig().set("enabled", false);
            saveConfig();
            sender.sendMessage("§cFastFurnace has been disabled.");
            return true;
        } else {
            try {
                int newMultiplier = Integer.parseInt(args[0]);
                if (newMultiplier >= 1 && newMultiplier <= 1000) {
                    speedMultiplier = newMultiplier;
                    getConfig().set("speed-multiplier", newMultiplier);
                    saveConfig();
                    sender.sendMessage("§aFastFurnace speed multiplier set to: §b" + speedMultiplier);
                } else {
                    sender.sendMessage("§cSpeed multiplier must be between 1 and 1000.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid argument. Please use 'on', 'off', or a number.");
            }
            return true;
        }
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (!enabled) return;

        int originalBurnTime = event.getBurnTime();
        int newBurnTime = originalBurnTime / speedMultiplier;
        if (newBurnTime < 1) newBurnTime = 1; // Ensure burn time is at least 1 tick
        event.setBurnTime(newBurnTime);
    }

    @EventHandler
    public void onFurnaceSmelt(FurnacesSmeltItemEvent event) {
        if (!enabled) return;

        Block block = event.getBlock();
        if (!(block.getState() instanceof Furnace)) return;

        Furnace furnace = (Furnace) block.getState();
        Material smelting = event.getSource().getType();

        // Get the original cook time for the recipe
        int originalCookTime = getOriginalCookTime(smelting);

        if (originalCookTime > 0) {
            int newCookTime = originalCookTime / speedMultiplier;
            if (newCookTime < 1) newCookTime = 1; // Ensure cook time is at least 1 tick
            furnace.setCookTimeTotal((short) newCookTime);
            furnace.update(); // Update the furnace state to apply the new cook time
        }
    }

    private int getOriginalCookTime(Material smeltedMaterial) {
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            if (recipe instanceof FurnaceRecipe) {
                FurnaceRecipe furnaceRecipe = (FurnaceRecipe) recipe;
                if (furnaceRecipe.getInput().getType() == smeltedMaterial) {
                    // Default cook time for furnaces is 200 ticks (10 seconds)
                    // Paper API does not expose getCookTime(), so we use the default and modify it from there.
                    return 200; 
                }
            }
        }
        return 0;
    }
}
