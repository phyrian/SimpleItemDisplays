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
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Data;

@Data
public class DisplayContainer {

  public static final Codec<DisplayContainer> CODEC;

  private UUID anchoredEntityId;
  private DisplayTransform displayTransform;
  private DisplayOrientation displayOrientation;
  private DisplayKind displayKind;

  public DisplayContainer() {
    this(null, new DisplayTransform(), DisplayOrientation.Horizontal, DisplayKind.Default);
  }

  public DisplayContainer(UUID anchoredEntityId, DisplayTransform displayTransform,
      DisplayOrientation displayOrientation, DisplayKind displayKind) {
    this.anchoredEntityId = anchoredEntityId;
    this.displayTransform = displayTransform;
    this.displayOrientation = displayOrientation;
    this.displayKind = displayKind;
  }

  public DisplayContainer(DisplayContainer other) {
    this(other.anchoredEntityId, other.displayTransform.clone(), other.displayOrientation,
        other.displayKind);
  }

  public boolean setItem(CommandBuffer<EntityStore> commandBuffer, Vector3i pos,
      ItemStack itemStack, BlockType blockType, int rotationIndex) {
    if (anchoredEntityId != null) {
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
    });

    return true;
  }

  public boolean update(CommandBuffer<EntityStore> commandBuffer, Vector3i pos, WorldChunk chunk,
      BlockType blockType, int rotationIndex) {
    var anchoredEntity = findAnchoredEntity(pos, chunk);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      return false;
    }

    if (!anchoredEntity.isValid()) {
      return false;
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

    return true;
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

  @Override
  public DisplayContainer clone() {
    return new DisplayContainer(this);
  }

  static {
    CODEC = BuilderCodec.builder(DisplayContainer.class, DisplayContainer::new)
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
        .build();
  }

}
