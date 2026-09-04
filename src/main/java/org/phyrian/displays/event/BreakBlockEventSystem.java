package org.phyrian.displays.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.util.BlockUtils;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

  public BreakBlockEventSystem() {
    super(BreakBlockEvent.class);
  }

  @Override
  public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull BreakBlockEvent event) {
    var world = store.getExternalData().getWorld();
    var targetBlock = event.getTargetBlock();

    var display = BlockUtils.getDisplayContainerBlock(world, targetBlock);
    if (display != null) {
      commandBuffer.run(store1 -> display.onDestroy(store1, targetBlock, world));
    }
  }

  @Nullable
  @Override
  public Query<EntityStore> getQuery() {
    return Archetype.empty();
  }
}
