package org.phyrian.displays.config;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.phyrian.displays.component.DisplayedItemComponent;
import org.phyrian.displays.util.DisplayUtils;
import org.phyrian.displays.util.EntityUtils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class DisplaySlot {

  public static final Codec<DisplaySlot> CODEC;

  protected UUID anchoredEntityId;
  protected DisplayTransform displayTransform = new DisplayTransform();
  protected DisplayOrientation displayOrientation = DisplayOrientation.Horizontal;
  protected DisplayKind displayKind = DisplayKind.Default;
  protected ItemFilter[] itemFilters;
  protected String addItemSoundEventId;
  @Setter(AccessLevel.NONE)
  protected transient int addItemSoundEventIndex;
  protected String removeItemSoundEventId;
  @Setter(AccessLevel.NONE)
  protected transient int removeItemSoundEventIndex;

  private DisplaySlot() {
  }

  public DisplaySlot(UUID anchoredEntityId, DisplayTransform displayTransform,
      DisplayOrientation displayOrientation, DisplayKind displayKind) {
    this.anchoredEntityId = anchoredEntityId;
    this.displayTransform = displayTransform;
    this.displayOrientation = displayOrientation;
    this.displayKind = displayKind;
    this.processConfig();
  }

  public DisplaySlot(DisplaySlot other) {
    this.anchoredEntityId = other.anchoredEntityId;
    this.displayTransform = other.displayTransform.clone();
    this.displayOrientation = other.displayOrientation;
    this.displayKind = other.displayKind;
    this.itemFilters = Arrays.copyOf(other.itemFilters, other.itemFilters.length);
    this.addItemSoundEventId = other.addItemSoundEventId;
    this.addItemSoundEventIndex = other.addItemSoundEventIndex;
    this.removeItemSoundEventId = other.removeItemSoundEventId;
    this.removeItemSoundEventIndex = other.removeItemSoundEventIndex;
  }

  public boolean canHoldItem(String itemId) {
    if (itemFilters == null || itemFilters.length == 0) {
      return true;
    }

    for (var itemFilter : itemFilters) {
      if (itemFilter.matches(itemId)) {
        return true;
      }
    }

    return false;
  }

  public boolean addItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, ItemStack itemStack, BlockType blockType, int rotationIndex) {
    if (anchoredEntityId != null) {
      return false;
    }

    if (!canHoldItem(itemStack.getItemId())) {
      return false;
    }

    var variantRotation = blockType.getVariantRotation();
    var blockTransform = DisplayUtils.getBlockTransform(pos, rotationIndex, variantRotation,
        displayOrientation, displayTransform);

    commandBuffer.run((store) -> {
      var uuid = UUID.randomUUID();
      var component = new DisplayedItemComponent(itemStack, pos, blockTransform.getPosition());

      var holder = DisplayUtils.createDisplayEntity(store, itemStack, rotationIndex,
          displayOrientation, blockTransform, displayKind);
      holder.putComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
      holder.putComponent(DisplayedItemComponent.getComponentType(), component);
      store.addEntity(holder, AddReason.SPAWN);

      this.setAnchoredEntityId(uuid);
      if (addItemSoundEventIndex != 0) {
        SoundUtil.playSoundEvent3d(ref, addItemSoundEventIndex, (double) pos.x + (double) 0.5F,
            (double) pos.y + (double) 0.5F, (double) pos.z + (double) 0.5F, commandBuffer);
      }
    });

    return true;
  }

  public boolean removeItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref,
      Vector3i pos, WorldChunk chunk) {
    var anchoredEntity = findAnchoredEntity(pos, chunk);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      return true;
    }

    if (!anchoredEntity.isValid()) {
      return true;
    }

    commandBuffer.run((store) -> {
      var component = store.getComponent(anchoredEntity, DisplayedItemComponent.getComponentType());
      if (component != null) {
        component.dropItem(store, ref);
      }

      store.removeEntity(anchoredEntity, RemoveReason.REMOVE);

      this.setAnchoredEntityId(null);
      if (removeItemSoundEventIndex != 0) {
        SoundUtil.playSoundEvent3d(ref, removeItemSoundEventIndex, (double) pos.x + (double) 0.5F,
            (double) pos.y + (double) 0.5F, (double) pos.z + (double) 0.5F, commandBuffer);
      }
    });

    return true;
  }

  public void update(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, WorldChunk chunk,
      BlockType blockType, int rotationIndex) {
    var anchoredEntity = findAnchoredEntity(pos, chunk);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      return;
    }

    if (!anchoredEntity.isValid()) {
      return;
    }

    var variantRotation = blockType.getVariantRotation();
    var blockTransform = DisplayUtils.getBlockTransform(pos, rotationIndex, variantRotation,
        displayOrientation, displayTransform);

    commandBuffer.run((store) -> {
      var component = store.getComponent(anchoredEntity, DisplayedItemComponent.getComponentType());

      if (component == null) {
        return;
      }

      var itemStack = component.getItemStack();

      // remove previous display entity after retrieving itemStack
      store.removeEntity(anchoredEntity, RemoveReason.REMOVE);
      this.setAnchoredEntityId(null);

      var uuid = UUID.randomUUID();
      var newComponent = new DisplayedItemComponent(itemStack, pos, blockTransform.getPosition());

      var holder = DisplayUtils.createDisplayEntity(store, itemStack, rotationIndex,
          displayOrientation, blockTransform, displayKind);
      holder.putComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
      holder.putComponent(DisplayedItemComponent.getComponentType(), newComponent);
      store.addEntity(holder, AddReason.SPAWN);

      this.setAnchoredEntityId(uuid);
    });
  }

  public void onDestroy(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, WorldChunk chunk) {
    var anchoredEntity = findAnchoredEntity(pos, chunk);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      return;
    }

    if (!anchoredEntity.isValid()) {
      return;
    }

    commandBuffer.run((store) -> {
      var display = store.getComponent(anchoredEntity, DisplayedItemComponent.getComponentType());
      if (display != null) {
        display.dropItem(store);
      }

      store.removeEntity(anchoredEntity, RemoveReason.REMOVE);
      this.setAnchoredEntityId(null);
    });
  }

  private Ref<EntityStore> findAnchoredEntity(Vector3i pos, WorldChunk chunk) {
    var world = chunk.getWorld();

    var anchoredEntity = EntityUtils.getEntity(world, this.anchoredEntityId);
    if (anchoredEntity != null) {
      return anchoredEntity;
    }

    // lookup displayed entity by display position
    return EntityUtils.lookupEntity(chunk, DisplayedItemComponent.getComponentType(),
        component -> Objects.equals(component.getDisplayPosition(), pos));
  }

  public void refresh() {
    processConfig();
  }

  @Override
  public DisplaySlot clone() {
    return new DisplaySlot(this);
  }

  protected void processConfig() {
    if (addItemSoundEventId != null) {
      addItemSoundEventIndex = SoundEvent.getAssetMap().getIndex(addItemSoundEventId);
    }
    if (removeItemSoundEventId != null) {
      removeItemSoundEventIndex = SoundEvent.getAssetMap().getIndex(removeItemSoundEventId);
    }
  }

  static {
    CODEC = BuilderCodec.builder(DisplaySlot.class, DisplaySlot::new)
        .append(new KeyedCodec<>("AnchoredEntity", Codec.UUID_BINARY),
            (component, anchoredEntity) -> component.anchoredEntityId = anchoredEntity,
            (component) -> component.anchoredEntityId)
        .add()
        .append(new KeyedCodec<>("DisplayTransform", DisplayTransform.CODEC),
            (component, displayTransform) -> component.displayTransform = Objects.requireNonNullElseGet(displayTransform, DisplayTransform::new),
            (component) -> component.displayTransform)
        .add()
        .append(new KeyedCodec<>("DisplayKind", new EnumCodec<>(DisplayKind.class)),
            (component, displayKind) -> component.displayKind = Objects.requireNonNullElse(displayKind, DisplayKind.Default),
            (component) -> component.displayKind)
        .add()
        .append(new KeyedCodec<>("DisplayOrientation", new EnumCodec<>(DisplayOrientation.class)),
            (component, displayOrientation) -> component.displayOrientation = Objects.requireNonNullElse(displayOrientation, DisplayOrientation.Horizontal),
            (component) -> component.displayOrientation)
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
        .afterDecode(DisplaySlot::processConfig)
        .build();
  }

}
