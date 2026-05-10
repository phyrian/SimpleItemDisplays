package org.phyrian.displays.component;

import java.util.Arrays;

import org.phyrian.displays.config.DisplayContainer;
import org.phyrian.displays.config.ItemFilter;
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
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Data;

@Data
public class DisplayContainerBlock implements Component<ChunkStore> {

  public static final BuilderCodec<DisplayContainerBlock> CODEC;
  public static ComponentType<ChunkStore, DisplayContainerBlock> TYPE;

  protected DisplayContainer[] displayContainers = new DisplayContainer[]{};
  protected ItemFilter[] itemFilters;
  protected String addItemSoundEventId;
  protected String removeItemSoundEventId;

  private DisplayContainerBlock() {
  }

  public DisplayContainerBlock(DisplayContainer[] displayContainers, ItemFilter[] itemFilters) {
    this.displayContainers = displayContainers;
    this.itemFilters = itemFilters;
    this.processConfig();
  }

  public DisplayContainerBlock(DisplayContainerBlock other) {
    this.displayContainers = ReflectionUtils.cloneArray(other.displayContainers, DisplayContainer.class);
    this.itemFilters = Arrays.copyOf(other.itemFilters, other.itemFilters.length);
    this.addItemSoundEventId = other.addItemSoundEventId;
    this.removeItemSoundEventId = other.removeItemSoundEventId;
  }

  public boolean addItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, ItemStack itemStack, BlockType blockType, int rotationIndex) {
    for (var displayContainer : displayContainers) {
      if (displayContainer.addItem(commandBuffer, ref, pos, itemStack, blockType, rotationIndex)) {
        return true;
      }
    }
    return false;
  }

  public boolean removeItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, WorldChunk chunk) {
    for (int i = displayContainers.length - 1; i >= 0; i--) {
      var displayContainer = displayContainers[i];
      if (displayContainer.removeItem(commandBuffer, ref, pos, chunk)) {
        return true;
      }
    }
    return false;
  }

  public void update(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, WorldChunk chunk,
      BlockType blockType, int rotationIndex) {
    for (var displayContainer : displayContainers) {
      displayContainer.update(commandBuffer, pos, chunk, blockType, rotationIndex);
    }
  }

  public void onDestroy(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, WorldChunk chunk) {
    for (var displayContainer : displayContainers) {
      displayContainer.onDestroy(commandBuffer, pos, chunk);
    }
  }

  @Override
  public DisplayContainerBlock clone() {
    return new DisplayContainerBlock(this);
  }

  protected void processConfig() {
    for (var displayContainer : displayContainers) {
      if (displayContainer.getItemFilters() == null) {
        displayContainer.setItemFilters(itemFilters);
      }
      if (displayContainer.getAddItemSoundEventId() == null) {
        displayContainer.setAddItemSoundEventId(addItemSoundEventId);
      }
      if (displayContainer.getRemoveItemSoundEventId() == null) {
        displayContainer.setRemoveItemSoundEventId(removeItemSoundEventId);
      }
      displayContainer.refresh();
    }
  }

  public static ComponentType<ChunkStore, DisplayContainerBlock> getComponentType() {
    return TYPE;
  }

  static {
    CODEC = BuilderCodec.builder(DisplayContainerBlock.class, DisplayContainerBlock::new)
        .append(new KeyedCodec<>("DisplayContainers", new ArrayCodec<>(DisplayContainer.CODEC, DisplayContainer[]::new)),
            (component, displayContainers) -> component.displayContainers = displayContainers,
            (component) -> component.displayContainers)
        .add()
        .append(new KeyedCodec<>("ItemFilters", new ArrayCodec<>(ItemFilter.CODEC, ItemFilter[]::new)),
            (component, itemFilters) -> component.itemFilters = itemFilters,
            (component) -> component.itemFilters)
        .addValidatorLate(() -> ItemFilter.VALIDATOR_CACHE.getArrayValidator().late())
        .add()
        .append(new KeyedCodec<>("AddItemSoundEventId", Codec.STRING),
            (component, addItemSoundEventId) -> component.addItemSoundEventId = addItemSoundEventId,
            (component) -> component.addItemSoundEventId)
        .addValidatorLate(() -> SoundEvent.VALIDATOR_CACHE.getValidator().late())
        .add()
        .append(new KeyedCodec<>("RemoveItemSoundEventId", Codec.STRING),
            (component, removeItemSoundEventId) -> component.removeItemSoundEventId = removeItemSoundEventId,
            (component) -> component.removeItemSoundEventId)
        .addValidatorLate(() -> SoundEvent.VALIDATOR_CACHE.getValidator().late())
        .add()
        .afterDecode(DisplayContainerBlock::processConfig)
        .build();
  }

}
