package org.phyrian.displays.config;

import java.util.Arrays;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BlockTypeListAsset;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemCategory;
import com.hypixel.hytale.server.core.asset.type.item.config.ResourceType;

import lombok.Data;

@Data
public class ItemFilter {

  public static final Codec<ItemFilter> CODEC;
  public static final ValidatorCache<ItemFilter> VALIDATOR_CACHE;

  private ItemFilterType type;
  private String[] values;

  public ItemFilter() {
    this(ItemFilterType.ItemId, new String[]{});
  }

  public ItemFilter(ItemFilterType type, String[] values) {
    this.type = type;
    this.values = values;
  }

  public ItemFilter(ItemFilter other) {
    this(other.type, Arrays.copyOf(other.values, other.values.length));
  }

  @Override
  protected ItemFilter clone() {
    return new ItemFilter(this);
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
