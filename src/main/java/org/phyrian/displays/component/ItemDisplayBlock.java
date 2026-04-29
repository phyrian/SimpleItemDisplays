package org.phyrian.displays.component;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.phyrian.displays.config.DisplayKind;
import org.phyrian.displays.config.DisplayOrientation;
import org.phyrian.displays.config.DisplayTransform;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BlockTypeListAsset;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import lombok.Data;

@Data
public class ItemDisplayBlock implements Component<ChunkStore> {

  public static final BuilderCodec<ItemDisplayBlock> CODEC;
  public static ComponentType<ChunkStore, ItemDisplayBlock> TYPE;

  private UUID anchoredEntityId;
  private DisplayTransform displayTransform;
  private DisplayOrientation displayOrientation;
  private DisplayKind displayKind;
  private Set<String> allowedItems;
  private Set<String> allowedBlockTypes;

  public ItemDisplayBlock() {
    this(null, new DisplayTransform(), DisplayOrientation.Horizontal, DisplayKind.Default, null,
        null);
  }

  public ItemDisplayBlock(UUID anchoredEntityId, DisplayTransform displayTransform,
      DisplayOrientation displayOrientation, DisplayKind displayKind, Set<String> allowedItems,
      Set<String> allowedResourceTypes) {
    this.anchoredEntityId = anchoredEntityId;
    this.displayTransform = displayTransform;
    this.displayOrientation = displayOrientation;
    this.displayKind = displayKind;
    this.allowedItems = allowedItems;
    this.allowedBlockTypes = allowedResourceTypes;
  }

  public ItemDisplayBlock(ItemDisplayBlock other) {
    this(other.anchoredEntityId, other.displayTransform, other.displayOrientation,
        other.displayKind, other.allowedItems, other.allowedBlockTypes);
  }

  @Override
  public ItemDisplayBlock clone() {
    return new ItemDisplayBlock(this);
  }

  public static ComponentType<ChunkStore, ItemDisplayBlock> getComponentType() {
    return TYPE;
  }

  static {
    CODEC = BuilderCodec.builder(ItemDisplayBlock.class, ItemDisplayBlock::new)
        .append(new KeyedCodec<>("AnchoredEntity", Codec.UUID_BINARY),
            (component, anchoredEntity) -> component.anchoredEntityId = anchoredEntity,
            (component) -> component.anchoredEntityId)
        .add()
        .append(new KeyedCodec<>("DisplayTransform", DisplayTransform.CODEC),
            (component, displayTransform) -> component.displayTransform = Objects.requireNonNullElseGet(displayTransform, DisplayTransform::new),
            (component) -> component.displayTransform)
        .add()
        .append(new KeyedCodec<>("DisplayKind", new EnumCodec<>(DisplayKind.class)),
            (component, displayKind) -> component.displayKind = Objects.requireNonNullElse(displayKind, DisplayKind.Default),
            (component) -> component.displayKind)
        .add()
        .append(new KeyedCodec<>("DisplayOrientation", new EnumCodec<>(DisplayOrientation.class)),
            (component, displayOrientation) -> component.displayOrientation = Objects.requireNonNullElse(displayOrientation, DisplayOrientation.Horizontal),
            (component) -> component.displayOrientation)
        .add()
        .append(new KeyedCodec<>("AllowedItems", Codec.STRING_ARRAY),
            (component, allowedItems) -> component.allowedItems = allowedItems != null ? Set.of(allowedItems) : null,
            (component) -> component.allowedItems != null ? component.allowedItems.toArray(String[]::new) : null)
        .add()
        .append(new KeyedCodec<>("AllowedBlockTypes", Codec.STRING_ARRAY),
            (component, allowedBlockTypes) -> component.allowedBlockTypes = allowedBlockTypes != null ? Set.of(allowedBlockTypes) : null,
            (component) -> component.allowedBlockTypes != null ? component.allowedBlockTypes.toArray(String[]::new) : null)
        .addValidator(BlockTypeListAsset.VALIDATOR_CACHE.getArrayValidator().late())
        .add()
        .build();
  }

}
