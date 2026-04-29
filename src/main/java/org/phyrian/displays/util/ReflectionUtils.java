package org.phyrian.displays.util;

import java.util.Arrays;

public class ReflectionUtils {

  private ReflectionUtils() {
  }

  public static <T> T[] cloneArray(T[] array, Class<T> tClass) {
    if (array == null) {
      return null;
    }

    var newArray = Arrays.copyOf(array, array.length);
    if (array.length == 0) {
      return newArray;
    }

    Arrays.fill(newArray, null);
    try {
      var cloneMethod = tClass.getDeclaredMethod("clone");
      if (!cloneMethod.trySetAccessible()
          || !tClass.isAssignableFrom(cloneMethod.getReturnType())) {
        return newArray;
      }

      for (int i = 0; i < array.length; i++) {
        var t = array[i];
        //noinspection unchecked
        newArray[i] = (T) cloneMethod.invoke(t);
      }
    } catch (Exception e) {
      // no-op
    }

    return newArray;
  }
}
