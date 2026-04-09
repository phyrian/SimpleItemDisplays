package org.phyrian.displays.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public enum DisplayKind {
  DEFAULT,
  MODEL,
  BLOCK,
  ITEM;

  public static final Codec<DisplayKind> CODEC = new EnumCodec<>(DisplayKind.class);
}
