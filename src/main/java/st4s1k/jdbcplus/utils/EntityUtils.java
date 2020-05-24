package st4s1k.jdbcplus.utils;

import st4s1k.jdbcplus.annotations.*;
import st4s1k.jdbcplus.exceptions.InvalidMappingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.concatenateArrays;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.toSnakeLowerCase;

public class EntityUtils {

  private EntityUtils() {
  }

  public static Field[] getFieldsAnnotatedWith(
      final Class<? extends Annotation> annotation,
      final Class<?> clazz) {
    return Optional.ofNullable(clazz)
        .map(c -> Arrays.stream(c.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(annotation))
            .toArray(Field[]::new))
        .orElseGet(() -> new Field[]{});
  }

  public static Map<String, Field> getFieldsMap(
      final Field[] fields,
      final Function<Field, String> keyGenerator) {
    return Arrays.stream(fields).collect(toMap(keyGenerator, field -> field));
  }

  public static String getTableName(final Class<?> clazz) {
    if (Optional.ofNullable(clazz).isPresent() &&
        !clazz.isAnnotationPresent(Table.class)) {
      throw new InvalidMappingException(
          "Missing @Table annotation in class " + clazz.getName());
    }
    return Optional.ofNullable(clazz)
        .map(c -> c.getAnnotation(Table.class))
        .map(Table::value)
        .orElse("");
  }

  public static Field getIdColumn(final Class<?> clazz) {
    final var idFields = Arrays.asList(getFieldsAnnotatedWith(Id.class, clazz));
    if (idFields.isEmpty()) {
      throw new InvalidMappingException(
          "Missing @Id annotation in class " + clazz.getName());
    }
    if (idFields.size() > 1) {
      throw new InvalidMappingException(
          "Too many @Id annotated columns in class " + clazz.getName());
    }
    return idFields.get(0);
  }

  public static String getIdColumnName(final Class<?> clazz) {
    final var idColumn = getIdColumn(clazz);
    return idColumn.isAnnotationPresent(Column.class)
        ? idColumn.getAnnotation(Column.class).value()
        : idColumn.getName();
  }

  public static Field[] getColumns(final Class<?> clazz) {
    return concatenateArrays(
        Field.class,
        getColumnFields(clazz),
        getOneToOneFields(clazz),
        getManyToOneFields(clazz));
  }

  public static Field[] getColumnFields(final Class<?> clazz) {
    return getFieldsAnnotatedWith(Column.class, clazz);
  }

  public static String getColumnName(final Field field) {
    return Optional.ofNullable(field)
        .filter(f -> f.isAnnotationPresent(Column.class))
        .map(f -> f.getAnnotation(Column.class).value())
        .orElse(getJoinColumnName(field));
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
    } else {
      return toSnakeLowerCase(field.getName());
    }
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

  public static Field[] getOneToManyFields(final Class<?> clazz) {
    return getFieldsAnnotatedWith(OneToMany.class, clazz);
  }

  public static <X> Class<X> getTargetEntity(final Field field) {
    final Optional<Class<X>> oneToOneTargetEntity =
        Optional.ofNullable(getOneToOneTargetEntity(field));
    final Optional<Class<X>> manyToOneTargetEntity =
        Optional.ofNullable(getManyToOneTargetEntity(field));
    final Optional<Class<X>> oneToManyTargetEntity =
        Optional.ofNullable(getOneToManyTargetEntity(field));
    final Optional<Class<X>> manyToManyTargetEntity =
        Optional.ofNullable(getManyToManyTargetEntity(field));

    return oneToOneTargetEntity
        .or(() -> manyToOneTargetEntity)
        .or(() -> oneToManyTargetEntity)
        .or(() -> manyToManyTargetEntity)
        .orElse(null);

  }

  @SuppressWarnings("unchecked")
  public static <X> Class<X> getOneToOneTargetEntity(final Field field) {
    return (Class<X>) Optional.ofNullable(field)
        .filter(f -> f.isAnnotationPresent(OneToOne.class))
        .map(f -> f.getAnnotation(OneToOne.class))
        .map(OneToOne::targetEntity)
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  public static <X> Class<X> getManyToOneTargetEntity(final Field field) {
    return (Class<X>) Optional.ofNullable(field)
        .filter(f -> f.isAnnotationPresent(ManyToOne.class))
        .map(f -> f.getAnnotation(ManyToOne.class))
        .map(ManyToOne::targetEntity)
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  public static <X> Class<X> getOneToManyTargetEntity(final Field field) {
    return (Class<X>) Optional.ofNullable(field)
        .filter(f -> f.isAnnotationPresent(OneToMany.class))
        .map(f -> f.getAnnotation(OneToMany.class))
        .map(OneToMany::targetEntity)
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private static <X> Class<X> getManyToManyTargetEntity(Field field) {
    return (Class<X>) Optional.ofNullable(field)
        .filter(f -> f.isAnnotationPresent(ManyToMany.class))
        .map(f -> f.getAnnotation(ManyToMany.class))
        .map(ManyToMany::targetEntity)
        .orElse(null);
  }

  public static String getJoinTableName(final Field field) {
    return field.getAnnotation(JoinTable.class).value();
  }

  public static Field[] getManyToManyFields(final Class<?> clazz) {
    final Field[] fields = getFieldsAnnotatedWith(ManyToMany.class, clazz);
    for (Field field : fields) {
      if (!field.isAnnotationPresent(JoinTable.class)) {
        throw new InvalidMappingException(
            "Missing @JoinTable annotation in class" + clazz +
                " on field " + field.getName());
      }
    }
    return fields;
  }

  public static Field[] getToManyFields(final Class<?> clazz) {
    return concatenateArrays(
        Field.class,
        getOneToManyFields(clazz),
        getManyToManyFields(clazz));
  }

  public static <T> Object getColumnValue(
      final Field field,
      final T entity) {
    return Optional.ofNullable(field)
        .filter(f -> f.isAnnotationPresent(Column.class))
        .map(column -> {
          try {
            column.setAccessible(true);
            return column.get(entity);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
          }
        }).orElse(null);
  }

  public static <T> Object[] getColumnValues(
      final T entity,
      final Class<?> clazz) {
    return Arrays.stream(getColumns(clazz))
        .map(f -> getColumnValue(f, entity))
        .toArray();
  }

  public static <T> String[] getColumnValuesAsString(
      final T entity,
      final Class<?> clazz) {
    return Arrays.stream(getColumns(clazz))
        .map(f -> getColumnValue(f, entity))
        .map(v -> v instanceof String
            ? "'" + v + "'"
            : v.toString())
        .toArray(String[]::new);
  }

  public static <T> Object getIdColumnValue(
      final T entity,
      final Class<?> clazz) {
    try {
      final var idColumn = getIdColumn(clazz);
      idColumn.setAccessible(true);
      return idColumn.get(entity);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return null;
    }
  }
}