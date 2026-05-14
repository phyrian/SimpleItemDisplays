package org.phyrian.displays.config;

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
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import static com.hypixel.hytale.protocol.SoundCategory.SFX;
import static org.phyrian.displays.util.ReflectionUtils.copyArray;

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
  protected String interactionHitboxType; // TODO: implement interactable placeholder entity
  @Setter(AccessLevel.NONE)
  protected transient int interactionHitboxIndex;
  protected String interactionHintEmpty;
  protected String interactionHintFull;
  protected String interactionHint;

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
    this.itemFilters = copyArray(other.itemFilters);
    this.addItemSoundEventId = other.addItemSoundEventId;
    this.addItemSoundEventIndex = other.addItemSoundEventIndex;
    this.removeItemSoundEventId = other.removeItemSoundEventId;
    this.removeItemSoundEventIndex = other.removeItemSoundEventIndex;
    this.interactionHitboxType = other.interactionHitboxType;
    this.interactionHitboxIndex = other.interactionHitboxIndex;
    this.interactionHintEmpty = other.interactionHintEmpty;
    this.interactionHintFull = other.interactionHintFull;
    this.interactionHint = other.interactionHint;
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

  public boolean addItem(ItemContainer itemContainer, byte slot, int amount,
      CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos,
      BlockType blockType, int rotationIndex) {
    if (anchoredEntityId != null) {
      return false;
    }

    var itemStack = itemContainer.getItemStack(slot);
    if (itemStack == null) {
      return false;
    }

    if (!canHoldItem(itemStack.getItemId())) {
      return false;
    }

    var transferAmount = Math.min(itemStack.getQuantity(), amount);
    if (transferAmount <= 0) {
      return false;
    }

    var transaction = itemContainer.removeItemStackFromSlot(slot, itemStack, transferAmount);
    if (!transaction.succeeded()) {
      return false;
    }
    var takenItemStack = transaction.getOutput();

    var variantRotation = blockType.getVariantRotation();
    var blockTransform = DisplayUtils.getBlockTransform(pos, rotationIndex, variantRotation,
        displayOrientation, displayTransform);

    commandBuffer.run((store) -> {
      var uuid = UUID.randomUUID();
      var holder = DisplayUtils.createDisplayEntity(store, takenItemStack, rotationIndex,
          displayOrientation, blockTransform, displayKind);

      holder.putComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
      holder.putComponent(DisplayedItemComponent.getComponentType(),
          new DisplayedItemComponent(takenItemStack, pos, blockTransform.getPosition()));

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
      Vector3i pos, World world) {
    var anchoredEntity = EntityUtils.getEntity(world, anchoredEntityId);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      return false;
    }

    if (!anchoredEntity.isValid()) {
      return false;
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

  public void update(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, World world,
      BlockType blockType, int rotationIndex) {
    var anchoredEntity = EntityUtils.getEntity(world, anchoredEntityId);
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

  public void onDestroy(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, World world) {
    var anchoredEntity = EntityUtils.getEntity(world, anchoredEntityId);
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

      if (removeItemSoundEventIndex != 0) {
        SoundUtil.playSoundEvent3d(removeItemSoundEventIndex, SFX, (double) pos.x + (double) 0.5F,
            (double) pos.y + (double) 0.5F, (double) pos.z + (double) 0.5F, commandBuffer);
      }
    });
  }

  public void refresh() {
    addItemSoundEventIndex = 0;
    removeItemSoundEventIndex = 0;
    interactionHitboxIndex = 0;
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
    if (interactionHitboxType != null) {
      interactionHitboxIndex = BlockBoundingBoxes.getAssetMap().getIndex(interactionHitboxType);
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
        .append(new KeyedCodec<>("InteractionHitboxType", Codec.STRING),
            (component, interactionHitboxType) -> component.interactionHitboxType = interactionHitboxType,
            (component) -> component.interactionHitboxType)
        .addValidatorLate(() -> BlockBoundingBoxes.VALIDATOR_CACHE.getValidator().late())
        .add()
        .append(new KeyedCodec<>("InteractionHintFull", Codec.STRING),
            (component, interactionHintFull) -> component.interactionHintFull = interactionHintFull,
            (component) -> component.interactionHintFull)
        .add()
        .append(new KeyedCodec<>("InteractionHintEmpty", Codec.STRING),
            (component, interactionHintEmpty) -> component.interactionHintEmpty = interactionHintEmpty,
            (component) -> component.interactionHintEmpty)
        .add()
        .append(new KeyedCodec<>("InteractionHint", Codec.STRING),
            (component, interactionHint) -> component.interactionHint = interactionHint,
            (component) -> component.interactionHint)
        .add()
        .afterDecode(DisplaySlot::processConfig)
        .build();
  }

}
