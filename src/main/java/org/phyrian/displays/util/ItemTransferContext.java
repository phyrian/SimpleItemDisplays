package org.phyrian.displays.util;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import lombok.Getter;

@Getter
public class ItemTransferContext {

  private final ItemContainer itemContainer;
  private final byte slot;
  private final ItemStack itemStack;

  public ItemTransferContext(ItemContainer itemContainer, byte slot, ItemStack itemStack) {
    this.itemContainer = itemContainer;
    this.slot = slot;
    this.itemStack = itemStack;
  }

  @Nullable
  public ItemStack remove(int amount) {
    var transferAmount = Math.min(itemStack.getQuantity(), amount);
    if (transferAmount <= 0) {
      return null;
    }
    var transaction = itemContainer.removeItemStackFromSlot(slot, itemStack, transferAmount);
    if (!transaction.succeeded()) {
      return null;
    }
    return transaction.getQuery();
  }
}
