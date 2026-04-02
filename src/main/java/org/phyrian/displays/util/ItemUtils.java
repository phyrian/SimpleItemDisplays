package org.phyrian.displays.util;

import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.model.ModelPlugin;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.ModelOverride;
import com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.Model.ModelReference;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent.Tool;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.system.ModelSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class ItemUtils {

  private ItemUtils() {
  }

  public static ItemStack pickupItem(Player playerComponent, ItemStack itemStack, Vector3d position, Store<EntityStore> componentAccessor, Ref<EntityStore> ref) {
    ItemStackTransaction transaction = playerComponent.giveItem(itemStack, ref, componentAccessor);
    ItemStack remainder = transaction.getRemainder();

    if (ItemStack.isEmpty(remainder)) {
      playerComponent.notifyPickupItem(ref, itemStack, position, componentAccessor);
      Holder<EntityStore> pickupItemHolder = ItemComponent.generatePickedUpItem(itemStack, position, componentAccessor, ref);
      componentAccessor.addEntity(pickupItemHolder, AddReason.SPAWN);
    } else if (!remainder.equals(itemStack)) {
      int quantity = itemStack.getQuantity() - remainder.getQuantity();
      if (quantity > 0) {
        ItemStack pickedUpItemStack = itemStack.withQuantity(quantity);
        playerComponent.notifyPickupItem(ref, itemStack, position, componentAccessor);
        Holder<EntityStore> pickupItemHolder = ItemComponent.generatePickedUpItem(pickedUpItemStack, position, componentAccessor, ref);
        componentAccessor.addEntity(pickupItemHolder, AddReason.SPAWN);
      }
    }

    return remainder;
  }

  public static void spawnItem(ComponentAccessor<EntityStore> store, ItemStack itemStack, Vector3d position) {
    Holder<EntityStore> holder = ItemComponent.generateItemDrop(store, itemStack, position, Vector3f.ZERO, 0.0F, 0.0F, 0.0F);
    if (holder != null) {
      ItemComponent itemcomponent = holder.getComponent(ItemComponent.getComponentType());
      if (itemcomponent != null) {
        itemcomponent.setPickupDelay(0.5F);
      }

      store.addEntity(holder, AddReason.SPAWN);
    }
  }

  public static @Nullable Model getItemModel(@Nonnull Item item) {
    String modelId = getItemModelId(item);
    if (modelId != null) {
      ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
      if (modelAsset != null) {
        AssetIconProperties iconProperties = item.getIconProperties();
        return Model.createScaledModel(modelAsset, iconProperties != null ? iconProperties.getScale() : 1.0F);
      }
    }

    return null;
  }

  public static Box getItemBoundingBox(Item item) {
    String modelId = getItemModelId(item);
    if (modelId != null) {
      ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
      if (modelAsset != null) {
        return modelAsset.getBoundingBox();
      }
      // TODO: find a way to load item model - above code does not find it
    }

    if (item.hasBlockType()) {
      BlockType blockType = BlockType.getAssetMap().getAsset(item.getBlockId());
      if (blockType != null) {
        BlockBoundingBoxes blockBoundingBoxes = BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex());
        if (blockBoundingBoxes != null) {
          return blockBoundingBoxes.get(0).getBoundingBox();
        }
      }
    }

    return null;
  }

  public static @Nullable String getItemModelId(@Nonnull Item item) {
    String itemModelId = item.getModel();
    if (itemModelId != null) {
      return itemModelId;
    }

    if (item.hasBlockType()) {
      BlockType blockType = BlockType.getAssetMap().getAsset(item.getBlockId());
      if (blockType != null) {
        return blockType.getCustomModel();
      }
    }

    return null;
  }
}
