package com.extendedclip.deluxemenus.utils;

import com.github.Anon8281.universalScheduler.bukkitScheduler.BukkitScheduler;
import com.github.Anon8281.universalScheduler.foliaScheduler.FoliaScheduler;
import com.github.Anon8281.universalScheduler.paperScheduler.PaperScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the scheduler for the current server implementation.
 * <p>
 * This intentionally does not use {@code UniversalScheduler#getScheduler(Plugin)} from the
 * UniversalScheduler library. Its class detection calls {@link Class#forName(String)} through the
 * plugin class loader, which on Paper also searches the class loaders of other plugins. A plugin
 * whose jar is unreadable or already closed then makes that lookup throw a runtime exception. Since
 * the detection happens in a static initializer, the whole scheduler class fails to initialize and
 * DeluxeMenus cannot enable.
 */
public final class SchedulerFactory {

    private static final boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    private static final boolean IS_CANVAS = classExists("io.canvasmc.canvas.server.ThreadedServer");
    private static final boolean IS_EXPANDED_SCHEDULING_AVAILABLE = classExists("io.papermc.paper.threadedregions.scheduler.ScheduledTask");

    private SchedulerFactory() {
        throw new AssertionError("Util classes should not be initialized");
    }

    public static @NotNull TaskScheduler getScheduler(final @NotNull Plugin plugin) {
        if (usesRegionScheduler()) {
            return new FoliaScheduler(plugin);
        }

        return IS_EXPANDED_SCHEDULING_AVAILABLE ? new PaperScheduler(plugin) : new BukkitScheduler(plugin);
    }

    public static boolean usesRegionScheduler() {
        return IS_FOLIA || IS_CANVAS;
    }

    private static boolean classExists(final @NotNull String className) {
        try {
            // Use the server's class loader instead of the plugin class loader. Paper's plugin
            // class loader delegates to other plugins' class loaders, and one of them may be in a
            // broken state (for example, after its jar has been closed).
            Class.forName(className, false, Bukkit.class.getClassLoader());
            return true;
        } catch (final Exception | LinkageError ignored) {
            return false;
        }
    }
}
