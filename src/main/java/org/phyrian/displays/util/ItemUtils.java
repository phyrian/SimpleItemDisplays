package org.phyrian.displays.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ItemUtils {

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
    if (modelId == null) {
      return null;
    } else {
      ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
      return modelAsset != null ? Model.createScaledModel(modelAsset, item.getIconProperties().getScale()) : null;
    }
  }

  public static @Nullable String getItemModelId(@Nonnull Item item) {
    String modelId = item.getModel();
    if (modelId == null && item.hasBlockType()) {
      BlockType blockType = BlockType.getAssetMap().getAsset(item.getId());
      if (blockType != null && blockType.getCustomModel() != null) {
        modelId = blockType.getCustomModel();
      }
    }

    return modelId;
  }
}
