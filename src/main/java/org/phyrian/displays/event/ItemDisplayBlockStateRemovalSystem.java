package org.phyrian.displays.event;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.DisplayedItemComponent;
import org.phyrian.displays.util.BlockUtils;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class ItemDisplayBlockStateRemovalSystem extends RefSystem<EntityStore> {

  private static final Set<String> OLD_STATE_DISPLAY_BLOCKS =
      Set.of(
          "SimpleItemDisplays_Feran_Flowerpot",
          "SimpleItemDisplays_Frozen_Castle_Flowerpot",
          "SimpleItemDisplays_Human_Ruins_Flowerpot",
          "SimpleItemDisplays_Jungle_Flowerpot",
          "SimpleItemDisplays_Royal_Magic_Flowerpot",
          "SimpleItemDisplays_Temple_Dark_Flowerpot",
          "SimpleItemDisplays_Temple_Light_Flowerpot",
          "SimpleItemDisplays_Temple_Wind_Flowerpot",
          "SimpleItemDisplays_Village_Flowerpot",
          "SimpleItemDisplays_Village_Item_Frame"
      );

  @Override
  public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason addReason,
      @Nonnull Store<EntityStore> entityStore, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    var displayedItemComponent = entityStore.getComponent(ref, DisplayedItemComponent.getComponentType());
    if (displayedItemComponent == null) {
      return;
    }

    var world = entityStore.getExternalData().getWorld();
    var pos = displayedItemComponent.getDisplayPosition();
    var chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
    var chunk = world.getChunk(chunkIndex);
    var blockType = world.getBlockType(pos);
    var rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

    if (blockType == null || !blockType.isState() || chunk == null) {
      return;
    }

    var key = blockType.getDefaultStateKey();
    if (!OLD_STATE_DISPLAY_BLOCKS.contains(key)) {
      return;
    }

    if (!BlockUtils.isInState(blockType, BlockUtils.FULL_STATE)) {
      return;
    }

    LOGGER.atInfo().log("Removing deprecated Full state from block " + key + " at position " + pos);

    BlockUtils.changeState(commandBuffer, null, pos, chunk, blockType, rotationIndex,
        BlockUtils.DEFAULT_STATE);
  }

  @Override
  public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason removeReason,
      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    // no-op
  }

  @Nullable
  @Override
  public Query<EntityStore> getQuery() {
    return DisplayedItemComponent.getComponentType();
  }
}
