package st4s1k.jdbcplus;

public class Function<T, U> implements java.util.function.Function<T, U> {

  private final java.util.function.Function<T, U> function;

  private Function(final java.util.function.Function<T, U> function) {
    this.function = function;
  }

  public static <T, U> Function<T, U> of(final java.util.function.Function<T, U> function) {
    return new Function<>(function);
  }

  public U apply(final T t) {
    return function.apply(t);
  }
}
