package org.phyrian.displays.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.DisplayContainerBlock;
import org.phyrian.displays.util.ItemTransferContext;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import static org.phyrian.displays.SimpleItemDisplaysPlugin.LOGGER;

public class DisplayItemInteraction extends SimpleBlockInteraction {

  public static final BuilderCodec<DisplayItemInteraction> CODEC;

  @Override
  protected void interactWithBlock(@Nonnull World world,
      @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type,
      @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i pos,
      @Nonnull CooldownHandler cooldownHandler) {
    var indexChunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
    var chunk = world.getChunk(indexChunk);
    var blockType = world.getBlockType(pos);
    var rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

    if (blockType == null || chunk == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var componentType = DisplayContainerBlock.getComponentType();
    var blockTypeDisplay = blockType.getBlockEntity().getComponent(componentType);
    if (blockTypeDisplay == null) {
      LOGGER.atWarning().log("Failed to interact with display due to missing ItemDisplayBlock component.");
      context.getState().state = InteractionState.Failed;
      return;
    }

    var chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    if (chunkRef == null) {
      LOGGER.atWarning().log("Failed to interact with " + blockType.getId() + " at position " + pos
          + " due to missing chunk ref.");
      context.getState().state = InteractionState.Failed;
      return;
    }

    var chunkStore = world.getChunkStore().getStore();
    var display = chunkStore.getComponent(chunkRef, componentType);
    if (display == null) {
      display = new DisplayContainerBlock(blockTypeDisplay);
      chunkStore.addComponent(chunkRef, componentType, display);
    }

    if (itemInHand == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var itemContainer = context.getHeldItemContainer();
    if (itemContainer == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var ref = context.getEntity();
    var itemSlot = context.getHeldItemSlot();
    var transferContext = new ItemTransferContext(itemContainer, itemSlot, itemInHand);
    if (!display.addItem(commandBuffer, ref, pos, transferContext, blockType, rotationIndex)) {
      context.getState().state = InteractionState.Failed;
    }
  }

  @Override
  protected void simulateInteractWithBlock(@Nonnull InteractionType type,
      @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world,
      @Nonnull Vector3i targetBlock) {
    // no-op
  }

  static {
    CODEC = BuilderCodec.builder(DisplayItemInteraction.class, DisplayItemInteraction::new,
            SimpleBlockInteraction.CODEC)
        .documentation("Adds an item to the target item display block.")
        .build();
  }
}
