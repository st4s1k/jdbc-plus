package st4s1k.jdbcplus.exceptions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;

public class MissingAnnotationException extends RuntimeException {
  @SafeVarargs
  public MissingAnnotationException(
      final Field field,
      final Class<? extends Annotation> annotation,
      final Class<? extends Annotation>... annotations
  ) {
    super(annotations.length > 0
              ? getMessageForMultipleAnnotations(field, annotation, annotations)
              : getMessageForSingleAnnotation(field, annotation));
  }

  @SafeVarargs
  public static String getMessageForMultipleAnnotations(
      final Field field,
      final Class<? extends Annotation> annotation,
      final Class<? extends Annotation>... annotations
  ) {
    return String.format(
        "Field (%s %s) is missing any of this annotations: @%s%s",
        field.getType().getName(),
        field.getName(),
        annotation.getSimpleName(),
        Arrays.stream(annotations)
            .map(Class::getSimpleName)
            .map(name -> ", @" + name)
            .reduce(String::concat)
            .orElse("")
    );
  }

  public static String getMessageForSingleAnnotation(
      final Field field,
      final Class<? extends Annotation> annotation
  ) {
    return String.format(
        "Field (%s %s) is missing annotation: @%s",
        field.getType().getName(),
        field.getName(),
        annotation.getSimpleName()
    );
  }
}
