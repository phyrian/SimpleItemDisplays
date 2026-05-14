package org.phyrian.displays.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BlockUtils {

  public static final String DEFAULT_STATE = "default";
  public static final String FULL_STATE = "Full";

  private BlockUtils() {
  }

  @Nullable
  public static Vector3i locateBlockByRef(Ref<ChunkStore> ref) {
    var store = ref.getStore();
    var blockStateInfo = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
    if (blockStateInfo == null) {
      return null;
    }

    var chunkRef = blockStateInfo.getChunkRef();
    var worldChunk = store.getComponent(chunkRef, WorldChunk.getComponentType());
    if (worldChunk == null) {
      return null;
    }

    var chunkX = worldChunk.getX();
    var chunkZ = worldChunk.getZ();

    var blockIndexInColumn = blockStateInfo.getIndex();
    var localX = ChunkUtil.xFromBlockInColumn(blockIndexInColumn);
    var globalY = ChunkUtil.yFromBlockInColumn(blockIndexInColumn);
    var localZ = ChunkUtil.zFromBlockInColumn(blockIndexInColumn);

    var globalX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
    var globalZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);

    return new Vector3i(globalX, globalY, globalZ);
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

    if (chunk.getBlock(pos) != 0) {
      //noinspection DataFlowIssue
      chunk.setBlock(pos.getX(), pos.getY(), pos.getZ(), newBlockId, newBlockType, rotation, 0,
          settings);
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
}
