package st4s1k.jdbcplus.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import st4s1k.jdbcplus.annotations.*;
import st4s1k.jdbcplus.exceptions.InvalidColumnTypeException;
import st4s1k.jdbcplus.exceptions.MissingAnnotationException;
import st4s1k.jdbcplus.repo.Entity;
import st4s1k.jdbcplus.repo.Entity1;
import st4s1k.jdbcplus.repo.Entity2;
import st4s1k.jdbcplus.repo.Entity3;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static st4s1k.jdbcplus.repo.TestUtils.getEntity;
import static st4s1k.jdbcplus.utils.EntityUtils.*;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.concatenateArrays;

@ExtendWith(MockitoExtension.class)
class EntityUtilsTest {

  private List<Entity> entities;

  @BeforeEach
  void setUp() {
  }

  @Test
  void testGetGenericTypeArgument() throws NoSuchFieldException {
    final var field = this.getClass().getDeclaredField("entities");
    final var genericTypeArgument = getGenericTypeArgument(field);
    assertThat(genericTypeArgument).isEqualTo(Entity.class);
  }

  @Test
  void testGetField() {
    final var fieldName = "name";
    final var field = getField(Entity.class, fieldName);
    assertThat(field.getName()).isEqualTo(fieldName);
    assertThat(field.getType()).isEqualTo(String.class);
  }

  @Test
  void testGetFieldWhenFieldDoesNotExistThenThrows() {
    final var fieldName = "bazinga";
    final var exception = assertThrows(
        RuntimeException.class,
        () -> getField(Entity.class, fieldName)
    );
    assertThat(exception.getCause()).isInstanceOf(NoSuchFieldException.class);
  }

  @ParameterizedTest
  @MethodSource("paramsForGetFieldsAnnotatedWith")
  void testGetFieldsAnnotatedWith(
      final Class<?> clazz,
      final Class<? extends Annotation> annotationClass,
      final List<Field> expectedFields
  ) {
    final var fields = getFieldsAnnotatedWith(annotationClass, clazz);
    assertThat(fields).containsExactly(expectedFields.toArray(Field[]::new));
  }

  private static Stream<Arguments> paramsForGetFieldsAnnotatedWith()
      throws NoSuchFieldException {
    return Stream.of(
        arguments(
            Entity.class,
            Id.class,
            List.of(Entity.class.getDeclaredField("id"))
        ),
        arguments(
            Entity.class,
            Column.class,
            List.of(
                Entity.class.getDeclaredField("name"),
                Entity.class.getDeclaredField("rank")
            )
        ),
        arguments(
            Entity.class,
            OneToMany.class,
            List.of(
                Entity.class.getDeclaredField("entity1s"),
                Entity.class.getDeclaredField("entity2s")
            )
        )
    );
  }

  @Test
  void testGetFieldsMap() {
    final var fields = Entity.class.getDeclaredFields();
    final var fieldsMap = getFieldsMap(fields, Field::getName);
    final var expectedFieldsMap = Arrays.stream(fields).collect(toMap(
        Field::getName,
        field -> field
    ));
    assertThat(fieldsMap).containsExactlyEntriesOf(expectedFieldsMap);
  }

  @Test
  void testGetTableName() {
    final var tableName = getTableName(Entity.class);
    final var expectedTableName = Entity.class.getAnnotation(Table.class).value();
    assertThat(tableName).isEqualTo(expectedTableName);
  }

  @ParameterizedTest
  @MethodSource("paramsForGetIdColumn")
  void testGetIdColumn(
      final Class<?> clazz,
      final String idFieldName
  ) throws NoSuchFieldException {
    final var idColumn = getIdColumn(clazz);
    assertThat(idColumn).isEqualTo(clazz.getDeclaredField(idFieldName));
  }

  private static Stream<Arguments> paramsForGetIdColumn() {
    return Stream.of(
        arguments(Entity.class, "id"),
        arguments(Entity1.class, "id"),
        arguments(Entity2.class, "id"),
        arguments(Entity3.class, "id")
    );
  }

  @Test
  void testGetIdColumnNameByField() {
    final var field = getFieldsAnnotatedWith(Id.class, Entity.class)[0];
    final var idColumnName = getIdColumnName(field);
    assertThat(idColumnName).isEqualTo(field.getName());
  }

  @Test
  void testGetIdColumnNameByClass() {
    final var field = getFieldsAnnotatedWith(Id.class, Entity.class)[0];
    final var idColumnName = getIdColumnName(Entity.class);
    assertThat(idColumnName).isEqualTo(field.getName());
  }

  @Test
  void testGetColumns() {
    final var clazz = Entity.class;
    final var columns = getColumns(clazz);
    assertThat(columns).containsExactly(concatenateArrays(
        Field.class,
        new Field[]{getIdColumn(clazz)},
        getColumnFields(clazz),
        getOneToOneFields(clazz),
        getManyToOneFields(clazz)
    ));
  }

  @Test
  void testGetColumnFields() {
    final var columnFields = getColumnFields(Entity.class);
    final var expectedColumnFields = getFieldsAnnotatedWith(Column.class, Entity.class);
    assertThat(columnFields).containsExactly(expectedColumnFields);
  }

  @Test
  void testGetColumnName() throws NoSuchFieldException {
    final var expectedColumnName = "rank";
    final var actualColumnName = getColumnName(Entity.class.getDeclaredField(expectedColumnName));
    assertThat(actualColumnName).isEqualTo(expectedColumnName);
  }

  @Test
  void testGetColumnNames() {
    final var actualColumnNames = getColumnNames(Entity.class);
    final var expectedColumnNames = Arrays.stream(getColumns(Entity.class))
        .map(EntityUtils::getColumnName)
        .toArray(String[]::new);
    assertThat(actualColumnNames).containsExactly(expectedColumnNames);
  }

  @Test
  void testGetJoinColumnName() throws NoSuchFieldException {
    final var field = Entity1.class.getDeclaredField("entity");
    final var actualJoinColumnName = getJoinColumnName(field);
    final var expectedJoinColumnName = getAnnotation(field, JoinColumn.class).value();
    assertThat(actualJoinColumnName).isEqualTo(expectedJoinColumnName);
  }

  @Test
  void testGetJoinColumnNameWhenFieldDoesNotContainJoinColumnThenThrows()
      throws NoSuchFieldException {
    final var field = Entity.class.getDeclaredField("rank");
    assertThrows(
        MissingAnnotationException.class,
        () -> getJoinColumnName(field)
    );
  }

  @Test
  void testGetColumnsMap() {
    final var actualColumnsMap = getColumnsMap(Entity.class);
    final var expectedColumnsMap = getFieldsMap(
        getColumns(Entity.class),
        EntityUtils::getColumnName
    );
    assertThat(actualColumnsMap).containsExactlyEntriesOf(expectedColumnsMap);
  }

  @Test
  @Disabled
  void testGetOneToOneFields() {
    final var oneToOneFields = getOneToOneFields(Entity.class);
  }

  @Test
  void testGetManyToOneFields() {
    final var actualManyToOneFields = getManyToOneFields(Entity.class);
    final var expectedManyToOneFields = getFieldsAnnotatedWith(ManyToOne.class, Entity.class);
    assertThat(actualManyToOneFields).containsExactly(expectedManyToOneFields);
  }

  @Test
  void testGetRelationalField() throws NoSuchFieldException {
    final var actualRelationalField = getRelationalField(
        Entity.class,
        Entity2.class,
        OneToMany.class
    );
    final var expectedRelationalField = Entity.class.getDeclaredField("entity2s");
    assertThat(actualRelationalField).isEqualTo(expectedRelationalField);
  }

  @Test
  @Disabled
  void testGetOneToManyFields() {

  }

  @Test
  void testGetTargetEntityByField() throws NoSuchFieldException {
    final var field = Entity.class.getDeclaredField("entity1s");
    final var actualTargetEntity = getTargetEntity(field);
    final var expectedTargetEntity = Entity1.class;
    assertThat(actualTargetEntity).isEqualTo(expectedTargetEntity);
  }

  @ParameterizedTest
  @MethodSource("paramsForGetTargetEntityByFieldOrAnnotation")
  <A extends Annotation> void testGetTargetEntityByFieldOrAnnotation(
      final Field field,
      final Class<A> annotationClass,
      final TargetEntitySupplier<A> targetEntitySupplier,
      final Class<?> expectedTargetEntity
  ) {
    final var actualTargetEntity = getTargetEntity(
        field,
        annotationClass,
        targetEntitySupplier.get()
    );
    assertThat(actualTargetEntity).isEqualTo(expectedTargetEntity);
  }

  private static Stream<Arguments> paramsForGetTargetEntityByFieldOrAnnotation()
      throws NoSuchFieldException {
    return Stream.of(
        arguments(
            Entity.class.getDeclaredField("entity1s"),
            OneToMany.class,
            TargetEntitySupplier.of(OneToMany::targetEntity),
            Entity1.class
        ),
        arguments(
            Entity.class.getDeclaredField("entity2s"),
            OneToMany.class,
            TargetEntitySupplier.of(OneToMany::targetEntity),
            Entity2.class
        ),
        arguments(
            Entity1.class.getDeclaredField("entity2s"),
            ManyToMany.class,
            TargetEntitySupplier.of(ManyToMany::targetEntity),
            Entity2.class
        ),
        arguments(
            Entity1.class.getDeclaredField("entity3s"),
            ManyToMany.class,
            TargetEntitySupplier.of(ManyToMany::targetEntity),
            Entity3.class
        ),
        arguments(
            Entity2.class.getDeclaredField("entity"),
            ManyToOne.class,
            TargetEntitySupplier.of(ManyToOne::targetEntity),
            Entity.class
        ),
        arguments(
            Entity2.class.getDeclaredField("entity3"),
            ManyToOne.class,
            TargetEntitySupplier.of(ManyToOne::targetEntity),
            Entity3.class
        )
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetObjectOrCollectionType")
  void testGetObjectOrCollectionType(
      final Field field,
      final Object expectedType
  ) {
    final var actualType = getObjectOrCollectionType(field);
    assertThat(actualType).isEqualTo(expectedType);
  }

  private static Stream<Arguments> paramsForGetObjectOrCollectionType()
      throws NoSuchFieldException {
    return Stream.of(
        arguments(
            Entity.class.getDeclaredField("id"),
            Integer.class
        ),
        arguments(
            Entity.class.getDeclaredField("name"),
            String.class
        ),
        arguments(
            Entity.class.getDeclaredField("rank"),
            Integer.class
        ),
        arguments(
            Entity.class.getDeclaredField("entity1s"),
            Entity1.class
        ),
        arguments(
            Entity.class.getDeclaredField("entity2s"),
            Entity2.class
        )
    );
  }

  @Test
  @Disabled
  void testGetAnnotation() {

  }

  @Test
  @Disabled
  void testGetJoinTable() {

  }

  @Test
  @Disabled
  void testGetJoinTableName() {

  }

  @Test
  @Disabled
  void testGetJoinTableColumnName() {

  }

  @Test
  @Disabled
  void testGetEntityJoinColumnName() {

  }

  @Test
  @Disabled
  void testGetManyToManyFields() {

  }

  @Test
  @Disabled
  void testGetToManyFields() {

  }

  @Test
  @Disabled
  void testGetColumnValue() {

  }

  @Test
  @Disabled
  void testGetColumnValues() {

  }

  @Test
  @Disabled
  void testGetColumnValuesAsStringForSQL() {

  }

  @Test
  @Disabled
  void testGetIdColumnValue() {
    final var entity = getEntity(
        1, "SomeEntity", 5,
        emptyList(), emptyList()
    );
    final Object actualIdColumnValue = getIdColumnValue(entity);
    assertThat(actualIdColumnValue).isEqualTo(entity.getId());
  }

  @Test
  @Disabled
  void testGetStringValueForSQL() {

  }

  @ParameterizedTest
  @MethodSource("parametersForGetStringValueForSql")
  void testGetStringValueForSql(final Object value, final String expectedValue) {
    final var stringValue = getStringValueForSQL(value);
    assertThat(stringValue).isEqualTo(expectedValue);
  }

  private static Stream<Arguments> parametersForGetStringValueForSql() {
    return Stream.of(
        arguments(1, "1"),
        arguments('a', "'a'"),
        arguments("1", "'1'"),
        arguments(null, "NULL"),
        arguments("abc", "'abc'")
    );
  }

  @ParameterizedTest
  @MethodSource("parametersGetStringValueForSqlWhenInvalidTypeThenThrow")
  void testGetStringValueForSqlWhenInvalidTypeThenThrow(final Object value) {
    assertThrows(
        InvalidColumnTypeException.class,
        () -> getStringValueForSQL(value)
    );
  }

  private static Stream<Arguments> parametersGetStringValueForSqlWhenInvalidTypeThenThrow() {
    return Stream.of(
        arguments(new long[]{1, 2, 3}),
        arguments(new int[]{1, 2, 3}),
        arguments(new short[]{1, 2, 3}),
        arguments(new char[]{'a', 'b', 'c'}),
        arguments(new byte[]{1, 2, 3}),
        arguments(new boolean[]{true, false, true}),
        arguments(new float[]{1.0f, 2.0f, 3.0f}),
        arguments(new double[]{1.0d, 2.0d, 3.0d}),
        arguments((Object) new String[]{"a", "b", "c"})
    );
  }
}