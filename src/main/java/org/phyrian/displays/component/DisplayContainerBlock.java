package org.phyrian.displays.component;

import java.util.Objects;
import java.util.UUID;

import org.phyrian.displays.config.DisplaySlot;
import org.phyrian.displays.config.ItemFilter;

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
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Data;
import static org.phyrian.displays.util.ReflectionUtils.copyArray;

@Data
public class DisplayContainerBlock implements Component<ChunkStore> {

  public static final BuilderCodec<DisplayContainerBlock> CODEC;
  public static ComponentType<ChunkStore, DisplayContainerBlock> TYPE;

  protected DisplaySlot[] displaySlots = new DisplaySlot[]{};
  protected ItemFilter[] itemFilters;
  protected String addItemSoundEventId;
  protected String removeItemSoundEventId;

  private DisplayContainerBlock() {
  }

  public DisplayContainerBlock(DisplaySlot[] displaySlots, ItemFilter[] itemFilters) {
    this.displaySlots = displaySlots;
    this.itemFilters = itemFilters;
    this.processConfig();
  }

  public DisplayContainerBlock(DisplayContainerBlock other) {
    this.displaySlots = copyArray(other.displaySlots, DisplaySlot::clone);
    this.itemFilters = copyArray(other.itemFilters);
    this.addItemSoundEventId = other.addItemSoundEventId;
    this.removeItemSoundEventId = other.removeItemSoundEventId;
  }

  public boolean addItem(ItemContainer itemContainer, byte slot, int amount,
      CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos,
      BlockType blockType, int rotationIndex) {
    for (var displaySlot : displaySlots) {
      if (displaySlot.addItem(itemContainer, slot, amount, commandBuffer, ref, pos, blockType,
          rotationIndex)) {
        return true;
      }
    }
    return false;
  }

  public boolean removeItem(UUID entityId, CommandBuffer<EntityStore> commandBuffer,
      Ref<EntityStore> ref, Vector3i pos, World world) {
    for (var displaySlot : displaySlots) {
      if (Objects.equals(displaySlot.getAnchoredEntityId(), entityId)
          && displaySlot.removeItem(commandBuffer, ref, pos, world)) {
        return true;
      }
    }
    return false;
  }

  public boolean removeLastItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, World world) {
    for (int i = displaySlots.length - 1; i >= 0; i--) {
      var displaySlot = displaySlots[i];
      if (displaySlot.removeItem(commandBuffer, ref, pos, world)) {
        return true;
      }
    }
    return false;
  }

  public void update(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, World world,
      BlockType blockType, int rotationIndex) {
    for (var displaySlot : displaySlots) {
      displaySlot.update(commandBuffer, pos, world, blockType, rotationIndex);
    }
  }

  public void onDestroy(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, World world) {
    for (var displaySlot : displaySlots) {
      displaySlot.onDestroy(commandBuffer, pos, world);
    }
  }

  @Override
  public DisplayContainerBlock clone() {
    return new DisplayContainerBlock(this);
  }

  protected void processConfig() {
    for (var displaySlot : displaySlots) {
      if (displaySlot.getItemFilters() == null) {
        displaySlot.setItemFilters(itemFilters);
      }
      if (displaySlot.getAddItemSoundEventId() == null) {
        displaySlot.setAddItemSoundEventId(addItemSoundEventId);
      }
      if (displaySlot.getRemoveItemSoundEventId() == null) {
        displaySlot.setRemoveItemSoundEventId(removeItemSoundEventId);
      }
      displaySlot.refresh();
    }
  }

  public static ComponentType<ChunkStore, DisplayContainerBlock> getComponentType() {
    return TYPE;
  }

  static {
    CODEC = BuilderCodec.builder(DisplayContainerBlock.class, DisplayContainerBlock::new)
        .append(new KeyedCodec<>("DisplaySlots", new ArrayCodec<>(DisplaySlot.CODEC, DisplaySlot[]::new)),
            (component, displaySlots) -> component.displaySlots = displaySlots,
            (component) -> component.displaySlots)
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
