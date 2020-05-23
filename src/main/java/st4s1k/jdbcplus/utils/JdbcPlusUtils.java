package st4s1k.jdbcplus.utils;

import java.lang.reflect.Array;
import java.util.function.UnaryOperator;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;

public class JdbcPlusUtils {

  private JdbcPlusUtils() {
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] concatenateArrays(final Class<T> clazz,
                                          final T[]... arrays) {
    int arrLen = calcArraysLength(arrays);
    final T[] newArr = (T[]) Array.newInstance(clazz, arrLen);
    if (arrLen > 0) {
      copyArrays(newArr, arrays);
    }
    return newArr;
  }

  private static <T> int calcArraysLength(T[][] arrays) {
    int arrLen = 0;
    for (T[] array : arrays) {
      if (array != null && array.length > 0) {
        arrLen += array.length;
      }
    }
    return arrLen;
  }

  private static <T> void copyArrays(T[] newArr, T[][] arrays) {
    int destPos = 0;
    for (T[] array : arrays) {
      if (array != null && array.length > 0) {
        System.arraycopy(array, 0, newArr, destPos, array.length);
        destPos += array.length;
      }
    }
  }

  public static String toSnakeLowerCase(final String str) {
    return toSnakeCase(str, String::toLowerCase);
  }

  public static String toSnakeUpperCase(final String str) {
    return toSnakeCase(str, String::toUpperCase);
  }

  private static String toSnakeCase(final String str,
                                    final UnaryOperator<String> postProcess) {
    final StringBuilder result = new StringBuilder(str);
    if (str.length() > 1) {
      for (int i = 1; i < result.length(); i++) {
        if (isUpperCase(str.charAt(i)) && isLowerCase(str.charAt(i - 1))) {
          result.insert(i, '_');
        }
      }
    }
    return postProcess.apply(result.toString());
  }

  /**
   * This method accepts a class object (type) as a parameter and
   * returns an instance of that class.
   *
   * @param clazz The Class object
   * @param <X>   The object type of the Class, for example Class<EntityUtils>
   * @return An instance of the Class object, or null if an exception occurs
   */
  public static <X> X getClassInstance(final Class<X> clazz) {
    try {
      return clazz.getConstructor().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
