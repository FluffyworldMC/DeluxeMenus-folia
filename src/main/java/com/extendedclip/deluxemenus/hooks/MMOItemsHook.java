package com.extendedclip.deluxemenus.hooks;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.cache.SimpleCache;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MMOItemsHook implements ItemHook, SimpleCache {

    private final Map<String, ItemStack> cache = new ConcurrentHashMap<>();
    private final DeluxeMenus plugin;

    public MMOItemsHook(final @NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack getItem(@NotNull final String... arguments) {
        if (arguments.length == 0) {
            return new ItemStack(Material.STONE, 1);
        }

        final ItemStack cached = cache.get(arguments[0]);
        if (cached != null) {
            return cached.clone();
        }

        String[] splitArgs = arguments[0].split(":", 2);
        if (splitArgs.length != 2) {
            return new ItemStack(Material.STONE, 1);
        }

        final Type itemType = MMOItems.plugin.getTypes().get(splitArgs[0]);
        if (itemType == null) {
            return new ItemStack(Material.STONE, 1);
        }

        ItemStack mmoItem = null;
        try {
            mmoItem = loadItem(itemType, splitArgs[1], arguments[0]);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            plugin.debug(DebugLevel.HIGHEST, Level.SEVERE, "Error getting MMOItem synchronously.");
        } catch (final Exception exception) {
            plugin.debug(DebugLevel.HIGHEST, Level.SEVERE, "Error getting MMOItem synchronously.");
        }

        return mmoItem == null ? new ItemStack(Material.STONE, 1) : mmoItem;
    }

    /**
     * MMOItems builds templates on the global region. Waiting for that region from a Folia region thread can deadlock,
     * so a region thread loads the item inline. Other threads hop to the global region and wait.
     */
    private ItemStack loadItem(@NotNull final Type itemType, @NotNull final String id, @NotNull final String cacheKey) throws InterruptedException, ExecutionException {
        if (DeluxeMenus.getScheduler().isGlobalThread() || (DeluxeMenus.usesRegionScheduler() && DeluxeMenus.getScheduler().isTickThread())) {
            return createItem(itemType, id, cacheKey);
        }

        final CompletableFuture<ItemStack> future = new CompletableFuture<>();
        DeluxeMenus.getScheduler().execute(() -> {
            try {
                future.complete(createItem(itemType, id, cacheKey));
            } catch (final Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future.get();
    }

    private ItemStack createItem(@NotNull final Type itemType, @NotNull final String id, @NotNull final String cacheKey) {
        final ItemStack item = MMOItems.plugin.getItem(itemType, id);

        if (item == null) {
            return new ItemStack(Material.STONE, 1);
        }

        cache.put(cacheKey, item);
        return item.clone();
    }

    @Override
    public boolean itemMatchesIdentifiers(@NotNull ItemStack item, @NotNull String... arguments) {
        if (arguments.length == 0) {
            return false;
        }
        String[] splitArgs = arguments[0].split(":", 2);
        if (splitArgs.length != 2) return false;
        return splitArgs[0].equalsIgnoreCase(MMOItems.getTypeName(item)) && splitArgs[1].equalsIgnoreCase(MMOItems.getID(item));
    }

    @Override
    public String getPrefix() {
        return "mmoitems-";
    }

    @Override
    public void clearCache() {
        cache.clear();
    }
}
