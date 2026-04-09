package org.phyrian.displays.interaction;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.phyrian.displays.component.DisplayedItemComponent;
import org.phyrian.displays.component.ItemDisplayBlock;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class RemoveDisplayedItemInteraction extends SimpleInstantInteraction {

  public static final BuilderCodec<RemoveDisplayedItemInteraction> CODEC;

  protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context,
      @Nonnull CooldownHandler handler) {
    var commandBuffer = context.getCommandBuffer();

    assert commandBuffer != null;

    var world = commandBuffer.getExternalData().getWorld();
    var targetRef = context.getTargetEntity();

    if (targetRef == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var displayComponent = commandBuffer.getComponent(targetRef, DisplayedItemComponent.getComponentType());
    if (displayComponent == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var pos = displayComponent.getDisplayPosition();
    var chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
    var blockType = world.getBlockType(pos);
    var rotationIndex = world.getBlockRotationIndex(pos.x, pos.y, pos.z);

    if (chunk == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
    var chunkStore = world.getChunkStore().getStore();
    var itemDisplay = chunkRef != null ? chunkStore.getComponent(chunkRef, ItemDisplayBlock.getComponentType()) : null;

    var ref = context.getEntity();
    if (blockType == null || itemDisplay == null || !isEntityAttached(targetRef, itemDisplay)) {
      commandBuffer.run((store) -> {
        displayComponent.dropItem(store, ref);
        store.removeEntity(targetRef, RemoveReason.REMOVE);
        if (itemDisplay != null) {
          itemDisplay.updateState(commandBuffer, ref, pos, chunk, blockType, rotationIndex);
        }
      });
    } else {
      itemDisplay.removeItem(commandBuffer, ref, pos, chunk, blockType, rotationIndex);
    }
  }

  private static boolean isEntityAttached(Ref<EntityStore> ref, ItemDisplayBlock itemDisplay) {
    if (itemDisplay.getAnchoredEntityId() == null) {
      return false;
    }

    var uuidComponent = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
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
