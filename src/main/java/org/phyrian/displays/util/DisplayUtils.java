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
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.VariantRotation;
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

  public static final float DEFAULT_SCALE = 1.0F;

  private DisplayUtils() {
  }

  // I've wasted too many hours on this, and it currently does work for most blocks.
  public static DisplayTransform getBlockTransform(Vector3i pos, int rotationIndex,
      VariantRotation variantRotation, DisplayOrientation displayOrientation,
      DisplayTransform displayTransform) {
    var rotationTuple = RotationTuple.get(rotationIndex);

    var blockPosition = new Vector3d(pos.x, pos.y, pos.z);
    var blockRotation = new Vector3d(
        rotationTuple.pitch().getRadians(),
        rotationTuple.yaw().getRadians(),
        rotationTuple.roll().getRadians()
    ).toVector3f();

    var blockTransform = new DisplayTransform(blockPosition, blockRotation);
    if (displayTransform != null) {
      var displayPosition = displayTransform.getPosition().clone();
      rotationTuple.applyRotationTo(displayPosition);

      if (variantRotation == VariantRotation.DoublePipe && displayOrientation == DisplayOrientation.Vertical) {
        if (rotationIndex == RotationTuple.index(Rotation.None, Rotation.Ninety, Rotation.None)) {
          displayPosition.y += 0.5;
          displayPosition.z += 0.5;
        } else if (rotationIndex == RotationTuple.index(Rotation.Ninety, Rotation.Ninety, Rotation.None)) {
          displayPosition.x += 0.5;
          displayPosition.y += 0.5;
          displayPosition.z += 1;
        } else if (rotationIndex == RotationTuple.index(Rotation.OneEighty, Rotation.Ninety, Rotation.None)) {
          displayPosition.x += 1;
          displayPosition.y += 0.5;
          displayPosition.z += 0.5;
        } else if (rotationIndex == RotationTuple.index(Rotation.TwoSeventy, Rotation.Ninety, Rotation.None)) {
          displayPosition.x += 0.5;
          displayPosition.y += 0.5;
        } else if (rotationIndex == RotationTuple.index(Rotation.None, Rotation.OneEighty, Rotation.None)) {
          displayPosition.z += 1;
        }
      } else if (variantRotation == VariantRotation.NESW) {
        if (rotationIndex == RotationTuple.index(Rotation.Ninety, Rotation.None, Rotation.None)) {
          displayPosition.z += 1;
        } else if (rotationIndex == RotationTuple.index(Rotation.OneEighty, Rotation.None, Rotation.None)) {
          displayPosition.x += 1;
          displayPosition.z += 1;
        } else if (rotationIndex == RotationTuple.index(Rotation.TwoSeventy, Rotation.None, Rotation.None)) {
          displayPosition.x += 1;
        }
      }

      blockTransform.addPosition(displayPosition);
      blockTransform.addRotation(displayTransform.getRotation());
      blockTransform.scale(displayTransform.getScale());
    }

    return blockTransform;
  }

  public static Holder<EntityStore> createDisplayEntity(Store<EntityStore> store,
      ItemStack itemStack, int rotationIndex, DisplayOrientation displayOrientation,
      DisplayTransform transform, DisplayKind displayKind) {
    var holder = EntityStore.REGISTRY.newHolder();
    var displayStack = ItemUtils.copyItemStack(itemStack);
    var displayedItem = displayStack.getItem();
    var displayScale = transform.getScale();

    var visualAlignmentTransform = applyVisual(holder, displayedItem, rotationIndex,
        displayOrientation, displayScale, displayKind);
    transform.add(visualAlignmentTransform);

    holder.addComponent(NetworkId.getComponentType(),
        new NetworkId(store.getExternalData().takeNextNetworkId()));
    holder.addComponent(TransformComponent.getComponentType(),
        new TransformComponent(transform.getPosition(), transform.getRotation()));

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

  private static DisplayTransform applyVisual(Holder<EntityStore> holder, Item item,
      int rotationIndex, DisplayOrientation orientation, float scale, DisplayKind displayKind) {
    if (displayKind != DisplayKind.Block && displayKind != DisplayKind.Item) {
      var modelAsset = ItemUtils.getItemModel(item);
      if (modelAsset != null) {
        holder.addComponent(ModelComponent.getComponentType(),
            new ModelComponent(Model.createScaledModel(modelAsset, scale)));
        holder.addComponent(PersistentModel.getComponentType(),
            new PersistentModel(new ModelReference(modelAsset.getId(), scale, null, true)));
        return centerDisplayedBlock(item, rotationIndex, orientation, scale);
      }
      if (displayKind == DisplayKind.Model) {
        LOGGER.atWarning().log("Tried applying model display for Item with missing ModelAsset: "
            + item.getId());
      }
    }

    // not supported
    holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale));
    if (displayKind != DisplayKind.Item) {
      if (item.hasBlockType()) {
        holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(item.getId()));
        return centerDisplayedBlock(item, rotationIndex, orientation, scale);
      }
      if (displayKind == DisplayKind.Block) {
        LOGGER.atWarning().log("Tried applying block display for Item with missing BlockType: "
            + item.getId());
      }
    }

    return centerDisplayedItem(item, rotationIndex, orientation, scale);
  }

  /**
   * Calculate the adjustment needed for the display entity if the entity is a block.
   * Note: this method only works for multiblock blocks where the placement starts from the front-left corner (e.g.: workbenches, tables, shallow roofs).
   */
  private static DisplayTransform centerDisplayedBlock(Item item, int rotationIndex,
      DisplayOrientation orientation, float scale) {
    var translation = new Vector3d();
    var rotation = new Vector3f();

    var hitbox = Objects.requireNonNullElse(ItemUtils.getItemHitbox(item), Box.UNIT);
    var rotationTuple = RotationTuple.get(rotationIndex);

    LOGGER.atFine().log("""
        Aligning block: %s
         - rotationTuple: %s
         - orientation: %s
         - scale: %s
         - hitbox: %s""",
        item.getId(), rotationTuple.toString(), orientation.toString(), (double) scale + "",
        hitbox.toString());

    if (hitbox.width() > 1.0) {
      var dx = getHorizontalAlignment(hitbox.width(), scale);
      translation.add(rotationTuple.rotatedVector(new Vector3d(dx, 0, 0)));
    }

    var rotated = false;
    if (hitbox.depth() > hitbox.width()) {
      var dx = getHorizontalAlignment(hitbox.depth(), scale);
      translation.add(rotationTuple.rotatedVector(new Vector3d(-dx, 0, 0)));
      rotation.addRotationOnAxis(Axis.Y, -90);
      rotated = true;
    }

    // TODO: the non-vertical orientation needs adjustments for multi-blocks
    if (orientation == DisplayOrientation.Vertical) {
      var dz = getVerticalAlignment(hitbox.height(), scale);
      translation.add(rotationTuple.rotatedVector(new Vector3d(0, -0.5, dz)));
      if (rotationIndex % 8 == 0) {
        if (rotated) {
          rotation.addRotationOnAxis(Axis.Z, -90);
          if (rotationIndex == 0) {
            rotation.addRotationOnAxis(Axis.Y, 180);
          }
        } else {
          rotation.addRotationOnAxis(Axis.X, 90);
          rotation.addRotationOnAxis(Axis.Y, 180);
        }
      } else {
        rotation.addRotationOnAxis(Axis.X, -90);
        rotation.addRotationOnAxis(Axis.Y, 180);
      }
    }

    LOGGER.atFine().log("""
        Aligned block: %s
         - outPosition: %s
         - outRotation: %s""",
        item.getId(), translation.toString(), rotation.toString());

    return new DisplayTransform(translation, rotation);
  }

  /**
   * Calculate the adjustment needed for the display entity if the entity is an item.
   */
  private static DisplayTransform centerDisplayedItem(Item item, int rotationIndex,
      DisplayOrientation orientation, float scale) {
    var translation = new Vector3d();
    var rotation = new Vector3f();
    scale *= item.getScale();

    var rotationTuple = RotationTuple.get(rotationIndex);
    var playerAnimationsId = item.getPlayerAnimationsId();

    LOGGER.atFine().log("""
        Aligning item: %s
         - rotationTuple: %s
         - orientation: %s
         - scale: %s""",
        item.getId(), rotationTuple.toString(), orientation.toString(), (double) scale + "");

    if (orientation == DisplayOrientation.Vertical) {
      if ("Block".equals(playerAnimationsId)) {
        // items held as blocks: mostly armor pieces
        rotation.addRotationOnAxis(Axis.Y, 180);
        if (rotationIndex % 8 == 0) {
          rotation.addRotationOnAxis(Axis.X, 90);
        } else {
          rotation.addRotationOnAxis(Axis.X, -90);
        }

      } else if (item.getGlider() != null) {
        var dx = (double) scale * 0.25;
        translation.add(rotationTuple.rotatedVector(new Vector3d(-dx, 0, 0)));

        rotation.addRotationOnAxis(Axis.Y, 180);
        if (rotationIndex % 8 == 0) {
          rotation.addRotationOnAxis(Axis.Z, -90);
        } else {
          rotation.addRotationOnAxis(Axis.Y, -90);
          rotation.addRotationOnAxis(Axis.X, 180);
        }

      } else if (ItemUtils.isHandheld(item)) {
        var standing = false;
        var mirror45 = false;
        var flipX = false;
        var centered = false;
        var toolSize = 0.75d;
        if (playerAnimationsId != null) {
          if (playerAnimationsId.equals("Spear")) {
            toolSize = 2.0;
          } else if (playerAnimationsId.equals("Longsword") || playerAnimationsId.equals("Staff")) {
            toolSize = 1.25;
          } else if (playerAnimationsId.startsWith("Rifle")) {
            centered = true;
            mirror45 = true;
          } else if (playerAnimationsId.equals("Crossbow")) {
            standing = true;
            mirror45 = true;
            toolSize = 0.25;
          } else if (playerAnimationsId.equals("Daggers") || playerAnimationsId.equals("Shears")) {
            toolSize = 0.5;
          } else if (playerAnimationsId.equals("Sword") || playerAnimationsId.equals("Shovel")) {
            toolSize = 1.0;
          } else if (playerAnimationsId.equals("Shield")) {
            flipX = true;
            toolSize = 1.0;
          }
        }

        if (centered) {
          translation.add(rotationTuple.rotatedVector(new Vector3d(0, 0, scale / 2)));
        } else {
          var dxz = ((double) scale * toolSize) / 2;
          translation.add(rotationTuple.rotatedVector(new Vector3d(-dxz, 0, dxz)));
        }

        rotation.addRotationOnAxis(Axis.Y, -90);
        if (rotationIndex == 0) {
          if (!standing) {
            rotation.addRotationOnAxis(Axis.Z, 90);
          }

          if (mirror45) {
            rotation.addRotationOnAxis(Axis.Y, 45);
          } else {
            rotation.addRotationOnAxis(Axis.Y, -45);
          }

          if (flipX) {
            rotation.addRotationOnAxis(Axis.X, 180);
          }
        } else if (rotationIndex == 8) {
          if (standing) {
            rotation.addRotationOnAxis(Axis.Z, 180);
          } else {
            rotation.addRotationOnAxis(Axis.Z, -90);
          }

          if (mirror45) {
            rotation.addRotationOnAxis(Axis.Y, -45);
          } else {
            rotation.addRotationOnAxis(Axis.Y, 45);
          }

          if (!flipX) {
            rotation.addRotationOnAxis(Axis.X, 180);
          }
        } else {
          if (standing) {
            rotation.addRotationOnAxis(Axis.Z, -90);
          }

          if (mirror45) {
            rotation.addRotationOnAxis(Axis.X, 45);
          } else {
            rotation.addRotationOnAxis(Axis.X, -45);
          }

          if (flipX) {
            rotation.addRotationOnAxis(Axis.Y, 180);
          } else {
            rotation.addRotationOnAxis(Axis.X, -90);
          }
        }
      } else {
        rotation.addRotationOnAxis(Axis.X, 180);
        rotation.addRotationOnAxis(Axis.Z, 180);
      }

      // item models are centered on the middle point of the bottom face of their hitbox,
      // so they need to be pushed to the middle
      if (rotationIndex == 8) {
        translation.y += 1d;
      } else if (rotationIndex != 0) {
        translation.add(rotationTuple.rotatedVector(new Vector3d(0, -0.5, -0.5)));
      }
    }

    LOGGER.atFine().log("""
        Aligned item: %s
         - outPosition: %s
         - outRotation: %s""",
        item.getId(), translation.toString(), rotation.toString());

    return new DisplayTransform(translation, rotation, scale);
  }

  private static double getHorizontalAlignment(double length, float scale) {
    return (length / 2.0d) * (scale * 0.25d);
  }

  /**
   * Calculate the adjustment needed for the display entity when using vertical orientation to counter-act the center pivot rotation.
   *
   * @param scale of the displayed entity
   * @return the correction vector
   */
  public static double getVerticalAlignment(double height, float scale) {
    var cz = 0.5 - (scale * 0.25d); // scaled diff from center on Z
    if (height > 1) {
      var dz = (height / 2.0d) * (scale * 0.25d);
      return -cz + dz;
    } else {
      return -cz;
    }
  }
}
