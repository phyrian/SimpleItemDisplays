package org.phyrian.displays.component;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.display.DisplayKind;
import org.phyrian.displays.display.DisplayTransform;
import org.phyrian.displays.display.DisplayVisual;
import org.phyrian.displays.util.ItemUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.common.util.StringUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BlockTypeListAsset;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ItemDisplayBlock implements Component<ChunkStore> {

  private static final String DEFAULT_STATE = "default";
  private static final String FULL_STATE = "Full";

  public static final BuilderCodec<ItemDisplayBlock> CODEC;
  public static ComponentType<ChunkStore, ItemDisplayBlock> TYPE;

  private UUID anchoredEntityId;
  private Vector3d displayTransform;
  private Float displayScale;
  private Set<String> allowedItems;
  private Set<String> allowedBlockTypes;
  private Set<String> itemFilter;

  public ItemDisplayBlock() {
    this(null, null, null, null, null);
  }

  public ItemDisplayBlock(UUID attachedEntity, Vector3d displayTransform, Float displayScale, Set<String> allowedItems, Set<String> allowedResourceTypes) {
    this.anchoredEntityId = attachedEntity;
    this.displayTransform = displayTransform;
    this.displayScale = displayScale;
    this.allowedItems = allowedItems;
    this.allowedBlockTypes = allowedResourceTypes;
    this.itemFilter = null;
  }

  public ItemDisplayBlock(ItemDisplayBlock other) {
    this(other.anchoredEntityId, other.displayTransform, other.displayScale, other.allowedItems, other.allowedBlockTypes);
  }

  public UUID getAnchoredEntityId() {
    return this.anchoredEntityId;
  }

  public void setAnchoredEntityId(UUID attachedEntity) {
    this.anchoredEntityId = attachedEntity;
  }

  public Vector3d getDisplayTransform() {
    return displayTransform;
  }

  public void setDisplayTransform(Vector3d displayTransform) {
    this.displayTransform = displayTransform;
  }

  public Float getDisplayScale() {
    return displayScale;
  }

  public void setDisplayScale(Float displayScale) {
    this.displayScale = displayScale;
  }

  public Set<String> getAllowedItems() {
    return allowedItems;
  }

  public void setAllowedItems(Set<String> allowedItems) {
    if (!Objects.equals(allowedItems, this.allowedItems)) {
      this.itemFilter = null;
    }
    this.allowedItems = allowedItems;
  }

  public Set<String> getAllowedBlockTypes() {
    return allowedBlockTypes;
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
    String itemId = itemStack.getItemId();
    Item item = Item.getAssetMap().getAsset(itemId);
    if (item == null) {
      return;
    }

    DisplayTransform transform = this.computeTransform(pos, blockType, rotationIndex);
    DisplayVisual visual = this.resolveVisual(item);
    commandBuffer.run((store) -> {
      Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
      this.buildEntity(holder, store, itemStack, transform, visual);
      UUID uuid = UUID.randomUUID();
      holder.putComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
      holder.putComponent(DisplayedItemComponent.getComponentType(), new DisplayedItemComponent(itemStack, pos, transform.position()));
      store.addEntity(holder, AddReason.SPAWN);
      this.setAnchoredEntityId(uuid);
      chunk.getWorld().performBlockUpdate(pos.x, pos.y, pos.z);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, FULL_STATE);
    });
  }

  public void removeItem(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos, WorldChunk chunk, BlockType blockType,
      int rotationIndex) {
    if (this.anchoredEntityId == null) {
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
      return;
    }

    Ref<EntityStore> anchoredEntity = chunk.getWorld().getEntityStore().getRefFromUUID(this.anchoredEntityId);
    if (anchoredEntity == null) {
      this.setAnchoredEntityId(null);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
      return;
    }

    commandBuffer.run((store) -> {
      DisplayedItemComponent displayComponent = store.getComponent(anchoredEntity, DisplayedItemComponent.getComponentType());
      if (displayComponent != null) {
        ItemStack itemStack = displayComponent.getItemStack();
        if (itemStack != null && !ItemStack.isEmpty(itemStack)) {
          Vector3d displayPos = getDisplayPosition(pos, blockType, rotationIndex);

          Player playerComponent = ref != null ? store.getComponent(ref, Player.getComponentType()) : null;
          if (playerComponent != null) {
            itemStack = ItemUtils.pickupItem(playerComponent, itemStack, displayPos, store, ref);
          }

          if (!ItemStack.isEmpty(itemStack)) {
            ItemUtils.spawnItem(store, itemStack, displayPos);
          }
        }
      }

      store.removeEntity(anchoredEntity, RemoveReason.REMOVE);
      this.setAnchoredEntityId(null);
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
    });
  }

  public void updateState(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, Vector3i pos, WorldChunk chunk, BlockType blockType, int rotationIndex) {
    if (this.anchoredEntityId == null) {
      this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
    } else {
      Ref<EntityStore> anchoredEntity = chunk.getWorld().getEntityStore().getRefFromUUID(this.anchoredEntityId);
      if (anchoredEntity == null) {
        this.setAnchoredEntityId(null);
        this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, DEFAULT_STATE);
      } else {
        this.changeState(commandBuffer, ref, pos, chunk, blockType, rotationIndex, FULL_STATE);
      }
    }
  }

  private void loadItemFilter() {
    itemFilter = new HashSet<>();

    Map<String, Item> items = Item.getAssetMap().getAssetMap();
    if (allowedItems != null && !allowedItems.isEmpty()) {
      allowedItems.forEach(glob -> {
        String globLower = glob.toLowerCase();
        items.keySet().forEach(id -> {
          if (StringUtil.isGlobMatching(globLower, id.toLowerCase())) {
            itemFilter.add(id);
          }
        });
      });
    }

    if (allowedBlockTypes != null) {
      allowedBlockTypes.forEach(id -> {
        BlockTypeListAsset blockTypeList = BlockTypeListAsset.getAssetMap().getAsset(id);
        if (blockTypeList != null) {
          itemFilter.addAll(blockTypeList.getBlockTypeKeys());
        }
      });
    }
  }

  private DisplayTransform computeTransform(Vector3i pos, BlockType blockType, int rotationIndex) {
    Vector3d displayPos = getDisplayPosition(pos, blockType, rotationIndex);
    Vector3f rotation = getDisplayRotation(rotationIndex);
    return new DisplayTransform(displayPos, rotation);
  }

  private Vector3d getDisplayPosition(Vector3i pos, BlockType blockType, int rotationIndex) {
    Vector3d blockCenter = new Vector3d();
    blockType.getBlockCenter(rotationIndex, blockCenter);
    Vector3d worldPos = new Vector3d(pos.x, pos.y, pos.z).add(blockCenter);
    if (displayTransform != null) {
      return worldPos.add(displayTransform);
    }
    return worldPos;
  }

  private Vector3f getDisplayRotation(int rotationIndex) {
    RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
    return new Vector3d(
        rotationTuple.pitch().getRadians(),
        rotationTuple.yaw().getRadians(),
        rotationTuple.roll().getRadians()
    ).toVector3f();
  }

  private DisplayVisual resolveVisual(@Nonnull Item item) {
    Model model = ItemUtils.getItemModel(item);
    DisplayKind displayKind = model != null ? DisplayKind.MODEL : (item.hasBlockType() ? DisplayKind.BLOCK : DisplayKind.ITEM);
    return new DisplayVisual(model, displayScale != null ? displayScale : 1.0F, displayKind);
  }

  private void buildEntity(Holder<EntityStore> holder, Store<EntityStore> store, ItemStack itemStack, DisplayTransform transform, DisplayVisual visual) {
    holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
    holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(transform.position(), transform.rotation()));
    holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
    holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
    holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(transform.rotation()));
    holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
    Interactions interactions = new Interactions();
    interactions.setInteractionId(InteractionType.Use, "SimpleItemDisplays_Remove_Displayed_Item");
    interactions.setInteractionHint("server.interactionHints.pickup");
    holder.addComponent(Interactions.getComponentType(), interactions);
    holder.ensureComponent(UUIDComponent.getComponentType());
    holder.ensureComponent(Interactable.getComponentType());
    holder.ensureComponent(PrefabCopyableComponent.getComponentType());
    this.applyVisual(holder, itemStack, visual);
  }

  private void applyVisual(Holder<EntityStore> holder, ItemStack itemStack, DisplayVisual visual) {
    ItemStack displayStack = new ItemStack(itemStack.getItemId(), 1);
    displayStack.setOverrideDroppedItemAnimation(true);
    switch (visual.kind()) {
      case MODEL:
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(visual.model()));
        Item item = Item.getAssetMap().getAsset(itemStack.getItemId());
        if (item == null) {
          return;
        }
        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(new Model.ModelReference(ItemUtils.getItemModelId(item), visual.scale(), null, true)));
        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(displayStack));
        break;
      case BLOCK:
        holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(itemStack.getItemId()));
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(visual.scale()));
        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(displayStack));
        break;
      case ITEM:
        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(displayStack));
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(visual.scale()));
    }
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

    String newBlock = current.getBlockKeyForState(newState);
    if (newBlock == null) {
      return;
    }

    int newBlockId = BlockType.getAssetMap().getIndex(newBlock);
    if (newBlockId == Integer.MIN_VALUE) {
      return;
    }

    BlockType newBlockType = BlockType.getAssetMap().getAsset(newBlockId);
    int settings = 262;

    if (chunk.getBlock(pos) != 0) {
      chunk.setBlock(pos.getX(), pos.getY(), pos.getZ(), newBlockId, newBlockType, rotation, 0, settings);
    }

    BlockType interactionStateBlock = current.getBlockForState(newState);
    if (interactionStateBlock == null) {
      return;
    }

    int soundEventIndex = interactionStateBlock.getInteractionSoundEventIndex();
    if (soundEventIndex == 0) {
      return;
    }

    SoundUtil.playSoundEvent3d(ref, soundEventIndex, (double) pos.x + (double) 0.5F, (double) pos.y + (double) 0.5F, (double) pos.z + (double) 0.5F, commandBuffer);
  }

  public static ComponentType<ChunkStore, ItemDisplayBlock> getComponentType() {
    return TYPE;
  }

  public Component<ChunkStore> clone() {
    return new ItemDisplayBlock(this);
  }

  static {
    Builder<ItemDisplayBlock> builder = BuilderCodec.builder(ItemDisplayBlock.class,
        ItemDisplayBlock::new);
    builder
        .append(new KeyedCodec<>("AnchoredEntity", Codec.UUID_BINARY),
            (component, anchoredEntity) -> component.anchoredEntityId = anchoredEntity,
            (component) -> component.anchoredEntityId)
        .add();
    builder
        .append(new KeyedCodec<>("DisplayTransform", Vector3d.CODEC),
            (component, displayTransform) -> component.displayTransform = displayTransform,
            (component) -> component.displayTransform)
        .add();
    builder
        .append(new KeyedCodec<>("DisplayScale", Codec.FLOAT),
            (component, displayScale) -> component.displayScale = displayScale,
            (component) -> component.displayScale)
        .add();
    builder
        .append(new KeyedCodec<>("AllowedItems", Codec.STRING_ARRAY),
            (component, allowedItems) -> component.allowedItems = allowedItems != null ? Set.of(allowedItems) : null,
            (component) -> component.allowedItems != null ? component.allowedItems.toArray(String[]::new) : null)
        .add();
    builder
        .append(new KeyedCodec<>("AllowedBlockTypes", Codec.STRING_ARRAY),
            (component, allowedBlockTypes) -> component.allowedBlockTypes = allowedBlockTypes != null ? Set.of(allowedBlockTypes) : null,
            (component) -> component.allowedBlockTypes != null ? component.allowedBlockTypes.toArray(String[]::new) : null)
        .addValidator(BlockTypeListAsset.VALIDATOR_CACHE.getArrayValidator().late())
        .add();
    CODEC = builder.build();
  }

}
