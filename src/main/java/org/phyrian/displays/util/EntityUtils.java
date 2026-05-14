package org.phyrian.displays.util;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityUtils {

  private EntityUtils() {
  }

  public static List<Ref<EntityStore>> findEntities(
      WorldChunk chunk, Query<EntityStore> query) {
    var entityStore = chunk.getWorld().getEntityStore().getStore();
    var entityChunk = chunk.getEntityChunk();
    if (entityChunk != null) {
      var entityReferences = entityChunk.getEntityReferences();
      return entityReferences.stream()
          .filter(entityRef -> {
            var archetype = entityStore.getArchetype(entityRef);
            return query.test(archetype);
          })
          .toList();
    }

    return List.of();
  }

  public static Ref<EntityStore> getEntity(World world, UUID entityId) {
    if (entityId == null) {
      return null;
    }

    return world.getEntityStore().getRefFromUUID(entityId);
  }
}
