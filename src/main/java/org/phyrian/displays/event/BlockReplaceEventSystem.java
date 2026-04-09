package org.phyrian.displays.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.ItemDisplayBlock;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.block.BlockReplaceEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BlockReplaceEventSystem extends EntityEventSystem<EntityStore, BlockReplaceEvent> {

  public BlockReplaceEventSystem() {
    super(BlockReplaceEvent.class);
  }

  @Override
  public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull BlockReplaceEvent event) {
    var world = store.getExternalData().getWorld();
    var chunkRef = event.getChunkRef();
    if (chunkRef == null) {
      return;
    }

    var chunkStore = world.getChunkStore().getStore();
    var itemDisplay = chunkStore.getComponent(chunkRef, ItemDisplayBlock.getComponentType());
    if (itemDisplay != null) {
      itemDisplay.onDestroy(commandBuffer, world);
    }
  }

  @Nullable
  @Override
  public Query<EntityStore> getQuery() {
    return Archetype.empty();
  }
}
