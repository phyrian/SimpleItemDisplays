package org.phyrian.displays.interaction;

import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.phyrian.displays.component.DisplayContainerBlock;
import org.phyrian.displays.component.DisplayedItemComponent;

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

    var componentType = DisplayedItemComponent.getComponentType();
    var component = commandBuffer.getComponent(targetRef, componentType);
    if (component == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var pos = component.getDisplayPosition();
    var chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
    var chunk = world.getChunk(chunkIndex);
    if (chunk == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    var rotationIndex = chunk.getRotationIndex(pos.x, pos.y, pos.z);
    var blockType = chunk.getBlockType(pos);
    var chunkRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);

    var ref = context.getEntity();
    var chunkStore = world.getChunkStore().getStore();

    commandBuffer.run(store -> {
      var display = chunkRef == null ? null : chunkStore.getComponent(chunkRef, DisplayContainerBlock.getComponentType());
      if (blockType == null || display == null || !isEntityAttached(targetRef, display)) {
        component.dropItem(store, ref);
        store.removeEntity(targetRef, RemoveReason.REMOVE);
        if (display != null) {
          display.update(store, pos, world, blockType, rotationIndex);
        }
        return;
      }

      var uuidComponent = store.getComponent(targetRef, UUIDComponent.getComponentType());
      if (uuidComponent != null) {
        var uuid = uuidComponent.getUuid();
        if (display.removeItem(uuid, store, ref, pos, world)) {
          return;
        }
      }

      display.removeLastItem(store, ref, pos, world);
    });
  }

  private static boolean isEntityAttached(Ref<EntityStore> ref, DisplayContainerBlock display) {
    return Arrays.stream(display.getDisplaySlots())
        .anyMatch(container -> {
          var anchoredEntityId = container.getAnchoredEntityId();
          if (anchoredEntityId == null) {
            return false;
          }

          var uuidComponent = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
          if (uuidComponent == null) {
            return false;
          }

          return Objects.equals(anchoredEntityId, uuidComponent.getUuid());
        });
  }

  static {
    CODEC = BuilderCodec.builder(RemoveDisplayedItemInteraction.class, RemoveDisplayedItemInteraction::new,
            SimpleInstantInteraction.CODEC)
        .documentation("Handles an item inside display entity behaviour.")
        .build();
  }
}
