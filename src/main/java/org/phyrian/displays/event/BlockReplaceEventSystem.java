package org.phyrian.displays.event;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.DisplayContainerBlock;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockReplaceEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BlockReplaceEventSystem extends EntityEventSystem<ChunkStore, BlockReplaceEvent> {

  public BlockReplaceEventSystem() {
    super(BlockReplaceEvent.class);
  }

  @Override
  public void handle(int i, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
      @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer,
      @Nonnull BlockReplaceEvent event) {
    var chunkRef = event.getChunkRef();
    if (chunkRef == null) {
      return;
    }

    var componentType = DisplayContainerBlock.getComponentType();
    var component = store.getComponent(chunkRef, componentType);
    if (component != null) {
      var world = store.getExternalData().getWorld();
      var entityStore = world.getEntityStore().getStore();
      var pos = new Vector3i(event.getSelfX(), event.getSelfY(), event.getSelfZ());

      var indexChunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
      var chunk = world.getChunk(indexChunk);

      if (chunk == null) {
        return;
      }

      var entityCommandBuffer = new CommandBuffer<>(entityStore) {
        @Override
        public void run(@Nonnull Consumer<Store<EntityStore>> consumer) {
          commandBuffer.run((_) -> consumer.accept(entityStore));
        }
      };

      if (event.getNewEntity().getComponent(componentType) == null) {
        component.onDestroy(entityCommandBuffer, pos, chunk);
      } else {
        var blockType = chunk.getBlockType(pos);
        var rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);
        component.update(entityCommandBuffer, pos, chunk, blockType, rotationIndex);
      }
    }
  }

  @Nullable
  @Override
  public Query<ChunkStore> getQuery() {
    return Archetype.empty();
  }
}
