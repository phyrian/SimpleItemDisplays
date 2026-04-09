package org.phyrian.displays.config;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;

public class DisplayTransform {

  public static final Codec<DisplayTransform> CODEC;

  private static final float DEFAULT_SCALE = 1.0F;

  private Vector3d position;
  private Vector3f rotation;
  private float scale;

  public DisplayTransform() {
    this(new Vector3d(), new Vector3f(), DEFAULT_SCALE);
  }

  public DisplayTransform(@Nonnull Vector3d displayPosition, @Nonnull Vector3f displayRotation) {
    this(displayPosition, displayRotation, DEFAULT_SCALE);
  }

  public DisplayTransform(@Nonnull Vector3d position, @Nonnull Vector3f rotation, float scale) {
    this.position = position;
    this.rotation = rotation;
    this.scale = scale;
  }

  public DisplayTransform(DisplayTransform other) {
    this(other.position.clone(), other.rotation.clone(), other.scale);
  }

  @Nonnull
  public DisplayTransform add(@Nonnull DisplayTransform other) {
    this.position.add(other.position);
    this.rotation.add(other.rotation);
    this.scale *= other.scale;
    return this;
  }

  @Nonnull
  public Vector3d getPosition() {
    return position;
  }

  public void setPosition(@Nonnull Vector3d position) {
    this.position = position;
  }

  @Nonnull
  public Vector3f getRotation() {
    return rotation;
  }

  public void setRotation(@Nonnull Vector3f rotation) {
    this.rotation = rotation;
  }

  public float getScale() {
    return scale;
  }

  public void setScale(float scale) {
    this.scale = scale;
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
    Builder<DisplayTransform> builder = BuilderCodec.builder(DisplayTransform.class,
        DisplayTransform::new);
    builder
        .append(new KeyedCodec<>("Position", Vector3d.CODEC),
            (component, position) -> component.position = Objects.requireNonNullElseGet(position, Vector3d::new),
            (component) -> component.position)
        .add();
    builder
        .append(new KeyedCodec<>("Rotation", Vector3i.CODEC),
            (component, rotation) -> component.rotation = rotation != null
                ? rotation.toVector3f().scale((float) Math.toRadians(1))
                : new Vector3f(),
            (component) -> component.rotation.scale((float) Math.toDegrees(1)).toVector3d().toVector3i())
        .add();
    builder
        .append(new KeyedCodec<>("Scale", Codec.FLOAT),
            (component, scale) -> component.scale = Objects.requireNonNullElse(scale, DEFAULT_SCALE),
            (component) -> component.scale)
        .add();
    CODEC = builder.build();
  }
}
