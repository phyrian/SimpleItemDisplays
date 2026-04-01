package org.phyrian.displays.display;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.asset.type.model.config.Model;

public record DisplayVisual(@Nullable Model model, float scale, DisplayKind kind) {
}
