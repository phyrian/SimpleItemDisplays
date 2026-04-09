package org.phyrian.displays.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public enum DisplayOrientation {
  HORIZONTAL,
  VERTICAL;

  public final static Codec<DisplayOrientation> CODEC = new EnumCodec<>(DisplayOrientation.class);
}
