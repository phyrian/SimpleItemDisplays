package org.phyrian.displays.util;

import java.util.Objects;

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
    var rotationTuple = RotationTuple.get(rotationIndex);

    var displayPosition = new Vector3d(pos.x, pos.y, pos.z);
    var displayRotation = new Vector3d(
        rotationTuple.pitch().getRadians(),
        rotationTuple.yaw().getRadians(),
        rotationTuple.roll().getRadians()
    ).toVector3f();

    centerDisplayedItem(item, rotationTuple, orientation, scale, displayPosition, displayRotation);

    return new DisplayTransform(displayPosition, displayRotation);
  }

  /**
   * Calculate the adjustment needed for the display entity if the entity occupies more than a single block. Note: this method only works for multiblock blocks where the placement
   * starts from the front-left corner (e.g.: workbenches, tables, shallow roofs).
   *
   * @param scale of the displayed entity
   */
  private static void centerDisplayedItem(Item item, RotationTuple rotationTuple, DisplayOrientation orientation, float scale, Vector3d outPosition, Vector3f outRotation) {
    var boundingBox = Objects.requireNonNullElse(ItemUtils.getItemBoundingBox(item), Box.UNIT);

    LOGGER.atFine().log("""
            Aligning item: %s
             - box: %s
             - rotationTuple: %s
             - orientation: %s
             - inPosition: %s
             - inRotation: %s""",
        item.getId(), boundingBox.toString(), rotationTuple.toString(), orientation.toString(), outPosition.toString(), outRotation.toString());

    if (boundingBox.width() > 1.0) {
      var dv = getHorizontalAlignment(boundingBox.min.x, boundingBox.max.x, scale);
      outPosition.add(rotationTuple.rotatedVector(dv));
    }

    var rotated = false;
    if (boundingBox.depth() > boundingBox.width()) {
      var dv = getHorizontalAlignment(boundingBox.min.z, boundingBox.max.z, scale);
      outPosition.add(rotationTuple.rotatedVector(dv.negate()));
      outRotation.addRotationOnAxis(Axis.Y, -90);
      rotated = true;
    }

    if (orientation == DisplayOrientation.VERTICAL) {
      var dv = getVerticalAlignment(boundingBox.min.y, boundingBox.max.y, scale);
      outPosition.add(rotationTuple.rotatedVector(dv));
      if (rotationTuple.index() % 8 == 0) {
        if (rotated) {
          outRotation.addRotationOnAxis(Axis.Z, -90);
          if (rotationTuple.index() == 0) {
            outRotation.addRotationOnAxis(Axis.Y, 180);
          }
        } else {
          outRotation.addRotationOnAxis(Axis.X, 90);
          outRotation.addRotationOnAxis(Axis.Y, 180);
        }
      } else {
        outRotation.addRotationOnAxis(Axis.X, -90);
        outRotation.addRotationOnAxis(Axis.Y, 180);
      }
    }

    LOGGER.atFine().log("""
        Aligned item: %s
         - outPosition: %s
         - outRotation: %s""", item.getId(), outPosition.toString(), outRotation.toString());
  }

  private static Vector3d getHorizontalAlignment(double min, double max, float scale) {
    var length = max - min;
    var d = (length / 2.0d) * (scale * 0.25d);
    return new Vector3d(d, 0, 0);
  }

  /**
   * Calculate the adjustment needed for the display entity when using vertical orientation to counter-act the center pivot rotation.
   *
   * @param scale of the displayed entity
   * @return the correction vector
   */
  public static Vector3d getVerticalAlignment(double min, double max, float scale) {
    var height = max - min;
    var cy = -0.425d;
    var cz = 0.5 - (scale * 0.25d); // scaled diff from center on Z
    var dz = height > 1 ? (height / 2.0d) * (scale * 0.25d) : 0d;
    return new Vector3d(0, cy, -cz + dz);
  }

  public static Holder<EntityStore> createDisplayEntity(Store<EntityStore> store, ItemStack itemStack, DisplayTransform transform, DisplayKind displayKind) {
    var holder = EntityStore.REGISTRY.newHolder();
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
    holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(transform.getPosition(), transform.getRotation()));

    var displayStack = ItemUtils.copyItemStack(itemStack);
    var displayedItem = displayStack.getItem();
    var displayScale = transform.getScale();
    applyVisual(holder, displayedItem, displayScale, displayKind);

    displayStack.setOverrideDroppedItemAnimation(true);
    holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(displayStack));
    holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
    holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);

    holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(transform.getRotation()));
    holder.addComponent(PropComponent.getComponentType(), PropComponent.get());

    var interactions = new Interactions();
    interactions.setInteractionId(InteractionType.Use, "SimpleItemDisplays_Remove_Displayed_Item");
    interactions.setInteractionHint("server.interactionHints.pickup");
    holder.addComponent(Interactions.getComponentType(), interactions);
    holder.ensureComponent(Interactable.getComponentType());

    holder.ensureComponent(UUIDComponent.getComponentType());
    holder.ensureComponent(PrefabCopyableComponent.getComponentType());
    return holder;
  }

  private static void applyVisual(Holder<EntityStore> holder, Item item, float scale, DisplayKind displayKind) {
    var model = ItemUtils.getItemModel(item);
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
