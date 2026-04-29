package org.phyrian.displays.component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.config.DisplayKind;
import org.phyrian.displays.config.DisplayOrientation;
import org.phyrian.displays.config.DisplayTransform;
import org.phyrian.displays.util.DisplayUtils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.common.util.StringUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BlockTypeListAsset;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.Pair;
import lombok.Data;

@Data
public class ItemDisplayBlock implements Component<ChunkStore> {

  private static final String DEFAULT_STATE = "default";
  private static final String FULL_STATE = "Full";

  public static final BuilderCodec<ItemDisplayBlock> CODEC;
  public static ComponentType<ChunkStore, ItemDisplayBlock> TYPE;

  private UUID anchoredEntityId;
  private DisplayTransform displayTransform;
  private DisplayOrientation displayOrientation;
  private DisplayKind displayKind;
  private Set<String> allowedItems;
  private Set<String> allowedBlockTypes;
  private Set<String> itemFilter;

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
    this.itemFilter = null;
  }

  public ItemDisplayBlock(ItemDisplayBlock other) {
    this(other.anchoredEntityId, other.displayTransform, other.displayOrientation,
        other.displayKind, other.allowedItems, other.allowedBlockTypes);
  }

  public void setAllowedItems(Set<String> allowedItems) {
    if (!Objects.equals(allowedItems, this.allowedItems)) {
      this.itemFilter = null;
    }
    this.allowedItems = allowedItems;
  }

  public void setAllowedBlockTypes(Set<String> allowedBlockTypes) {
    if (!Objects.equals(allowedBlockTypes, this.allowedBlockTypes)) {
      this.itemFilter = null;
    }
    this.allowedBlockTypes = allowedBlockTypes;
  }

  public boolean canHoldItem(String itemId) {
    if (itemFilter == null) {
      loadItemFilter();
    }

    if (itemFilter.isEmpty()) {
      return true;
    }

    return itemFilter.contains(itemId);
  }

  public void addItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos, ItemStack itemStack, WorldChunk chunk, BlockType blockType,
      int rotationIndex) {
    var itemId = itemStack.getItemId();
    var item = Item.getAssetMap().getAsset(itemId);
    if (item == null) {
      return;
    }

    var variantRotation = blockType.getVariantRotation();
    var blockTransform = DisplayUtils.getBlockTransform(pos, rotationIndex, variantRotation, displayOrientation, displayTransform);

    commandBuffer.run((store) -> {
      var holder = DisplayUtils.createDisplayEntity(store, itemStack, rotationIndex, displayOrientation, blockTransform, displayKind);
      var uuid = UUID.randomUUID();
      holder.putComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
      holder.putComponent(DisplayedItemComponent.getComponentType(), new DisplayedItemComponent(itemStack, pos, blockTransform.getPosition()));
      store.addEntity(holder, AddReason.SPAWN);

      this.setAnchoredEntityId(uuid);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, FULL_STATE);
    });
  }

  public void removeItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos, WorldChunk chunk, BlockType blockType,
      int rotationIndex) {
    var anchoredEntity = findAnchoredEntity(ref, pos, chunk, blockType);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
      return;
    }

    if (!anchoredEntity.isValid()) {
      return;
    }

    commandBuffer.run((store) -> {
      var displayComponent = store.getComponent(anchoredEntity, DisplayedItemComponent.getComponentType());
      if (displayComponent != null) {
        displayComponent.dropItem(store, ref);
      }

      store.removeEntity(anchoredEntity, RemoveReason.REMOVE);
      this.setAnchoredEntityId(null);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
    });
  }

  public void updateState(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos, WorldChunk chunk, BlockType blockType, int rotationIndex) {
    var world = chunk.getWorld();

    var anchoredEntity = getAnchoredEntity(world);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
      return;
    }

    if (!anchoredEntity.isValid()) {
      return;
    }

    this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, FULL_STATE);
  }

  public void refreshDisplay(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos, WorldChunk chunk, BlockType blockType, int rotationIndex) {
    var anchoredEntity = findAnchoredEntity(ref, pos, chunk, blockType);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
      return;
    }

    if (!anchoredEntity.isValid()) {
      return;
    }

    var variantRotation = blockType.getVariantRotation();
    var blockTransform = DisplayUtils.getBlockTransform(pos, rotationIndex, variantRotation, displayOrientation, displayTransform);

    commandBuffer.run((store) -> {
      var displayComponent = store.getComponent(anchoredEntity, DisplayedItemComponent.getComponentType());
      if (displayComponent == null) {
        store.removeEntity(anchoredEntity, RemoveReason.REMOVE);
        this.setAnchoredEntityId(null);
        this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
        return;
      }

      var itemStack = displayComponent.getItemStack();
      // remove previous display entity after retrieving itemStack
      store.removeEntity(anchoredEntity, RemoveReason.REMOVE);
      this.setAnchoredEntityId(null);

      var itemId = itemStack.getItemId();
      var item = Item.getAssetMap().getAsset(itemId);
      if (item == null) {
        this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
        return;
      }

      var holder = DisplayUtils.createDisplayEntity(store, itemStack, rotationIndex, displayOrientation, blockTransform, displayKind);
      var uuid = UUID.randomUUID();
      holder.putComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
      holder.putComponent(DisplayedItemComponent.getComponentType(), new DisplayedItemComponent(itemStack, pos, blockTransform.getPosition()));
      store.addEntity(holder, AddReason.SPAWN);

      this.setAnchoredEntityId(uuid);
    });
  }

  public void onDestroy(CommandBuffer<EntityStore> commandBuffer, World world) {
    var anchoredEntity = getAnchoredEntity(world);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      return;
    }

    if (!anchoredEntity.isValid()) {
      return;
    }

    commandBuffer.run((store) -> {
      var displayComponent = store.getComponent(anchoredEntity, DisplayedItemComponent.getComponentType());
      if (displayComponent != null) {
        displayComponent.dropItem(store);
      }

      store.removeEntity(anchoredEntity, RemoveReason.REMOVE);
      this.setAnchoredEntityId(null);
    });
  }

  private Ref<EntityStore> findAnchoredEntity(Ref<EntityStore> ref, Vector3i pos, WorldChunk chunk, BlockType blockType) {
    var world = chunk.getWorld();

    var anchoredEntity = getAnchoredEntity(world);
    if (anchoredEntity != null) {
      return anchoredEntity;
    }

    var currentState = blockType.getStateForBlock(blockType);
    if (!Objects.equals(currentState, FULL_STATE)) {
      return null;
    }

    // lookup displayed entity by display position
    var entityStore = ref.getStore();
    var entityChunk = chunk.getEntityChunk();
    if (entityChunk != null) {
      var entityReferences = entityChunk.getEntityReferences();
      var componentType = DisplayedItemComponent.getComponentType();
      var optFoundEntity = entityReferences.stream()
          .map(entityRef -> Pair.of(entityRef, entityStore.getComponent(entityRef, componentType)))
          .filter(pair -> pair.value() != null)
          .filter(pair -> Objects.equals(pair.value().getDisplayPosition(), pos))
          .findFirst();
      if (optFoundEntity.isPresent()) {
        return optFoundEntity.get().key();
      }
    }

    return null;
  }

  private Ref<EntityStore> getAnchoredEntity(World world) {
    if (this.anchoredEntityId == null) {
      return null;
    }

    return world.getEntityStore().getRefFromUUID(this.anchoredEntityId);
  }

  private void changeState(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nullable Ref<EntityStore> ref, @Nonnull Vector3i pos, @Nonnull WorldChunk chunk,
      @Nonnull BlockType current, int rotation, String newState) {
    if (newState == null) {
      return;
    }

    var currentState = current.getStateForBlock(current);
    if (currentState == null) {
      currentState = DEFAULT_STATE;
    }

    if (newState.equalsIgnoreCase(currentState)) {
      return;
    }

    var newBlock = current.getBlockKeyForState(newState);
    if (newBlock == null) {
      return;
    }

    var newBlockId = BlockType.getAssetMap().getIndex(newBlock);
    if (newBlockId == Integer.MIN_VALUE) {
      return;
    }

    var newBlockType = BlockType.getAssetMap().getAsset(newBlockId);
    var settings = 262;

    if (chunk.getBlock(pos) != 0) {
      //noinspection DataFlowIssue
      chunk.setBlock(pos.getX(), pos.getY(), pos.getZ(), newBlockId, newBlockType, rotation, 0, settings);
    }

    var interactionStateBlock = current.getBlockForState(newState);
    if (interactionStateBlock == null) {
      return;
    }

    var soundEventIndex = interactionStateBlock.getInteractionSoundEventIndex();
    if (soundEventIndex == 0) {
      return;
    }

    SoundUtil.playSoundEvent3d(ref, soundEventIndex, (double) pos.x + (double) 0.5F, (double) pos.y + (double) 0.5F, (double) pos.z + (double) 0.5F, commandBuffer);
  }

  private void loadItemFilter() {
    itemFilter = new HashSet<>();

    var items = Item.getAssetMap().getAssetMap();
    if (allowedItems != null && !allowedItems.isEmpty()) {
      allowedItems.forEach(glob -> {
        var globLower = glob.toLowerCase();
        items.keySet().forEach(id -> {
          if (StringUtil.isGlobMatching(globLower, id.toLowerCase())) {
            itemFilter.add(id);
          }
        });
      });
    }

    if (allowedBlockTypes != null) {
      allowedBlockTypes.forEach(id -> {
        var blockTypeList = BlockTypeListAsset.getAssetMap().getAsset(id);
        if (blockTypeList != null) {
          itemFilter.addAll(blockTypeList.getBlockTypeKeys());
        }
      });
    }
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
