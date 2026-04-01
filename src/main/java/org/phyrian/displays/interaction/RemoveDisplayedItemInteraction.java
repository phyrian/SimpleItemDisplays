package org.phyrian.displays.interaction;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.phyrian.displays.component.DisplayedItemComponent;
import org.phyrian.displays.component.ItemDisplayBlock;
import org.phyrian.displays.util.ItemUtils;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class RemoveDisplayedItemInteraction extends SimpleInstantInteraction {

  public static final BuilderCodec<RemoveDisplayedItemInteraction> CODEC;

  protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context,
      @Nonnull CooldownHandler handler) {
    CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

    assert commandBuffer != null;

    World world = commandBuffer.getExternalData().getWorld();
    Ref<EntityStore> targetRef = context.getTargetEntity();

    if (targetRef == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    DisplayedItemComponent displayComponent = commandBuffer.getComponent(targetRef, DisplayedItemComponent.getComponentType());
    if (displayComponent == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    Vector3i pos = displayComponent.getDisplayPosition();
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
    BlockType blockType = world.getBlockType(pos);
    int rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

    if (chunk == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    Ref<ChunkStore> chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    ItemDisplayBlock itemDisplay = chunkRef != null ? chunkStore.getComponent(chunkRef, ItemDisplayBlock.getComponentType()) : null;

    Ref<EntityStore> ref = context.getEntity();
    if (blockType == null || itemDisplay == null || !isEntityAttached(targetRef, itemDisplay)) {
      commandBuffer.run((store) -> {
        ItemStack itemStack = displayComponent.getItemStack();
        if (itemStack != null && !ItemStack.isEmpty(itemStack)) {
          Vector3d itemPosition = pos.toVector3d().add(0.5, 0.5, 0.5);

          Player playerComponent = store.getComponent(ref, Player.getComponentType());
          if (playerComponent != null) {
            itemStack = ItemUtils.pickupItem(playerComponent, itemStack, itemPosition, store, ref);
          }

          if (!ItemStack.isEmpty(itemStack)) {
            ItemUtils.spawnItem(store, itemStack, itemPosition);
          }
        }

        store.removeEntity(targetRef, RemoveReason.REMOVE);
        if (itemDisplay != null) {
          LOGGER.atWarning().log("Huh, that's weird...");
          itemDisplay.updateState(commandBuffer, ref, pos, chunk, blockType, rotationIndex);
        }
      });
      return;
    }

    itemDisplay.removeItem(commandBuffer, ref, pos, chunk, blockType, rotationIndex);
  }

  private static boolean isEntityAttached(Ref<EntityStore> ref, ItemDisplayBlock itemDisplay) {
    if (itemDisplay.getAnchoredEntityId() == null) {
      return false;
    }

    UUIDComponent uuidComponent = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
    if (uuidComponent == null) {
      return false;
    }

    return Objects.equals(itemDisplay.getAnchoredEntityId(), uuidComponent.getUuid());
  }

  static {
    CODEC = BuilderCodec.builder(RemoveDisplayedItemInteraction.class, RemoveDisplayedItemInteraction::new,
            SimpleInstantInteraction.CODEC)
        .documentation("Handles an item inside display entity behaviour.")
        .build();
  }
}
