package org.phyrian.displays.component;

import org.phyrian.displays.util.ItemUtils;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Data;

@Data
public class DisplayedItemComponent implements Component<EntityStore> {
  public static final BuilderCodec<DisplayedItemComponent> CODEC;

  public static ComponentType<EntityStore, DisplayedItemComponent> TYPE;

  private ItemStack itemStack;
  private Vector3i displayPosition;
  private Vector3d dropPosition;

  public DisplayedItemComponent() {
    this(null, null, null);
  }

  public DisplayedItemComponent(ItemStack itemStack, Vector3i displayPosition,
      Vector3d dropPosition) {
    this.itemStack = itemStack;
    this.displayPosition = displayPosition;
    this.dropPosition = dropPosition;
  }

  public DisplayedItemComponent(DisplayedItemComponent other) {
    this(other.itemStack, other.displayPosition, other.dropPosition);
  }

  public void dropItem(Store<EntityStore> store) {
    if (itemStack != null && !ItemStack.isEmpty(itemStack)) {
      ItemUtils.spawnItem(itemStack, dropPosition, store);
    }
  }

  public void dropItem(Store<EntityStore> store, Ref<EntityStore> ref) {
    if (itemStack != null && !ItemStack.isEmpty(itemStack)) {
      var playerComponent = store.getComponent(ref, Player.getComponentType());
      if (playerComponent != null) {
        itemStack = ItemUtils.pickupItem(playerComponent, itemStack, dropPosition, store, ref);
      }

      if (!ItemStack.isEmpty(itemStack)) {
        ItemUtils.spawnItem(itemStack, dropPosition, store);
      }
    }
  }

  public static ComponentType<EntityStore, DisplayedItemComponent> getComponentType() {
    return TYPE;
  }

  @Override
  public DisplayedItemComponent clone() {
    return new DisplayedItemComponent(this);
  }

  static {
    CODEC = BuilderCodec.builder(DisplayedItemComponent.class, DisplayedItemComponent::new)
        .append(new KeyedCodec<>("ItemStack", ItemStack.CODEC),
            (component, itemStack) -> component.itemStack = itemStack,
            (component) -> component.itemStack
        )
        .add()
        .append(new KeyedCodec<>("DisplayPosition", Vector3i.CODEC),
            (component, displayPosition) -> component.displayPosition = displayPosition,
            (component) -> component.displayPosition
        )
        .add()
        .append(new KeyedCodec<>("DropPosition", Vector3d.CODEC),
            (component, dropPosition) -> component.dropPosition = dropPosition,
            (component) -> component.dropPosition
        )
        .add()
        .build();
  }
}
