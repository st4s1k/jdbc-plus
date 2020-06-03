package st4s1k.jdbcplus.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import st4s1k.jdbcplus.annotations.*;
import st4s1k.jdbcplus.exceptions.InvalidColumnTypeException;
import st4s1k.jdbcplus.exceptions.MissingAnnotationException;
import st4s1k.jdbcplus.repo.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static st4s1k.jdbcplus.repo.TestUtils.*;
import static st4s1k.jdbcplus.utils.EntityUtils.*;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.concatenateArrays;

@ExtendWith(MockitoExtension.class)
class EntityUtilsTest {

  private final List<Entity> entities = getEntities(100, 5);

  private static Entity entity;
  private static Entity1 entity1;
  private static Entity2 entity2;
  private static Entity3 entity3;
  private static Entity4 entity4;

  @BeforeAll
  static void setUp() {
    entity = getEntity(1, "SomeEntity", 5);
    entity1 = getEntity1(10, "SomeEntity1", 6);
    entity2 = getEntity2(20, "SomeEntity2", 7);
    entity3 = getEntity3(30, "SomeEntity3", 8);
    entity4 = getEntity4(40, "SomeEntity4", 9);

    entity.setEntity1s(List.of(entity1));
    entity.setEntity2s(List.of(entity2));
    entity.setEntity4(entity4);

    entity1.setEntity(entity);
    entity1.setEntity2s(List.of(entity2));
    entity1.setEntity3s(List.of(entity3));
    entity1.setEntity4(entity4);

    entity2.setEntity(entity);
    entity2.setEntity3(entity3);

    entity3.setEntity1s(List.of(entity1));
    entity3.setEntity2s(List.of(entity2));

    entity4.setEntity(entity);
    entity4.setEntity1(entity1);
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

  @Test
  void testGetTableNameWhenMissingAnnotationThenThrows() {
    assertThrows(
        MissingAnnotationException.class,
        () -> getTableName(InvalidEntity.class)
    );
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
  void testGetIdColumnWhenIdColumnIsMissing() {
    assertThrows(
        MissingAnnotationException.class,
        () -> getIdColumn(InvalidEntity.class)
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
  void testGetIdColumnNameWhenAnnotationIsMissingThenThrows() {
    assertThrows(
        MissingAnnotationException.class,
        () -> getIdColumnName(InvalidEntity.class)
    );
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
  void testGetOneToOneFields() {
    final var actualOneToOneFields = getOneToOneFields(Entity.class);
    final var expectedOneToOneFields = getFieldsAnnotatedWith(OneToOne.class, Entity.class);
    assertThat(actualOneToOneFields).containsExactly(expectedOneToOneFields);
  }

  @Test
  void testGetOneToManyFields() {
    final var actualOneToManyFields = getOneToManyFields(Entity.class);
    final var expectedManyToOneFields = getFieldsAnnotatedWith(OneToMany.class, Entity.class);
    assertThat(actualOneToManyFields).containsExactly(expectedManyToOneFields);
  }

  @Test
  void testGetManyToOneFields() {
    final var actualManyToOneFields = getManyToOneFields(Entity2.class);
    final var expectedManyToOneFields = getFieldsAnnotatedWith(ManyToOne.class, Entity2.class);
    assertThat(actualManyToOneFields).containsExactly(expectedManyToOneFields);
  }

  @Test
  void testGetManyToManyFields() {
    final var actualManyToManyFields = getManyToManyFields(Entity1.class);
    final var expectedManyToManyFields = getFieldsAnnotatedWith(ManyToMany.class, Entity1.class);
    assertThat(actualManyToManyFields).containsExactly(expectedManyToManyFields);
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

  @ParameterizedTest
  @MethodSource("paramsForGetAnnotation")
  <A extends Annotation> void testGetAnnotation(
      final Field field,
      final Class<A> annotationClass
  ) {
    final var actualAnnotation = getAnnotation(field, annotationClass);
    final var expectedAnnotation = field.getAnnotation(annotationClass);
    assertThat(actualAnnotation).isEqualTo(expectedAnnotation);
  }

  private static Stream<Arguments> paramsForGetAnnotation() throws NoSuchFieldException {
    return Stream.of(
        arguments(Entity.class.getDeclaredField("id"), Id.class),
        arguments(Entity.class.getDeclaredField("rank"), Column.class),
        arguments(Entity.class.getDeclaredField("entity1s"), OneToMany.class),
        arguments(Entity.class.getDeclaredField("entity4"), OneToOne.class),
        arguments(Entity1.class.getDeclaredField("entity"), ManyToOne.class),
        arguments(Entity1.class.getDeclaredField("entity"), JoinColumn.class),
        arguments(Entity1.class.getDeclaredField("entity2s"), ManyToMany.class),
        arguments(Entity1.class.getDeclaredField("entity2s"), JoinTable.class)
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetJoinTable")
  void testGetJoinTable(
      final Field manyToManyField,
      final Field expectedFieldHavingJoinTable
  ) {
    final var actualJoinTable = getJoinTable(manyToManyField);
    final var expectedJoinTable = getAnnotation(expectedFieldHavingJoinTable, JoinTable.class);
    assertThat(actualJoinTable).isEqualTo(expectedJoinTable);
  }

  private static Stream<Arguments> paramsForGetJoinTable() throws NoSuchFieldException {
    final var entity3s = Entity1.class.getDeclaredField("entity3s");
    final var entity2s = Entity1.class.getDeclaredField("entity2s");
    final var entity1s = Entity3.class.getDeclaredField("entity1s");
    return Stream.of(
        arguments(entity3s, entity3s),
        arguments(entity2s, entity2s),
        arguments(entity1s, entity3s)
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetJoinTableName")
  void testGetJoinTableName(final Field field, final String expectedJoinTableName) {
    final var actualJoinTableName = getJoinTableName(field);
    assertThat(actualJoinTableName).isEqualTo(expectedJoinTableName);
  }

  private static Stream<Arguments> paramsForGetJoinTableName() throws NoSuchFieldException {
    final var entity3s = Entity1.class.getDeclaredField("entity3s");
    final var entity2s = Entity1.class.getDeclaredField("entity2s");
    return Stream.of(
        arguments(entity3s, "entity1s_entity3s"),
        arguments(entity2s, "entity1s_entity2s")
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetJoinTableColumnName")
  void testGetJoinTableColumnName(final Class<?> clazz) {
    final var actualJoinTableColumnName = generateJoinTableColumnName(clazz);
    final var expectedJoinTableColumnName = String.format(
        "%s_%s",
        getTableName(clazz),
        getIdColumnName(clazz)
    );
    assertThat(actualJoinTableColumnName).isEqualTo(expectedJoinTableColumnName);
  }

  private static Stream<Arguments> paramsForGetJoinTableColumnName() {
    return Stream.of(
        arguments(Entity.class),
        arguments(Entity1.class),
        arguments(Entity2.class),
        arguments(Entity3.class),
        arguments(Entity4.class)
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetEntityJoinColumnName")
  void testGetEntityJoinColumnName(final Field field, final Class<?> clazz) {
    final var joinTable = getJoinTable(field);
    final var joinColumn = joinTable.joinColumn();
    final var actualEntityJoinColumnName = getEntityJoinColumnName(clazz, joinColumn);
    final var expectedEntityJoinColumnName = joinColumn.value().isEmpty()
        ? generateJoinTableColumnName(clazz)
        : joinColumn.value();
    assertThat(actualEntityJoinColumnName).isEqualTo(expectedEntityJoinColumnName);
  }

  private static Stream<Arguments> paramsForGetEntityJoinColumnName() throws NoSuchFieldException {
    final var entity3s = Entity1.class.getDeclaredField("entity3s");
    final var entity2s = Entity1.class.getDeclaredField("entity2s");
    final var entity1s = Entity3.class.getDeclaredField("entity1s");
    return Stream.of(
        arguments(entity3s, Entity1.class),
        arguments(entity2s, Entity1.class),
        arguments(entity1s, Entity3.class)
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetToManyFields")
  void testGetToManyFields(final Class<?> clazz, final Field[] expectedToManyFields) {
    final var actualToManyFields = getToManyFields(clazz);
    assertThat(actualToManyFields).containsExactlyInAnyOrder(expectedToManyFields);
  }

  private static Stream<Arguments> paramsForGetToManyFields() throws NoSuchFieldException {
    return Stream.of(
        arguments(Entity.class, new Field[]{
            Entity.class.getDeclaredField("entity1s"),
            Entity.class.getDeclaredField("entity2s")
        }),
        arguments(Entity1.class, new Field[]{
            Entity1.class.getDeclaredField("entity2s"),
            Entity1.class.getDeclaredField("entity3s")
        }),
        arguments(Entity2.class, new Field[]{}),
        arguments(Entity3.class, new Field[]{
            Entity3.class.getDeclaredField("entity1s"),
            Entity3.class.getDeclaredField("entity2s")
        }),
        arguments(Entity4.class, new Field[]{})
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetColumnValue")
  void testGetColumnValue(
      final Field field,
      final Object entity,
      final Object expectedColumnValue
  ) {
    final Object actualColumnValue = getColumnValue(field, entity);
    assertThat(actualColumnValue).isEqualTo(expectedColumnValue);
  }

  private static Stream<Arguments> paramsForGetColumnValue() throws NoSuchFieldException {
    return Stream.of(
        arguments(Entity.class.getDeclaredField("id"), entity, entity.getId()),
        arguments(Entity.class.getDeclaredField("name"), entity, entity.getName()),
        arguments(Entity.class.getDeclaredField("rank"), entity, entity.getRank()),
        arguments(Entity.class.getDeclaredField("entity4"), entity, entity.getEntity4()),
        arguments(Entity1.class.getDeclaredField("id"), entity1, entity1.getId()),
        arguments(Entity1.class.getDeclaredField("name"), entity1, entity1.getName()),
        arguments(Entity1.class.getDeclaredField("rank"), entity1, entity1.getRank()),
        arguments(Entity1.class.getDeclaredField("entity"), entity1, entity1.getEntity()),
        arguments(Entity1.class.getDeclaredField("entity4"), entity1, entity1.getEntity4()),
        arguments(Entity2.class.getDeclaredField("id"), entity2, entity2.getId()),
        arguments(Entity2.class.getDeclaredField("name"), entity2, entity2.getName()),
        arguments(Entity2.class.getDeclaredField("rank"), entity2, entity2.getRank()),
        arguments(Entity2.class.getDeclaredField("entity"), entity2, entity2.getEntity()),
        arguments(Entity2.class.getDeclaredField("entity3"), entity2, entity2.getEntity3()),
        arguments(Entity3.class.getDeclaredField("id"), entity3, entity3.getId()),
        arguments(Entity3.class.getDeclaredField("name"), entity3, entity3.getName()),
        arguments(Entity3.class.getDeclaredField("rank"), entity3, entity3.getRank()),
        arguments(Entity4.class.getDeclaredField("id"), entity4, entity4.getId()),
        arguments(Entity4.class.getDeclaredField("name"), entity4, entity4.getName()),
        arguments(Entity4.class.getDeclaredField("rank"), entity4, entity4.getRank()),
        arguments(Entity4.class.getDeclaredField("entity"), entity4, entity4.getEntity()),
        arguments(Entity4.class.getDeclaredField("entity1"), entity4, entity4.getEntity1())
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetColumnValues")
  void testGetColumnValues(
      final Object entity,
      final Class<?> clazz,
      final Object[] expectedColumnValues
  ) {
    final Object[] actualColumnValues = getColumnValues(entity, clazz);
    assertThat(actualColumnValues).containsExactly(expectedColumnValues);
  }

  private static Stream<Arguments> paramsForGetColumnValues() {
    return Stream.of(
        arguments(entity, Entity.class, new Object[]{
            entity.getId(),
            entity.getName(),
            entity.getRank(),
            entity.getEntity4()
        }),
        arguments(entity1, Entity1.class, new Object[]{
            entity1.getId(),
            entity1.getName(),
            entity1.getRank(),
            entity1.getEntity(),
            entity1.getEntity4()
        }),
        arguments(entity2, Entity2.class, new Object[]{
            entity2.getId(),
            entity2.getName(),
            entity2.getRank(),
            entity2.getEntity(),
            entity2.getEntity3()
        }),
        arguments(entity3, Entity3.class, new Object[]{
            entity3.getId(),
            entity3.getName(),
            entity3.getRank()
        }),
        arguments(entity4, Entity4.class, new Object[]{
            entity4.getId(),
            entity4.getName(),
            entity4.getRank(),
            entity4.getEntity(),
            entity4.getEntity1()
        })
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetColumnValuesAsStringForSQL")
  void testGetColumnValuesAsStringForSQL(
      final Object entity,
      final Class<?> clazz,
      final Object[] expectedColumnValues
  ) {
    final Object[] actualColumnValues = getColumnValuesAsStringForSQL(entity, clazz);
    assertThat(actualColumnValues).containsExactly(expectedColumnValues);
  }

  private static Stream<Arguments> paramsForGetColumnValuesAsStringForSQL() {
    return Stream.of(
        arguments(entity, Entity.class, new Object[]{
            getStringValueForSql(entity.getId()),
            getStringValueForSql(entity.getName()),
            getStringValueForSql(entity.getRank()),
            getStringValueForSql(entity.getEntity4().getId())
        }),
        arguments(entity1, Entity1.class, new Object[]{
            getStringValueForSql(entity1.getId()),
            getStringValueForSql(entity1.getName()),
            getStringValueForSql(entity1.getRank()),
            getStringValueForSql(entity1.getEntity().getId()),
            getStringValueForSql(entity1.getEntity4().getId())
        }),
        arguments(entity2, Entity2.class, new Object[]{
            getStringValueForSql(entity2.getId()),
            getStringValueForSql(entity2.getName()),
            getStringValueForSql(entity2.getRank()),
            getStringValueForSql(entity2.getEntity().getId()),
            getStringValueForSql(entity2.getEntity3().getId())
        }),
        arguments(entity3, Entity3.class, new Object[]{
            getStringValueForSql(entity3.getId()),
            getStringValueForSql(entity3.getName()),
            getStringValueForSql(entity3.getRank())
        }),
        arguments(entity4, Entity4.class, new Object[]{
            getStringValueForSql(entity4.getId()),
            getStringValueForSql(entity4.getName()),
            getStringValueForSql(entity4.getRank()),
            getStringValueForSql(entity4.getEntity().getId()),
            getStringValueForSql(entity4.getEntity1().getId())
        })
    );
  }

  @Test
  void testGetIdColumnValue() {
    final var entity = getEntity(1, "SomeEntity", 5);
    final var actualIdColumnValue = getIdColumnValue(entity);
    assertThat(actualIdColumnValue).isEqualTo(entity.getId());
  }

  @ParameterizedTest
  @MethodSource("paramsForGetStringValueForSql")
  void testGetStringValueForSql(final Object value, final String expectedValue) {
    final var stringValue = getStringValueForSql(value);
    assertThat(stringValue).isEqualTo(expectedValue);
  }

  private static Stream<Arguments> paramsForGetStringValueForSql() {
    return Stream.of(
        arguments(1, "1"),
        arguments('a', "'a'"),
        arguments("1", "'1'"),
        arguments(null, "NULL"),
        arguments("abc", "'abc'")
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForGetStringValueForSqlWhenInvalidTypeThenThrow")
  void testGetStringValueForSqlWhenInvalidTypeThenThrow(final Object value) {
    assertThrows(
        InvalidColumnTypeException.class,
        () -> getStringValueForSql(value)
    );
  }

  @SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
  private static Stream<Arguments> paramsForGetStringValueForSqlWhenInvalidTypeThenThrow() {
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