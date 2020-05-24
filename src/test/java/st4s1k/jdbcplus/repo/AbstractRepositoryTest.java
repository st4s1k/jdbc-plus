package st4s1k.jdbcplus.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import st4s1k.jdbcplus.config.DatabaseConnection;
import st4s1k.jdbcplus.utils.EntityUtils;

import java.lang.reflect.Field;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static st4s1k.jdbcplus.utils.EntityUtils.*;

@ExtendWith(MockitoExtension.class)
class AbstractRepositoryTest {

  @Mock
  private DatabaseConnection databaseConnection;

  private Entity entity;
  private String entityTableName;

  private AbstractRepository abstractRepository;

  @BeforeEach
  void setup() {
    abstractRepository = TestUtils.spyLambda(AbstractRepository.class, () -> databaseConnection);
    entity = new Entity();
    entity.setId(1);
    entity.setName("SomeEntity");
    entityTableName = EntityUtils.getTableName(entity.getClass());
  }

  @ParameterizedTest
  @MethodSource("parametersForGetStringValueForSql")
  void testGetStringValueForSql(final Object value, final String expectedValue) {
    final var stringValue = abstractRepository.getStringValueForSql(value);
    assertThat(stringValue).isEqualTo(expectedValue);
  }

  private static Stream<Arguments> parametersForGetStringValueForSql() {
    return Stream.of(
        arguments(1, "1"),
        arguments("1", "'1'"),
        arguments(null, "NULL"),
        arguments(new long[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new int[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new short[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new char[]{'a', 'b', 'c'}, "['a', 'b', 'c']"),
        arguments(new byte[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new boolean[]{true, false, true}, "[true, false, true]"),
        arguments(new float[]{1.0f, 2.0f, 3.0f}, "[1.0, 2.0, 3.0]"),
        arguments(new double[]{1.0d, 2.0d, 3.0d}, "[1.0, 2.0, 3.0]"),
        arguments(new String[]{"a", "b", "c"}, "['a', 'b', 'c']"),
        arguments("abc", "'abc'")
    );
  }

  @Test
  void testSqlRemove() {
    final var result = abstractRepository.sqlRemove(entity, Entity.class);
    assertThat(result).isEqualTo("delete from " + entityTableName
        + " where id = " + entity.getId()
        + " returning *");
  }

  @Test
  void testSqlInsert() {
    final var result = abstractRepository.sqlInsert(entity, Entity.class);
    assertThat(result).isEqualTo("insert into " + entityTableName
        + "(id, name) values "
        + "(" + entity.getId() + ", '" + entity.getName() + "') "
        + "returning *");
  }

  @Test
  void testSqlUpdate() {
    final var result = abstractRepository.sqlUpdate(entity, Entity.class);
    assertThat(result).isEqualTo(
        "update " + entityTableName + " set"
            + " name = '" + entity.getName() + "'"
            + " where id = " + entity.getId()
            + " returning *");
  }

  @Test
  void testSqlSelectAll() {
    final var result = abstractRepository.sqlSelectAll(entityTableName);
    assertThat(result).isEqualTo("select * from " + entityTableName);
  }

  @Test
  void testSqlSelectAllByColumn() {
    final var column = "name";
    final var value = "SomeEntity";
    final var result = abstractRepository.sqlSelectAllByColumn(
        entityTableName,
        column,
        value
    );

    final var expectedQuery = "select * from "
        + entityTableName + " where "
        + column + " = '" + value + "'";

    assertThat(result).isEqualTo(expectedQuery);
  }

  @Test
  void testSqlSelectAllByColumns() {
    String column1 = "column1";
    String column2 = "column2";
    String value1 = "value1";
    String value2 = "value2";
    final var result = abstractRepository.sqlSelectAllByColumns(
        entityTableName,
        new String[]{column1, column2},
        new Object[]{value1, value2}
    );
    final var expectedQuery = "select * from "
        + entityTableName + " where "
        + column1 + " = '" + value1 + "', "
        + column2 + " = '" + value2 + "'";
    assertThat(result).isEqualTo(expectedQuery);
  }

  @Test
  void testSave() {
    abstractRepository.save(entity, Entity.class);
    final var expectedQuery = abstractRepository.sqlInsert(entity, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testUpdate() {
    final var selectQuery = abstractRepository.sqlSelectAllByColumn(
        entityTableName,
        "id",
        entity.getId());
    final var expectedQuery = abstractRepository.sqlUpdate(entity, Entity.class);
    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));
    when(databaseConnection.queryTransaction(eq(expectedQuery), any(), any()))
        .thenReturn(Optional.of(entity));
    abstractRepository.update(entity, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testRemove() {
    final var selectQuery = abstractRepository.sqlSelectAllByColumn(
        entityTableName,
        "id",
        entity.getId());
    final var expectedQuery = abstractRepository.sqlRemove(entity, Entity.class);
    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));
    when(databaseConnection.queryTransaction(eq(expectedQuery), any(), any()))
        .thenReturn(Optional.of(entity));
    abstractRepository.remove(entity, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFind() {
    abstractRepository.find(entity, Entity.class);
    final var expectedQuery = abstractRepository.sqlSelectAllByColumns(
        entityTableName,
        getColumnNames(Entity.class),
        getColumnValues(entity, Entity.class));
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindAll() {
    abstractRepository.findAll(Entity.class);
    final var expectedQuery = abstractRepository.sqlSelectAll(entityTableName);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindByColumn() {
    final var column = "name";
    final var value = "SomeEntity";
    abstractRepository.findByColumn(column, value, Entity.class);
    final var expectedQuery = abstractRepository.sqlSelectAllByColumn(
        entityTableName,
        column,
        value);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindById() {
    final var entityId = entity.getId();
    final var expectedQuery = abstractRepository.sqlSelectAllByColumn(
        entityTableName,
        "id",
        entityId);
    when(databaseConnection.queryTransaction(eq(expectedQuery), any(), any()))
        .thenReturn(List.of(entity));
    abstractRepository.findById(entityId, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testGetObject() {
    final var entityResultSet = TestUtils.getResultSet(entity);
    final var result = abstractRepository.getObject(entityResultSet, Entity.class);
    assertThat(result).isEqualTo(entity);
  }

  @Test
  void testGetObjects() throws SQLException {
    final var entityResultSet = TestUtils.getResultSet(entity);
    when(entityResultSet.next())
        .thenReturn(true)
        .thenReturn(false);
    final var result = abstractRepository.getObjects(entityResultSet, Entity.class);
    assertThat(result).containsOnly(entity);
  }

  @Test
  void testPopulateByColumnsMap() throws SQLException, IllegalAccessException, NoSuchFieldException {
    final var entityResultSet = TestUtils.getResultSet(this.entity);
    final var columnsMap = new HashMap<String, Field>();
    columnsMap.put("id", getIdColumn(Entity.class));
    columnsMap.put("name", Entity.class.getDeclaredField("name"));
    final var entity = new Entity();
    abstractRepository.populateByColumnsMap(entityResultSet, entity, columnsMap);
    assertThat(entity).isEqualTo(this.entity);
  }

  @Test
  void testPopulateColumnFields() throws SQLException, IllegalAccessException {
    final var entityResultSet = TestUtils.getResultSet(this.entity);
    final var entity = new Entity();
    abstractRepository.populateColumnFields(entityResultSet, entity, Entity.class);
    assertThat(entity).isEqualTo(this.entity);
  }

  @Test
  void testPopulateOneToManyField() throws IllegalAccessException, NoSuchFieldException {
    final var query = "query";
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity));
    abstractRepository.populateOneToManyField(
        Entity.class.getDeclaredField("entities"),
        entity,
        Entity.class);
    assertThat(entity.getEntities()).containsOnly(entity);
  }

  @Test
  void testPopulateField() throws IllegalAccessException {
    final var query = "query";
    when(databaseConnection.queryTransaction(eq(query), any(), any()))
        .thenReturn(2);
    abstractRepository.populateField(
        getIdColumn(Entity.class),
        entity,
        "query",
        null,
        null);
    assertThat(entity.getId()).isEqualTo(2);
  }

  @Test
  void testPopulateOneToManyFields() throws IllegalAccessException {
    abstractRepository.populateOneToManyFields(entity, null);
    assertThat(true).isFalse();
  }

  @Test
  void testPopulateManyToManyField() throws IllegalAccessException {
    abstractRepository.populateManyToManyField(entity, null, null);
    assertThat(true).isFalse();
  }

  @Test
  void testFetchRelatedEntities() {
    final var result = abstractRepository.fetchRelatedEntities(null,
        "currentEntityIdColumnName", "targetEntityIdColumnName", null);
    assertThat(result).isEqualTo(singletonList(entity));
    assertThat(true).isFalse();
  }

  @ParameterizedTest
  @MethodSource("parametersForValidManyToManyResultSet")
  void testValidManyToManyResultSet(
      final int columnCount,
      final String actualCurrentEntityIdColumnName,
      final String actualTargetEntityIdColumnName,
      final String expectedCurrentEntityIdColumnNameBar,
      final String expectedTargetEntityIdColumnNameBar,
      final boolean expectedResult
  ) throws SQLException {
    final var metaData = mock(ResultSetMetaData.class);

    when(metaData.getColumnCount()).thenReturn(columnCount);
    lenient().when(metaData.getColumnName(1)).thenReturn(actualCurrentEntityIdColumnName);
    lenient().when(metaData.getColumnName(2)).thenReturn(actualTargetEntityIdColumnName);

    final var actualResult = abstractRepository.validManyToManyResultSet(
        metaData,
        expectedCurrentEntityIdColumnNameBar,
        expectedTargetEntityIdColumnNameBar
    );

    assertThat(actualResult).isEqualTo(expectedResult);
  }

  private static Stream<Arguments> parametersForValidManyToManyResultSet() {
    final var validNumberOfColumns = 2;
    final var validActualCurrentEntityIdColumnName = "currentEntityIdColumnName";
    final var validActualTargetEntityIdColumnName = "targetEntityIdColumnName";
    final var validExpectedCurrentEntityIdColumnName = "currentEntityIdColumnName";
    final var validExpectedTargetEntityIdColumnName = "targetEntityIdColumnName";
    return Stream.of(
        arguments(
            validNumberOfColumns,
            validActualCurrentEntityIdColumnName,
            validActualTargetEntityIdColumnName,
            validExpectedCurrentEntityIdColumnName,
            validExpectedTargetEntityIdColumnName,
            true
        ),
        arguments(
            validNumberOfColumns,
            validActualCurrentEntityIdColumnName,
            validActualTargetEntityIdColumnName,
            validExpectedTargetEntityIdColumnName,
            validExpectedCurrentEntityIdColumnName,
            true
        ),
        arguments(
            1,
            validActualCurrentEntityIdColumnName,
            validActualTargetEntityIdColumnName,
            validExpectedCurrentEntityIdColumnName,
            validExpectedTargetEntityIdColumnName,
            false
        ),
        arguments(
            3,
            validActualCurrentEntityIdColumnName,
            validActualTargetEntityIdColumnName,
            validExpectedCurrentEntityIdColumnName,
            validExpectedTargetEntityIdColumnName,
            false
        ),
        arguments(
            validNumberOfColumns,
            "invalidActualCurrentEntityIdColumnName",
            validActualTargetEntityIdColumnName,
            validExpectedCurrentEntityIdColumnName,
            validExpectedTargetEntityIdColumnName,
            false
        ),
        arguments(
            validNumberOfColumns,
            validActualCurrentEntityIdColumnName,
            "invalidActualTargetEntityIdColumnName",
            validExpectedCurrentEntityIdColumnName,
            validExpectedTargetEntityIdColumnName,
            false
        ),
        arguments(
            validNumberOfColumns,
            "invalidActualCurrentEntityIdColumnName",
            "invalidActualTargetEntityIdColumnName",
            validExpectedCurrentEntityIdColumnName,
            validExpectedTargetEntityIdColumnName,
            false
        )
    );
  }

  @Test
  void testPopulateManyToManyFields() throws IllegalAccessException {
    abstractRepository.populateManyToManyFields(entity, null);
    assertThat(true).isFalse();
  }
}