package org.phyrian.displays.util;

import java.util.UUID;
import java.util.function.Predicate;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.Pair;

public class EntityUtils {

  private EntityUtils() {
  }

  public static <C extends Component<EntityStore>> Ref<EntityStore> lookupEntity(WorldChunk chunk,
      ComponentType<EntityStore, C> componentType, Predicate<C> predicate) {
    var entityStore = chunk.getWorld().getEntityStore().getStore();
    var entityChunk = chunk.getEntityChunk();
    if (entityChunk != null) {
      var entityReferences = entityChunk.getEntityReferences();
      var optFoundEntity = entityReferences.stream()
          .map(entityRef -> Pair.of(entityRef, entityStore.getComponent(entityRef, componentType)))
          .filter(pair -> pair.value() != null)
          .filter(predicate != null ? pair -> predicate.test(pair.value()) : _ -> true)
          .findFirst();
      if (optFoundEntity.isPresent()) {
        return optFoundEntity.get().key();
      }
    }

    return null;
  }

  public static Ref<EntityStore> getEntity(World world, UUID entityId) {
    if (entityId == null) {
      return null;
    }

    return world.getEntityStore().getRefFromUUID(entityId);
  }
}
