package org.phyrian.displays.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.ItemDisplayBlock;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class DisplayItemInteraction extends SimpleBlockInteraction {

  public static final BuilderCodec<DisplayItemInteraction> CODEC;

  @Override
  protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context,
      @Nullable ItemStack itemInHand, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
    long indexChunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
    WorldChunk chunk = world.getChunk(indexChunk);
    BlockType blockType = world.getBlockType(pos);
    int rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

    if (blockType == null || chunk == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    ItemDisplayBlock blockItemDisplay = blockType.getBlockEntity().getComponent(ItemDisplayBlock.getComponentType());
    if (blockItemDisplay == null) {
      LOGGER.atWarning().log("Failed to interact with display due to missing ItemDisplayBlock component.");
      context.getState().state = InteractionState.Failed;
      return;
    }

    Ref<ChunkStore> chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    if (chunkRef == null) {
      LOGGER.atWarning().log("Failed to interact with " + blockType.getId() + " at position " + pos + " due to missing chunk ref.");
      context.getState().state = InteractionState.Failed;
      return;
    }

    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    ItemDisplayBlock itemDisplay = chunkStore.getComponent(chunkRef, ItemDisplayBlock.getComponentType());
    if (itemDisplay == null) {
      itemDisplay = new ItemDisplayBlock(blockItemDisplay);
      chunkStore.addComponent(chunkRef, ItemDisplayBlock.getComponentType(), itemDisplay);
    }

    Ref<EntityStore> ref = context.getEntity();
    if (itemDisplay.getAnchoredEntityId() != null) {
      MovementStatesComponent movementStates = ref.getStore()
          .getComponent(ref, MovementStatesComponent.getComponentType());
      if (movementStates != null && movementStates.getMovementStates().crouching) {
        itemDisplay.removeItem(commandBuffer, ref, pos, chunk, blockType, rotationIndex);
        return;
      }

      context.getState().state = InteractionState.Failed;
      return;
    }

    if (itemInHand == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    if (!itemDisplay.canHoldItem(itemInHand.getItemId())) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    ItemStack itemStack = itemInHand.withQuantity(1);
    if (itemStack == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    if (context.getHeldItemContainer() != null) {
      ItemStackSlotTransaction transaction = context.getHeldItemContainer().removeItemStackFromSlot(context.getHeldItemSlot(), itemInHand, 1);
      if (!transaction.succeeded()) {
        context.getState().state = InteractionState.Failed;
        return;
      }
    }

    itemDisplay.addItem(commandBuffer, ref, pos, itemStack, chunk, blockType, rotationIndex);
  }

  @Override
  protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand,
      @Nonnull World world, @Nonnull Vector3i targetBlock) {
  }

  static {
    CODEC = BuilderCodec.builder(DisplayItemInteraction.class, DisplayItemInteraction::new,
            SimpleBlockInteraction.CODEC)
        .documentation("Handles an item display entity behaviour.")
        .build();
  }
}
