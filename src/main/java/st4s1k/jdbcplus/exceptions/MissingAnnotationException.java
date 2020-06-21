package st4s1k.jdbcplus.exceptions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;

public class MissingAnnotationException extends JdbcPlusException {

  @SafeVarargs
  protected MissingAnnotationException(
      final Field field,
      final Class<? extends Annotation> annotation,
      final Class<? extends Annotation>... annotations
  ) {
    super(annotations.length > 0
              ? getMessageForMultipleAnnotations(field, annotation, annotations)
              : getMessageForSingleAnnotation(field, annotation));
  }

  @SafeVarargs
  protected MissingAnnotationException(
      final Class<?> clazz,
      final Class<? extends Annotation> annotation,
      final Class<? extends Annotation>... annotations
  ) {
    super(annotations.length > 0
              ? getMessageForMultipleAnnotations(clazz, annotation, annotations)
              : getMessageForSingleAnnotation(clazz, annotation));
  }

  @SafeVarargs
  public static MissingAnnotationException of(
      final Field field,
      final Class<? extends Annotation> annotation,
      final Class<? extends Annotation>... annotations
  ) {
    return new MissingAnnotationException(field, annotation, annotations);
  }

  @SafeVarargs
  public static MissingAnnotationException of(
      final Class<?> clazz,
      final Class<? extends Annotation> annotation,
      final Class<? extends Annotation>... annotations
  ) {
    return new MissingAnnotationException(clazz, annotation, annotations);
  }

  @SafeVarargs
  protected static String getMessageForMultipleAnnotations(
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

  protected static String getMessageForSingleAnnotation(
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

  @SafeVarargs
  protected static String getMessageForMultipleAnnotations(
      final Class<?> clazz,
      final Class<? extends Annotation> annotation,
      final Class<? extends Annotation>... annotations
  ) {
    return String.format(
        "Class %s is missing any of this annotations: @%s%s",
        clazz.getName(),
        annotation.getSimpleName(),
        Arrays.stream(annotations)
            .map(Class::getSimpleName)
            .map(name -> ", @" + name)
            .reduce(String::concat)
            .orElse("")
    );
  }

  protected static String getMessageForSingleAnnotation(
      final Class<?> clazz,
      final Class<? extends Annotation> annotation
  ) {
    return String.format(
        "Class %s is missing annotation: @%s",
        clazz.getName(),
        annotation.getSimpleName()
    );
  }
}
