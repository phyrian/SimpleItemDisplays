package org.phyrian.displays.util;

import org.phyrian.displays.config.DisplayKind;
import org.phyrian.displays.config.DisplayOrientation;
import org.phyrian.displays.config.DisplayTransform;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.Model.ModelReference;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.prefab.PrefabCopyableComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class DisplayUtils {

  private DisplayUtils() {
  }

  // I've wasted too many hours on this, and it currently does work for most blocks.
  public static DisplayTransform getDisplayTransform(Vector3i pos, Item item, int rotationIndex, DisplayOrientation orientation, float scale) {
    RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
    Box boundingBox = ItemUtils.getItemBoundingBox(item);

    Vector3d displayPosition = new Vector3d(pos.x, pos.y, pos.z);
    Vector3f displayRotation = new Vector3d(
        rotationTuple.pitch().getRadians(),
        rotationTuple.yaw().getRadians(),
        rotationTuple.roll().getRadians()
    ).toVector3f();

    boolean zAdjusted = false;
    if (boundingBox != null) {
      if (boundingBox.width() > 1.0) {
        Vector3d dv = getHorizontalAlignment(boundingBox.width(), scale);
        displayPosition.add(rotationTuple.rotatedVector(dv));
      }
      if (boundingBox.depth() > 1.0) {
        Vector3d dv = getHorizontalAlignment(boundingBox.depth(), scale);
        displayPosition.add(rotationTuple.rotatedVector(dv.negate()));
        displayRotation.addRotationOnAxis(Axis.Y, -90);
        zAdjusted = true;
      }
    }

    if (orientation == DisplayOrientation.VERTICAL) {
      double height = boundingBox != null ? boundingBox.height() : 1.0;
      Vector3d dv = getVerticalAlignment(height, scale);
      displayPosition.add(rotationTuple.rotatedVector(dv));
      if (rotationIndex % 8 == 0) {
        if (zAdjusted) {
          displayRotation.addRotationOnAxis(Axis.Z, -90);
          if (rotationIndex == 0) {
            displayRotation.addRotationOnAxis(Axis.Y, 180);
          }
        } else {
          displayRotation.addRotationOnAxis(Axis.X, 90);
          displayRotation.addRotationOnAxis(Axis.Y, 180);
        }
      } else {
        displayRotation.addRotationOnAxis(Axis.X, -90);
        displayRotation.addRotationOnAxis(Axis.Y, 180);
      }
    }

    return new DisplayTransform(displayPosition, displayRotation);
  }

  /**
   * Calculate the adjustment needed for the display entity the entity occupies more than a single block.
   * Note: this method only works for multiblock blocks where the placement starts from the front-left corner (e.g.: workbenches, tables, shallow roofs).
   * @param scale of the displayed entity
   * @return the correction vector
   */
  private static Vector3d getHorizontalAlignment(double length, float scale/*, int rotationIndex*/) {
    double d = (length / 2.0d) * (scale * 0.25d);
    return new Vector3d(d, 0, 0);
  }

  /**
   * Calculate the adjustment needed for the display entity when using vertical orientation to counter-act the center pivot rotation.
   * @param scale of the displayed entity
   * @return the correction vector
   */
  public static Vector3d getVerticalAlignment(double height, float scale) {
    double cy = -0.425d;
    double cz = 0.5 - (scale * 0.25d); // scaled diff from center on Z
    double dz = height > 1 ? (height / 2.0d) * (scale * 0.25d) : 0d;
    return new Vector3d(0, cy, -cz + dz);
  }

  public static Holder<EntityStore> createDisplayEntity(Store<EntityStore> store, ItemStack itemStack, DisplayTransform transform, DisplayKind displayKind) {
    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
    holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(transform.getPosition(), transform.getRotation()));

    ItemStack displayStack = ItemUtils.copyItemStack(itemStack);
    Item displayedItem = displayStack.getItem();
    float displayScale = transform.getScale();
    applyVisual(holder, displayedItem, displayScale, displayKind);

    displayStack.setOverrideDroppedItemAnimation(true);
    holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(displayStack));
    holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
    holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);

    holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(transform.getRotation()));
    holder.addComponent(PropComponent.getComponentType(), PropComponent.get());

    Interactions interactions = new Interactions();
    interactions.setInteractionId(InteractionType.Use, "SimpleItemDisplays_Remove_Displayed_Item");
    interactions.setInteractionHint("server.interactionHints.pickup");
    holder.addComponent(Interactions.getComponentType(), interactions);
    holder.ensureComponent(Interactable.getComponentType());

    holder.ensureComponent(UUIDComponent.getComponentType());
    holder.ensureComponent(PrefabCopyableComponent.getComponentType());
    return holder;
  }

  private static void applyVisual(Holder<EntityStore> holder, Item item, float scale, DisplayKind displayKind) {
    Model model = ItemUtils.getItemModel(item);
    if (displayKind == DisplayKind.MODEL && model == null) {
      LOGGER.atWarning().log("Tried applying model for item with no model definition: " + item.getId());
    }

    if (model != null && displayKind != DisplayKind.BLOCK && displayKind != DisplayKind.ITEM) {
      holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
      holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(new ModelReference(model.getModelAssetId(), scale, null, true)));
      return;
    }

    if (item.hasBlockType() && displayKind != DisplayKind.ITEM) {
      holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(item.getId()));
    }
    holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale));
  }
}
