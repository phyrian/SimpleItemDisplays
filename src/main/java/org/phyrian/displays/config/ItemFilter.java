package org.phyrian.displays.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.common.util.StringUtil;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BlockTypeListAsset;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemCategory;
import com.hypixel.hytale.server.core.asset.type.item.config.ResourceType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.NONE)
public class ItemFilter {

  public static final Codec<ItemFilter> CODEC;
  public static final ValidatorCache<ItemFilter> VALIDATOR_CACHE;

  private ItemFilterType type;
  private String[] values;

  private transient Set<String> itemIds;

  private ItemFilter() {
    this(ItemFilterType.ItemId, new String[]{});
  }

  public ItemFilter(ItemFilterType type, String[] values) {
    this.type = type;
    this.values = values;
    this.processConfig();
  }

  public boolean matches(String itemId) {
    return itemIds != null && itemIds.contains(itemId);
  }

  protected void processConfig() {
    itemIds = new HashSet<>();
    if (values == null || values.length == 0) {
      return;
    }

    switch (type) {
      case ItemId -> {
        var globsLower = Arrays.stream(values).map(String::toLowerCase).toList();
        for (var itemId : Item.getAssetMap().getAssetMap().keySet()) {
          if (globsLower.stream()
              .anyMatch(globLower -> StringUtil.isGlobMatching(globLower, itemId.toLowerCase()))) {
            itemIds.add(itemId);
          }
        }
      }
      case ResourceType -> {
        var resourceTypeIds = Arrays.asList(values);
        for (var entry : Item.getAssetMap().getAssetMap().entrySet()) {
          var itemId = entry.getKey();
          var item = entry.getValue();
          if (Arrays.stream(item.getResourceTypes())
              .anyMatch(it -> resourceTypeIds.contains(it.id))) {
            itemIds.add(itemId);
          }
        }
      }
      case BlockType -> {
        for (var blockTypeId : values) {
          var blockTypeList = BlockTypeListAsset.getAssetMap().getAsset(blockTypeId);
          if (blockTypeList != null) {
            itemIds.addAll(blockTypeList.getBlockTypeKeys());
          }
        }
      }
      case ItemCategory -> {
        var itemCategoryIds = Arrays.asList(values);
        for (var entry : Item.getAssetMap().getAssetMap().entrySet()) {
          var itemId = entry.getKey();
          var item = entry.getValue();

          var categories = item.getCategories();
          var subCategory = item.getSubCategory();
          if ((categories != null && Arrays.stream(categories).anyMatch(itemCategoryIds::contains))
              || (subCategory != null && itemCategoryIds.contains(subCategory))) {
            itemIds.add(itemId);
          }
        }
      }
    }
  }

  static {
    CODEC = BuilderCodec.builder(ItemFilter.class, ItemFilter::new)
        .append(new KeyedCodec<>("Type", new EnumCodec<>(ItemFilterType.class)),
            (component, type) -> component.type = type,
            (component) -> component.type)
        .add()
        .append(new KeyedCodec<>("Value", Codec.STRING_ARRAY),
            (component, values) -> component.values = values,
            (component) -> component.values)
        .add()
        .afterDecode(ItemFilter::processConfig)
        .build();
    VALIDATOR_CACHE = new ValidatorCache<>(new ItemFilterValidator());
  }

  private static class ItemFilterValidator implements Validator<ItemFilter> {

    @Override
    public void accept(ItemFilter itemFilter, ValidationResults validationResults) {
      var values = itemFilter.values;
      if (values == null || values.length == 0) {
        return;
      }
      switch (itemFilter.type) {
        case ItemId -> {
          // no-op - this one accepts globs for pattern matching
        }
        case ResourceType -> ResourceType.VALIDATOR_CACHE.getArrayValidator()
            .accept(values, validationResults);
        case BlockType -> BlockTypeListAsset.VALIDATOR_CACHE.getArrayValidator()
            .accept(values, validationResults);
        case ItemCategory -> ItemCategory.VALIDATOR_CACHE.getArrayValidator()
            .accept(values, validationResults);
      }
    }

    @Override
    public void updateSchema(SchemaContext schemaContext, Schema schema) {
      // no-op
    }
  }
}
