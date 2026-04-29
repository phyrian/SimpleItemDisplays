package org.phyrian.displays;

import javax.annotation.Nonnull;

import org.phyrian.displays.component.DisplayContainerBlock;
import org.phyrian.displays.component.DisplayedItemComponent;
import org.phyrian.displays.component.ItemDisplayBlock;
import org.phyrian.displays.event.BlockReplaceEventSystem;
import org.phyrian.displays.event.BreakBlockEventSystem;
import org.phyrian.displays.event.PlaceBlockEventSystem;
import org.phyrian.displays.interaction.ChangeOrientationInteraction;
import org.phyrian.displays.interaction.ChangeScaleInteraction;
import org.phyrian.displays.interaction.DisplayItemInteraction;
import org.phyrian.displays.interaction.RemoveDisplayedItemInteraction;
import org.phyrian.displays.interaction.RemoveItemInteraction;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import lombok.Getter;

/**
 * Adds custom interactions for putting items into Simple Item Displays
 */
public class SimpleItemDisplaysPlugin extends JavaPlugin {

  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Getter
  private static SimpleItemDisplaysPlugin instance;

  public SimpleItemDisplaysPlugin(@Nonnull JavaPluginInit init) {
    super(init);
    instance = this;
    LOGGER.atInfo().log("SimpleItemDisplays v" + this.getManifest().getVersion().toString() + " loading...");
  }

  @Override
  protected void setup() {
    LOGGER.atInfo().log("Setting up SimpleItemDisplays...");
    super.setup();

    DisplayContainerBlock.TYPE = this.getChunkStoreRegistry().registerComponent(DisplayContainerBlock.class, "SimpleItemDisplays_DisplayContainerBlock", DisplayContainerBlock.CODEC);
    ItemDisplayBlock.TYPE = this.getChunkStoreRegistry().registerComponent(ItemDisplayBlock.class, "SimpleItemDisplays_ItemDisplayBlock", ItemDisplayBlock.CODEC);
    DisplayedItemComponent.TYPE = this.getEntityStoreRegistry().registerComponent(DisplayedItemComponent.class, "SimpleItemDisplays_DisplayedItem", DisplayedItemComponent.CODEC);

    this.getCodecRegistry(Interaction.CODEC).register("SimpleItemDisplays_ChangeOrientation", ChangeOrientationInteraction.class, ChangeOrientationInteraction.CODEC);
    this.getCodecRegistry(Interaction.CODEC).register("SimpleItemDisplays_ChangeScale", ChangeScaleInteraction.class, ChangeScaleInteraction.CODEC);
    this.getCodecRegistry(Interaction.CODEC).register("SimpleItemDisplays_DisplayItem", DisplayItemInteraction.class, DisplayItemInteraction.CODEC);
    this.getCodecRegistry(Interaction.CODEC).register("SimpleItemDisplays_RemoveItem", RemoveItemInteraction.class, RemoveItemInteraction.CODEC);
    this.getCodecRegistry(Interaction.CODEC).register("SimpleItemDisplays_RemoveDisplayedItem", RemoveDisplayedItemInteraction.class, RemoveDisplayedItemInteraction.CODEC);

    this.getChunkStoreRegistry().registerSystem(new BlockReplaceEventSystem());
    this.getEntityStoreRegistry().registerSystem(new BreakBlockEventSystem());
    this.getEntityStoreRegistry().registerSystem(new PlaceBlockEventSystem());

    LOGGER.atInfo().log("SimpleItemDisplays setup complete!");
  }

  @Override
  protected void start() {
    super.start();
  }

  @Override
  protected void shutdown() {
    super.shutdown();
  }

}
