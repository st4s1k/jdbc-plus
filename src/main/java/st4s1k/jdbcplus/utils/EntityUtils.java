package st4s1k.jdbcplus.utils;

import st4s1k.jdbcplus.annotations.*;
import st4s1k.jdbcplus.exceptions.InvalidColumnTypeException;
import st4s1k.jdbcplus.exceptions.InvalidMappingException;
import st4s1k.jdbcplus.exceptions.MissingAnnotationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.System.Logger.Level.ERROR;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.concatenateArrays;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.toSnakeLowerCase;

public class EntityUtils {

  private static final System.Logger LOGGER = System.getLogger("EntityUtils");

  private EntityUtils() {
  }

  public static Class<?> getGenericTypeArgument(final Field field) {
    return (Class<?>) ((ParameterizedType) field.getGenericType())
        .getActualTypeArguments()[0];
  }

  public static Field[] getFieldsAnnotatedWith(
      final Class<? extends Annotation> annotation,
      final Class<?> clazz
  ) {
    return Optional.ofNullable(clazz)
        .map(c -> Arrays.stream(c.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(annotation))
            .toArray(Field[]::new))
        .orElseGet(() -> new Field[]{});
  }

  public static Map<String, Field> getFieldsMap(
      final Field[] fields,
      final Function<Field, String> keyGenerator
  ) {
    return Arrays.stream(fields).collect(toMap(keyGenerator, field -> field));
  }

  public static String getTableName(final Class<?> clazz) {
    if (!clazz.isAnnotationPresent(Table.class)) {
      throw new InvalidMappingException(String.format(
          "Missing @Table annotation in class %s",
          clazz.getName()
      ));
    }
    return Objects.requireNonNullElse(
        clazz.getAnnotation(Table.class).value(),
        toSnakeLowerCase(clazz.getSimpleName())
    );
  }

  public static Field getIdColumn(final Class<?> clazz) {
    final List<Field> idFields = Arrays.asList(getFieldsAnnotatedWith(Id.class, clazz));
    if (idFields.isEmpty()) {
      throw new InvalidMappingException(String.format(
          "Missing @Id annotation in class %s",
          clazz.getName()
      ));
    }
    if (idFields.size() > 1) {
      throw new InvalidMappingException(String.format(
          "Too many @Id annotated columns in class %s",
          clazz.getName()
      ));
    }
    return idFields.get(0);
  }

  public static String getIdColumnName(final Class<?> clazz) {
    return getIdColumnName(getIdColumn(clazz));
  }

  public static String getIdColumnName(final Field field) {
    requireNonNull(field);
    if (field.isAnnotationPresent(Id.class)) {
      return field.isAnnotationPresent(Column.class)
          ? field.getAnnotation(Column.class).value()
          : toSnakeLowerCase(field.getName());
    } else {
      throw new MissingAnnotationException(String.format(
          "Missing @Id annotation on this field (%s %s)",
          field.getType().getName(),
          field.getName()
      ));
    }
  }

  public static Field[] getColumns(final Class<?> clazz) {
    return concatenateArrays(
        Field.class,
        new Field[]{getIdColumn(clazz)},
        getColumnFields(clazz),
        getOneToOneFields(clazz),
        getManyToOneFields(clazz)
    );
  }

  public static Field[] getColumnFields(final Class<?> clazz) {
    return getFieldsAnnotatedWith(Column.class, clazz);
  }

  public static String getColumnName(final Field field) {
    if (field.isAnnotationPresent(Column.class)) {
      return field.getAnnotation(Column.class).value();
    } else if (field.isAnnotationPresent(JoinColumn.class)) {
      return getJoinColumnName(field);
    } else if (field.isAnnotationPresent(Id.class)) {
      return getIdColumnName(field);
    }
    return null;
  }

  public static String[] getColumnNames(final Class<?> clazz) {
    return Arrays.stream(getColumns(clazz))
        .map(EntityUtils::getColumnName)
        .toArray(String[]::new);
  }

  public static String getJoinColumnName(final Field field) {
    requireNonNull(field);
    if (field.isAnnotationPresent(JoinColumn.class)) {
      return field.getAnnotation(JoinColumn.class).value();
    }
    return null;
  }

  public static Map<String, Field> getColumnsMap(final Class<?> clazz) {
    return getFieldsMap(getColumns(clazz), EntityUtils::getColumnName);
  }

  public static Field[] getOneToOneFields(final Class<?> clazz) {
    return getFieldsAnnotatedWith(OneToOne.class, clazz);
  }

  public static Field[] getManyToOneFields(final Class<?> clazz) {
    return getFieldsAnnotatedWith(ManyToOne.class, clazz);
  }

  public static Field getManyToOneField(
      final Class<?> clazz,
      final Class<?> targetEntity
  ) {
    final List<Field> manyToOneParentFields = Arrays.stream(getManyToOneFields(clazz))
        .filter(field -> field.getType().equals(targetEntity))
        .collect(toList());
    if (manyToOneParentFields.isEmpty()) {
      throw new InvalidMappingException(String.format(
          "There should be at least one @%s field of type %s in class %s",
          ManyToOne.class.getSimpleName(),
          targetEntity.getName(),
          clazz.getName()
      ));
    } else if (manyToOneParentFields.size() > 1) {
      throw new InvalidMappingException(String.format(
          "There should be only one @%s field of type %s in class %s",
          ManyToOne.class.getSimpleName(),
          targetEntity.getName(),
          clazz.getName()
      ));
    }
    return manyToOneParentFields.get(0);
  }

  public static Field[] getOneToManyFields(final Class<?> clazz) {
    return getFieldsAnnotatedWith(OneToMany.class, clazz);
  }

  public static Class<?> getTargetEntity(final Field field) {
    if (field.isAnnotationPresent(OneToOne.class)) {
      return getOneToOneTargetEntity(field);
    } else if (field.isAnnotationPresent(ManyToOne.class)) {
      return getManyToOneTargetEntity(field);
    } else if (field.isAnnotationPresent(OneToMany.class)) {
      return getOneToManyTargetEntity(field);
    } else if (field.isAnnotationPresent(ManyToMany.class)) {
      return getManyToManyTargetEntity(field);
    }
    throw new InvalidMappingException(String.format(
        "Field %s does not contain any relation defining annotation",
        field.getName()
    ));
  }

  public static <A extends Annotation> Class<?> getTargetEntity(
      final Field field,
      final Class<A> annotationClass,
      final Function<A, Class<?>> targetEntitySupplier
  ) {
    return Optional.ofNullable(field)
        .filter(f -> f.isAnnotationPresent(annotationClass))
        .map(f -> f.getAnnotation(annotationClass))
        .map(targetEntitySupplier)
        .map(clazz -> void.class.equals(clazz) ? getObjectOrCollectionType(field) : clazz)
        .orElseThrow(() -> new InvalidMappingException(String.format(
            "Missing @%s annotation",
            annotationClass.getSimpleName()
        )));
  }

  public static Class<?> getObjectOrCollectionType(final Field field) {
    final Class<?> fieldType = field.getType();
    if (Collection.class.isAssignableFrom(fieldType)) {
      return getGenericTypeArgument(field);
    }
    return fieldType;
  }

  public static Class<?> getOneToOneTargetEntity(final Field field) {
    return getTargetEntity(field, OneToOne.class, OneToOne::targetEntity);
  }

  public static Class<?> getManyToOneTargetEntity(final Field field) {
    return getTargetEntity(field, ManyToOne.class, ManyToOne::targetEntity);
  }

  public static Class<?> getOneToManyTargetEntity(final Field field) {
    return getTargetEntity(field, OneToMany.class, OneToMany::targetEntity);
  }

  public static Class<?> getManyToManyTargetEntity(final Field field) {
    return getTargetEntity(field, ManyToMany.class, ManyToMany::targetEntity);
  }

  public static String getJoinTableName(final Field field) {
    return field.getAnnotation(JoinTable.class).value();
  }

  public static Field[] getManyToManyFields(final Class<?> clazz) {
    final Field[] fields = getFieldsAnnotatedWith(ManyToMany.class, clazz);
    for (Field field : fields) {
      if (!field.isAnnotationPresent(JoinTable.class)) {
        throw new InvalidMappingException(String.format(
            "Missing @JoinTable annotation in class%s at field %s",
            clazz.getName(),
            field.getName()
        ));
      }
    }
    return fields;
  }

  public static Field[] getToManyFields(final Class<?> clazz) {
    return concatenateArrays(
        Field.class,
        getOneToManyFields(clazz),
        getManyToManyFields(clazz)
    );
  }

  public static <T> Object getColumnValue(
      final Field field,
      final T entity
  ) {
    final Predicate<Field> hasColumnAnnotation = f -> f.isAnnotationPresent(Column.class);
    final Predicate<Field> hasIdAnnotation = f -> f.isAnnotationPresent(Id.class);
    final Predicate<Field> hasJoinColumnAnnotation = f -> f.isAnnotationPresent(JoinColumn.class);
    final Predicate<Field> isColumn = hasColumnAnnotation
        .or(hasJoinColumnAnnotation)
        .or(hasIdAnnotation);
    return Optional.ofNullable(field)
        .filter(isColumn)
        .map(column -> {
          try {
            column.setAccessible(true);
            return column.get(entity);
          } catch (IllegalAccessException e) {
            LOGGER.log(ERROR, e.getLocalizedMessage(), e);
            return null;
          }
        })
        .orElse(null);
  }

  public static <T> Object[] getColumnValues(
      final T entity,
      final Class<?> clazz
  ) {
    return Arrays.stream(getColumns(clazz))
        .map(f -> getColumnValue(f, entity))
        .toArray();
  }

  public static <T> String[] getColumnValuesAsStringForSQL(
      final T entity,
      final Class<?> clazz
  ) {
    return Arrays.stream(getColumns(clazz))
        .map(f -> getColumnValue(f, entity))
        .map(EntityUtils::getStringValueForSQL)
        .toArray(String[]::new);
  }

  public static <T> Object getIdColumnValue(
      final T entity,
      final Class<?> clazz
  ) {
    try {
      final Field idColumn = getIdColumn(clazz);
      idColumn.setAccessible(true);
      return idColumn.get(entity);
    } catch (IllegalAccessException e) {
      LOGGER.log(ERROR, e.getLocalizedMessage(), e);
      return null;
    }
  }

  /**
   * Get {@link String} value for building SQL query.
   *
   * @return string value
   */
  public static String getStringValueForSQL(final Object value) {
    if (value == null) {
      return "NULL";
    } else if (!value.getClass().isArray()) {
      if (value instanceof String ||
          value instanceof Character) {
        return "'" + value + "'";
      }
      return String.valueOf(value);
    } else {
      throw new InvalidColumnTypeException();
    }
  }
}
