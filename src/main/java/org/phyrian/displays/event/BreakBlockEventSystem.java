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
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

  public BreakBlockEventSystem() {
    super(BreakBlockEvent.class);
  }

  @Override
  public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull BreakBlockEvent event) {
    var world = store.getExternalData().getWorld();
    var pos = event.getTargetBlock();

    var indexChunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
    var chunk = world.getChunk(indexChunk);
    var blockType = world.getBlockType(pos);

    if (blockType == null || chunk == null) {
      return;
    }

    var chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
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
