package org.phyrian.displays.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.ItemDisplayBlock;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class BreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

  public BreakBlockEventSystem() {
    super(BreakBlockEvent.class);
  }

  @Override
  public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull BreakBlockEvent event) {
    World world = store.getExternalData().getWorld();
    Vector3i pos = event.getTargetBlock();

    long indexChunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
    WorldChunk chunk = world.getChunk(indexChunk);
    BlockType blockType = world.getBlockType(pos);

    LOGGER.atInfo().log("Breaking block at " + pos + ": " + blockType);

    if (blockType == null || chunk == null) {
      return;
    }

    Ref<ChunkStore> chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    if (chunkRef == null) {
      return;
    }

    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    ItemDisplayBlock itemDisplay = chunkStore.getComponent(chunkRef, ItemDisplayBlock.getComponentType());
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
