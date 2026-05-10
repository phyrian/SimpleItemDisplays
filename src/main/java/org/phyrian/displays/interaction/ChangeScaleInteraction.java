package org.phyrian.displays.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.phyrian.displays.component.DisplayContainerBlock;
import org.phyrian.displays.config.DisplayTransform;
import org.phyrian.displays.util.DisplayUtils;

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

public class ChangeScaleInteraction extends SimpleBlockInteraction {

  public static final BuilderCodec<ChangeScaleInteraction> CODEC;

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

    var chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    if (chunkRef == null) {
      LOGGER.atWarning().log("Failed to interact with " + blockType.getId() + " at position " + pos
          + " due to missing chunk ref.");
      context.getState().state = InteractionState.Failed;
      return;
    }

    var chunkStore = world.getChunkStore().getStore();
    var display = chunkStore.getComponent(chunkRef, DisplayContainerBlock.getComponentType());
    if (display == null) {
      LOGGER.atWarning().log("Failed to interact with display due to missing DisplayContainerBlock"
          + " component.");
      context.getState().state = InteractionState.Failed;
      return;
    }

    for (var displayContainer : display.getDisplayContainers()) {
      var displayTransform = displayContainer.getDisplayTransform();
      if (displayContainer.getDisplayTransform() == null) {
        displayTransform = new DisplayTransform();
        displayContainer.setDisplayTransform(displayTransform);
      }

      float currentScale = displayTransform.getScale();

      var maxScale = DisplayUtils.DEFAULT_SCALE * 2;
      var minScale = DisplayUtils.DEFAULT_SCALE / 2;

      var newScale = currentScale >= maxScale ? minScale : currentScale + minScale;
      displayTransform.setScale(newScale);
    }

    display.update(commandBuffer, pos, chunk, blockType, rotationIndex);
  }

  @Override
  protected void simulateInteractWithBlock(@Nonnull InteractionType type,
      @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world,
      @Nonnull Vector3i targetBlock) {
    // no-op
  }

  static {
    CODEC = BuilderCodec.builder(ChangeScaleInteraction.class, ChangeScaleInteraction::new,
            SimpleBlockInteraction.CODEC)
        .documentation("Changes the display scale of the target item display block.")
        .build();
  }
}
