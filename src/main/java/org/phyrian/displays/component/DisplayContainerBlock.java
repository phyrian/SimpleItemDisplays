package org.phyrian.displays.component;

import java.util.Arrays;
import java.util.Set;

import org.phyrian.displays.config.DisplayContainer;
import org.phyrian.displays.config.ItemFilter;
import org.phyrian.displays.util.BlockUtils;
import org.phyrian.displays.util.ItemUtils;
import org.phyrian.displays.util.ReflectionUtils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class DisplayContainerBlock implements Component<ChunkStore> {

  public static final BuilderCodec<DisplayContainerBlock> CODEC;
  public static ComponentType<ChunkStore, DisplayContainerBlock> TYPE;

  private int size;
  private DisplayContainer[] displayContainers;
  private ItemFilter[] itemFilters;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private transient Set<String> itemFilterCache = null;

  public DisplayContainerBlock() {
    this(0, new DisplayContainer[]{}, new ItemFilter[]{});
  }

  public DisplayContainerBlock(int size, DisplayContainer[] displayContainers,
      ItemFilter[] itemFilters) {
    this.size = size;
    this.displayContainers = displayContainers;
    this.itemFilters = itemFilters;
  }

  public DisplayContainerBlock(DisplayContainerBlock other) {
    this(other.size, ReflectionUtils.cloneArray(other.displayContainers, DisplayContainer.class),
        ReflectionUtils.cloneArray(other.itemFilters, ItemFilter.class));
  }

  public void setItemFilters(ItemFilter[] itemFilters) {
    if (!Arrays.deepEquals(this.itemFilters, itemFilters)) {
      invalidateItemFilterCache();
    }
    this.itemFilters = itemFilters;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public boolean isFull() {
    return size >= displayContainers.length;
  }

  public void invalidateItemFilterCache() {
    itemFilterCache = null;
  }

  public boolean canHoldItem(String itemId) {
    if (itemFilters == null || itemFilters.length == 0) {
      return true;
    }

    if (itemFilterCache == null) {
      itemFilterCache = ItemUtils.findMatchingItemIds(itemFilters);
    }

    return itemFilterCache.contains(itemId);
  }

  public boolean addItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, ItemStack itemStack, WorldChunk chunk, BlockType blockType, int rotationIndex) {
    if (isFull() || !canHoldItem(itemStack.getItemId())) {
      return false;
    }

    var nextSlot = displayContainers[size];
    if (nextSlot.setItem(commandBuffer, pos, itemStack, blockType, rotationIndex)) {
      size++;
      if (size == displayContainers.length) {
        BlockUtils.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex,
            BlockUtils.FULL_STATE);
      }
      return true;
    }
    return false;
  }

  public boolean removeItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, WorldChunk chunk, BlockType blockType, int rotationIndex) {
    if (isEmpty()) {
      return false;
    }

    var lastSlot = displayContainers[size - 1];
    if (lastSlot.removeItem(commandBuffer, ref, pos, chunk)) {
      if (size == displayContainers.length) {
        BlockUtils.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex,
            BlockUtils.DEFAULT_STATE);
      }
      size--;
      return true;
    }
    return false;
  }

  public void update(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, WorldChunk chunk, BlockType blockType, int rotationIndex) {
    var count = 0;
    for (var displayContainer : displayContainers) {
      if (displayContainer.update(commandBuffer, pos, chunk, blockType, rotationIndex)) {
        count++;
      }
    }

    String newState;
    if (count == displayContainers.length) {
      newState = BlockUtils.FULL_STATE;
    } else {
      newState = BlockUtils.DEFAULT_STATE;
    }

    size = count;
    BlockUtils.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, newState);
  }

  public void onDestroy(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, WorldChunk chunk) {
    for (int i = 0; i < size; i++) {
      displayContainers[i].onDestroy(commandBuffer, pos, chunk);
    }
  }

  @Override
  public DisplayContainerBlock clone() {
    return new DisplayContainerBlock(this);
  }

  public static ComponentType<ChunkStore, DisplayContainerBlock> getComponentType() {
    return TYPE;
  }

  static {
    CODEC = BuilderCodec.builder(DisplayContainerBlock.class, DisplayContainerBlock::new)
        .append(new KeyedCodec<>("Size", Codec.INTEGER),
            (component, size) -> component.size = size,
            (component) -> component.size)
        .add()
        .append(new KeyedCodec<>("DisplayContainers", new ArrayCodec<>(DisplayContainer.CODEC, DisplayContainer[]::new)),
            (component, displayContainers) -> component.displayContainers = displayContainers,
            (component) -> component.displayContainers)
        .add()
        .append(new KeyedCodec<>("ItemFilters", new ArrayCodec<>(ItemFilter.CODEC, ItemFilter[]::new)),
            (component, itemFilters) -> component.itemFilters = itemFilters,
            (component) -> component.itemFilters)
        .addValidatorLate(() -> ItemFilter.VALIDATOR_CACHE.getArrayValidator().late())
        .add()
        .build();
  }

}
