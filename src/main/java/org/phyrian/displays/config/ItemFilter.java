package org.phyrian.displays.config;

import java.util.Arrays;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

import lombok.Data;

@Data
public class ItemFilter {

  public static final Codec<ItemFilter> CODEC;

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
  }
}
