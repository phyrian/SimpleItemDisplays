package org.phyrian.displays.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;
import org.phyrian.displays.component.DisplayContainerBlock;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BlockUtils {

  public static final String DEFAULT_STATE = "default";
  public static final String FULL_STATE = "Full";

  private BlockUtils() {
  }

  public static boolean hasState(BlockType blockType, String state) {
    return blockType.getBlockForState(state) != null;
  }

  public static boolean isInState(BlockType blockType, String state) {
    return state.equals(blockType.getStateForBlock(blockType));
  }

  public static void changeState(@Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nullable Ref<EntityStore> ref, @Nonnull Vector3i pos, @Nonnull WorldChunk chunk,
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

    if (chunk.getBlock(pos.x, pos.y, pos.z) != 0) {
      //noinspection DataFlowIssue
      chunk.setBlock(pos.x, pos.y, pos.z, newBlockId, newBlockType, rotation, 0, settings);
    }

    var interactionStateBlock = current.getBlockForState(newState);
    if (interactionStateBlock == null) {
      return;
    }

    var soundEventIndex = interactionStateBlock.getInteractionSoundEventIndex();
    if (soundEventIndex == 0) {
      return;
    }

    SoundUtil.playSoundEvent3d(ref, soundEventIndex, (double) pos.x + (double) 0.5F,
        (double) pos.y + (double) 0.5F, (double) pos.z + (double) 0.5F, commandBuffer);
  }

  public static DisplayContainerBlock getDisplayContainerBlock(World world, Vector3i targetBlock) {
    var chunkStore = world.getChunkStore();
    var chunkStoreStore = chunkStore.getStore();
    var sectionReference = chunkStore.getChunkSectionReferenceAtBlock(targetBlock.x, targetBlock.y,
        targetBlock.z);
    if (sectionReference == null || !sectionReference.isValid()) {
      return null;
    }

    var blockComponentSection = chunkStoreStore.getComponent(sectionReference,
        BlockComponentSection.getComponentType());
    if (blockComponentSection == null) {
      return null;
    }

    var blockIndex = ChunkUtil.indexBlock(targetBlock.x, targetBlock.y, targetBlock.z);
    var blockRef = blockComponentSection.getBlockReference(blockIndex);
    if (blockRef == null) {
      return null;
    }

    return chunkStoreStore.getComponent(blockRef, DisplayContainerBlock.getComponentType());
  }
}
