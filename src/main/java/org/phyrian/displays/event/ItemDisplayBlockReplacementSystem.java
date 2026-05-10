package org.phyrian.displays.event;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.DisplayContainerBlock;
import org.phyrian.displays.component.ItemDisplayBlock;
import org.phyrian.displays.config.DisplayContainer;
import org.phyrian.displays.config.ItemFilter;
import org.phyrian.displays.config.ItemFilterType;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class ItemDisplayBlockReplacementSystem extends RefSystem<ChunkStore> {

  @Override
  public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason,
      @Nonnull Store<ChunkStore> chunkStore, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
    var oldComponentType = ItemDisplayBlock.getComponentType();
    var oldComponent = chunkStore.getComponent(ref, oldComponentType);
    if (oldComponent == null) {
      return;
    }

    var newComponentType = DisplayContainerBlock.getComponentType();
    if (chunkStore.getComponent(ref, newComponentType) != null) {
      LOGGER.atInfo().log("Removing deprecated " + oldComponentType);
      commandBuffer.run((store) -> store.removeComponent(ref, oldComponentType));
      return;
    }

    LOGGER.atInfo().log("Replacing deprecated " + oldComponentType + " with " + newComponentType);

    var itemFilters = getItemFilters(oldComponent);
    var newComponent = new DisplayContainerBlock(getDisplayContainers(oldComponent), itemFilters);
    commandBuffer.run((store) -> {
      store.addComponent(ref, newComponentType, newComponent);
      store.removeComponent(ref, oldComponentType);
    });
  }

  @Override
  public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason,
      @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
    // no-op
  }

  @Nullable
  @Override
  public Query<ChunkStore> getQuery() {
    return Archetype.of(ItemDisplayBlock.getComponentType());
  }

  private DisplayContainer[] getDisplayContainers(ItemDisplayBlock itemDisplayBlock) {
    return new DisplayContainer[]{
        new DisplayContainer(
            itemDisplayBlock.getAnchoredEntityId(),
            itemDisplayBlock.getDisplayTransform(),
            itemDisplayBlock.getDisplayOrientation(),
            itemDisplayBlock.getDisplayKind()
        )
    };
  }

  private ItemFilter[] getItemFilters(ItemDisplayBlock itemDisplayBlock) {
    var allowedItems = itemDisplayBlock.getAllowedItems();
    var allowedBlockTypes = itemDisplayBlock.getAllowedBlockTypes();

    var itemFilters = new ItemFilter[]{};
    if (allowedItems != null) {
      var itemIdFilter = new ItemFilter(ItemFilterType.ItemId, allowedItems.toArray(String[]::new));
      var newLength = itemFilters.length + 1;
      itemFilters = Arrays.copyOf(itemFilters, newLength);
      itemFilters[newLength - 1] = itemIdFilter;
    }
    if (allowedBlockTypes != null) {
      var blockTypeFilter = new ItemFilter(ItemFilterType.BlockType, allowedBlockTypes.toArray(String[]::new));
      var newLength = itemFilters.length + 1;
      itemFilters = Arrays.copyOf(itemFilters, newLength);
      itemFilters[newLength - 1] = blockTypeFilter;
    }
    return itemFilters;
  }
}
