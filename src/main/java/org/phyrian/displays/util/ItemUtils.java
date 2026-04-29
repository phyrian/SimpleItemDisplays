package org.phyrian.displays.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.config.ItemFilter;
import org.phyrian.displays.config.ItemFilterType;

import com.hypixel.hytale.common.util.StringUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BlockTypeListAsset;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ItemUtils {

  private ItemUtils() {
  }

  public static ItemStack pickupItem(Player playerComponent, ItemStack itemStack, Vector3d position,
      Store<EntityStore> componentAccessor, Ref<EntityStore> ref) {
    var transaction = playerComponent.giveItem(itemStack, ref, componentAccessor);
    var remainder = transaction.getRemainder();

    if (ItemStack.isEmpty(remainder)) {
      playerComponent.notifyPickupItem(ref, itemStack, position, componentAccessor);
      var pickupItemHolder = ItemComponent.generatePickedUpItem(itemStack, position,
          componentAccessor, ref);
      componentAccessor.addEntity(pickupItemHolder, AddReason.SPAWN);
    } else if (!remainder.equals(itemStack)) {
      var pickedUpQuantity = itemStack.getQuantity() - remainder.getQuantity();
      var pickedUpItemStack = itemStack.withQuantity(pickedUpQuantity);
      if (pickedUpItemStack != null) {
        playerComponent.notifyPickupItem(ref, pickedUpItemStack, position, componentAccessor);
        var pickupItemHolder = ItemComponent.generatePickedUpItem(pickedUpItemStack, position,
            componentAccessor, ref);
        componentAccessor.addEntity(pickupItemHolder, AddReason.SPAWN);
      }
    }

    return remainder;
  }

  public static void spawnItem(ItemStack itemStack, Vector3d position,
      ComponentAccessor<EntityStore> store) {
    var holder = ItemComponent.generateItemDrop(store, itemStack, position, Vector3f.ZERO,
        0.0F, 0.0F, 0.0F);
    if (holder != null) {
      var itemcomponent = holder.getComponent(ItemComponent.getComponentType());
      if (itemcomponent != null) {
        itemcomponent.setPickupDelay(0.5F);
      }

      store.addEntity(holder, AddReason.SPAWN);
    }
  }

  public static @Nullable ModelAsset getItemModel(@Nonnull Item item) {
    var modelId = getItemModelId(item);
    if (modelId != null) {
      return ModelAsset.getAssetMap().getAsset(modelId);
    }

    return null;
  }

  public static Box getItemHitbox(Item item) {
    var modelId = getItemModelId(item);
    if (modelId != null) {
      var modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
      if (modelAsset != null) {
        return modelAsset.getBoundingBox();
      }
    }

    if (item.hasBlockType()) {
      var blockType = BlockType.getAssetMap().getAsset(item.getBlockId());
      if (blockType != null) {
        var hitboxTypeIndex = blockType.getHitboxTypeIndex();
        var blockBoundingBoxes = BlockBoundingBoxes.getAssetMap().getAsset(hitboxTypeIndex);
        if (blockBoundingBoxes != null) {
          return blockBoundingBoxes.get(0).getBoundingBox();
        }
      }
    }

    return null;
  }

  public static @Nullable String getItemModelId(@Nonnull Item item) {
    var itemModelId = item.getModel();
    if (itemModelId != null) {
      return itemModelId;
    }

    if (item.hasBlockType()) {
      var blockType = BlockType.getAssetMap().getAsset(item.getBlockId());
      if (blockType != null) {
        return blockType.getCustomModel();
      }
    }

    return null;
  }

  public static boolean isHandheld(Item item) {
    return item.getTool() != null || item.getWeapon() != null || item.getBuilderTool() != null
        || Optional.ofNullable(item.getCategories())
        .filter(categories -> Arrays.stream(categories)
            .anyMatch(category -> "Items.Tools".equals(category) || "Items.Weapons".equals(category)
                || "Tool.BuilderTool".equals(category)))
        .isPresent();
  }

  public static ItemStack copyItemStack(ItemStack itemStack) {
    //noinspection deprecation
    return new ItemStack(itemStack.getItemId(), itemStack.getQuantity(), itemStack.getDurability(),
        itemStack.getMaxDurability(), itemStack.getMetadata());
  }

  public static Set<String> findMatchingItemIds(ItemFilter[] itemFilters) {
    var result = new HashSet<String>();
    for (var itemFilter : itemFilters) {
      findMatchingItemIds(itemFilter.getType(), itemFilter.getValues(), result);
    }
    return result;
  }

  private static void findMatchingItemIds(ItemFilterType type,
      String[] values, Collection<String> out) {
    if (values == null) {
      return;
    }

    var items = Item.getAssetMap().getAssetMap();
    // TODO: implement the rest
    if (type == ItemFilterType.ItemId) {
      for (String glob : values) {
        var globLower = glob.toLowerCase();
        items.keySet().forEach(id -> {
          if (StringUtil.isGlobMatching(globLower, id.toLowerCase())) {
            out.add(id);
          }
        });
      }
    } else if (type == ItemFilterType.BlockType) {
      for (String key : values) {
        var blockTypeList = BlockTypeListAsset.getAssetMap().getAsset(key);
        if (blockTypeList != null) {
          out.addAll(blockTypeList.getBlockTypeKeys());
        }
      }
    }
  }
}
