package org.phyrian.displays.config;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.phyrian.displays.util.DisplayUtils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Rotation3fc;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.math.vector.Vector3iUtil;

import lombok.Data;

@Data
public class DisplayTransform {

  public static final Codec<DisplayTransform> CODEC;

  private Vector3d position;
  private Rotation3fc rotation;
  private float scale;

  public DisplayTransform() {
    this(new Vector3d(), new Rotation3f());
  }

  public DisplayTransform(@Nonnull Vector3d position, @Nonnull Rotation3fc rotation) {
    this(position, rotation, DisplayUtils.DEFAULT_SCALE);
  }

  public DisplayTransform(@Nonnull Vector3d position, @Nonnull Rotation3fc rotation, float scale) {
    this.position = position;
    this.rotation = rotation;
    this.scale = scale;
  }

  public DisplayTransform(DisplayTransform other) {
    this(new Vector3d(other.position), new Rotation3f(other.rotation), other.scale);
  }

  @Nonnull
  public DisplayTransform add(@Nonnull DisplayTransform other) {
    this.position.add(other.position);
    this.rotation = new Rotation3f(
        this.rotation.x() + other.rotation.x(),
        this.rotation.y() + other.rotation.y(),
        this.rotation.z() + other.rotation.z()
    );
    this.scale *= other.scale;
    return this;
  }

  @Nonnull
  public DisplayTransform addPosition(@Nonnull Vector3d position) {
    this.position.add(position);
    return this;
  }

  @Nonnull
  public DisplayTransform addRotation(@Nonnull Rotation3fc rotation) {
    this.rotation = new Rotation3f(
        this.rotation.x() + rotation.x(),
        this.rotation.y() + rotation.y(),
        this.rotation.z() + rotation.z()
    );
    return this;
  }

  @Nonnull
  public DisplayTransform scale(float scale) {
    this.scale *= scale;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (DisplayTransform) obj;
    return Objects.equals(this.position, that.position) &&
        Objects.equals(this.rotation, that.rotation) &&
        Objects.equals(this.scale, that.scale);
  }

  @Override
  public int hashCode() {
    return Objects.hash(position, rotation, scale);
  }

  @Override
  public String toString() {
    return "DisplayTransform[" +
        "position=" + position + ", " +
        "rotation=" + rotation + ", " +
        "scale=" + scale + ']';
  }

  @Override
  public DisplayTransform clone() {
    return new DisplayTransform(this);
  }

  static {
    CODEC = BuilderCodec.builder(DisplayTransform.class, DisplayTransform::new)
        .append(new KeyedCodec<>("Position", Vector3dUtil.CODEC),
            (component, position) -> component.position = Objects.requireNonNullElseGet(position, Vector3d::new),
            (component) -> component.position)
        .add()
        .append(new KeyedCodec<>("Rotation", Vector3iUtil.CODEC),
            (component, rotation) -> component.rotation = rotation != null
                ? new Rotation3f(
                    rotation.x * (float) Math.toRadians(1),
                    rotation.y * (float) Math.toRadians(1),
                    rotation.z * (float) Math.toRadians(1)
                  )
                : new Rotation3f(),
            (component) -> new Vector3f(
                component.rotation.x() * (float) Math.toDegrees(1),
                component.rotation.y() * (float) Math.toDegrees(1),
                component.rotation.z() * (float) Math.toDegrees(1)
            ).get(RoundingMode.HALF_EVEN, new Vector3i()))
        .add()
        .append(new KeyedCodec<>("Scale", Codec.FLOAT),
            (component, scale) -> component.scale = Objects.requireNonNullElse(scale, DisplayUtils.DEFAULT_SCALE),
            (component) -> component.scale)
        .add()
        .build();
  }
}
