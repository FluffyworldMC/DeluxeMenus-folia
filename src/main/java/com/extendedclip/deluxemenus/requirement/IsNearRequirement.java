package com.extendedclip.deluxemenus.requirement;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.menu.MenuHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class IsNearRequirement extends Requirement {

  private final Location location;
  private final int distance;
  private final boolean invert;

  public IsNearRequirement(Location location, int distance, boolean invert) {
    this.location = location;
    this.distance = distance;
    this.invert = invert;
  }

  @Override
  public boolean evaluate(MenuHolder holder) {
    final Player viewer = holder.getViewer();
    if (viewer == null) {
      return false;
    }

    // Another region must not read this player's world or location.
    if (DeluxeMenus.usesRegionScheduler() && !DeluxeMenus.getScheduler().isEntityThread(viewer)) {
      return false;
    }

    boolean withinRange = false;
    if (viewer.getWorld().getName().equals(location.getWorld().getName())) {
      withinRange = viewer.getLocation().distance(location) < distance;
    }
    return invert ? !withinRange : withinRange;
  }
}
