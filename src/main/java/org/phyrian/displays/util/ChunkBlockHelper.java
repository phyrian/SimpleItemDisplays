package org.phyrian.displays.util;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChunkBlockHelper {
  private World world;
  private Vector3i pos;
  private int index;
  private ChunkStore chunkStore;
  private Store<ChunkStore> store;
  private Ref<ChunkStore> sectionRef;
  private BlockSection blockSection;
  private BlockComponentSection blockComponentSection;
  private Ref<ChunkStore> blockRef;
  private BlockType blockType;
  private int rotationIndex = -1;

  public static ChunkBlockHelper create(World world, Vector3i pos) {
    var blockInfo = new ChunkBlockHelper();

    blockInfo.world = world;
    blockInfo.pos = pos;
    blockInfo.index = ChunkUtil.indexBlock(pos.x, pos.y, pos.z);
    blockInfo.chunkStore = world.getChunkStore();
    blockInfo.store = blockInfo.chunkStore.getStore();
    blockInfo.sectionRef = blockInfo.chunkStore.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);

    if (blockInfo.sectionRef == null || !blockInfo.sectionRef.isValid()) {
      return blockInfo;
    }
    
    blockInfo.blockSection = blockInfo.store.getComponent(blockInfo.sectionRef, BlockSection.getComponentType());
    blockInfo.blockComponentSection = blockInfo.store.getComponent(blockInfo.sectionRef,
        BlockComponentSection.getComponentType());
    
    if (blockInfo.blockSection == null || blockInfo.blockComponentSection == null) {
      return blockInfo;
    }
    
    blockInfo.rotationIndex = blockInfo.blockSection.getRotationIndex(blockInfo.index);
    blockInfo.blockType = BlockType.getAssetMap().getAsset(blockInfo.blockSection.get(blockInfo.index));
    blockInfo.blockRef = blockInfo.blockComponentSection.getBlockReference(blockInfo.index);
    
    return blockInfo;
  }

  public <T extends Component<ChunkStore>> T getBlockComponent(ComponentType<ChunkStore, T> componentType) {
    if (blockRef == null) {
      return null;
    }
    return store.getComponent(blockRef, componentType);
  }

  public <T extends Component<ChunkStore>> void addBlockComponent(ComponentType<ChunkStore, T> componentType, T component) {
    if (blockRef == null) {
      return;
    }
    store.addComponent(blockRef, componentType, component);
  }
}
