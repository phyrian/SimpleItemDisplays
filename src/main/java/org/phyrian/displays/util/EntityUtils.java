package org.phyrian.displays.util;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityUtils {

  private EntityUtils() {
  }

  public static Ref<EntityStore> getEntity(World world, UUID entityId) {
    if (entityId == null) {
      return null;
    }

    return world.getEntityStore().getRefFromUUID(entityId);
  }
}
