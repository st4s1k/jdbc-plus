package st4s1k.jdbcplus.utils;

import java.lang.annotation.Annotation;
import java.util.function.Function;

class TargetEntitySupplier<A extends Annotation> {

  private final Function<A, Class<?>> supplier;

  private TargetEntitySupplier(final Function<A, Class<?>> supplier) {
    this.supplier = supplier;
  }

  static <A extends Annotation> TargetEntitySupplier<A> of(Function<A, Class<?>> supplier) {
    return new TargetEntitySupplier<>(supplier);
  }

  Function<A, Class<?>> get() {
    return supplier;
  }
}
