package org.phyrian.displays.util;

import java.util.Arrays;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

public class ReflectionUtils {

  private ReflectionUtils() {
  }

  public static <T> T[] copyArray(@Nullable T[] array) {
    return copyArray(array, UnaryOperator.identity());
  }

  public static <T> T[] copyArray(@Nullable T[] array, UnaryOperator<T> elementMapper) {
    if (array == null) {
      return null;
    }

    var newArray = Arrays.copyOf(array, array.length);
    for (int i = 0; i < array.length; i++) {
      var t = array[i];
      //noinspection
      newArray[i] = elementMapper.apply(t);
    }

    return newArray;
  }
}
