package org.phyrian.displays.util;

import org.joml.Vector3i;
import org.phyrian.displays.component.DisplayContainerBlock;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;

public class BlockUtils {

  private BlockUtils() {
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
