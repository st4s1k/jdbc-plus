package st4s1k.jdbcplus.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.util.Arrays;
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
class AbstractJdbcPlusRepositoryTest {

  @Mock
  private DatabaseConnection databaseConnection;

  private Entity entity;
  private String entityTableName;

  private AbstractJdbcPlusRepository abstractJdbcPlusRepository;

  @BeforeEach
  void setup() {
    abstractJdbcPlusRepository = TestUtils.spyLambda(
        AbstractJdbcPlusRepository.class,
        () -> databaseConnection
    );
    entity = getEntity(1, "SomeEntity", 5);
    entityTableName = EntityUtils.getTableName(entity.getClass());
  }

  private Entity getEntity(
      final int id,
      final String name,
      final int rank,
      final Entity... entities
  ) {
    final var newEntity = new Entity();
    newEntity.setId(id);
    newEntity.setName(name);
    newEntity.setRank(rank);
    if (entities.length > 0) {
      newEntity.setEntities(Arrays.asList(entities));
    }
    return newEntity;
  }

  @Test
  void testSqlRemove() {
    final var result = abstractJdbcPlusRepository.sqlRemove(entity, Entity.class);
    final var expectedStringTemplate = "delete from %s where id = %d returning *";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName,
        entity.getId()
    );
  }

  @Test
  void testSqlInsert() {
    final var result = abstractJdbcPlusRepository.sqlInsert(entity, Entity.class);
    final var expectedStringTemplate =
        "insert into %s(id, name, rank) values (%d, '%s', %d) returning *";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName,
        entity.getId(),
        entity.getName(),
        entity.getRank()
    );
  }

  @Test
  void testSqlUpdate() {
    final var result = abstractJdbcPlusRepository.sqlUpdate(entity, Entity.class);
    final var expectedStringTemplate =
        "update %s set name = '%s', rank = %d where id = %d returning *";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName,
        entity.getName(),
        entity.getRank(),
        entity.getId()
    );
  }

  @Test
  void testSqlSelectAll() {
    final var result = abstractJdbcPlusRepository.sqlSelectAll(entityTableName);
    final var expectedStringTemplate = "select * from %s";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName
    );
  }

  @Test
  void testSqlSelectAllByColumn() {
    final var column = "name";
    final var value = "SomeEntity";
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        entityTableName,
        column,
        value
    );
    final var expectedStringTemplate = "select * from %s where %s = '%s'";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName,
        column,
        value
    );
  }

  @Test
  void testSqlSelectAllByColumns() {
    final var column1 = "column1";
    final var column2 = "column2";
    final var value1 = "value1";
    final var value2 = "value2";
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        entityTableName,
        new String[]{column1, column2},
        new Object[]{value1, value2}
    );
    final var expectedStringTemplate = "select * from %s where %s = '%s', %s = '%s'";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName,
        column1, value1,
        column2, value2
    );
  }

  @Test
  void testSqlSelectAllByColumnsWhenNumberOfColumnsIsZero() {
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        entityTableName,
        new String[]{},
        new Object[]{}
    );
    final var expectedStringTemplate = "";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName
    );
  }

  @Test
  void testSqlSelectAllByColumnsWhenNumberOfColumnsDiffersFromNumberOfValues() {
    final var column1 = "column1";
    final var value1 = "value1";
    final var value2 = "value2";
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        entityTableName,
        new String[]{column1},
        new Object[]{value1, value2}
    );
    final var expectedStringTemplate = "";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        entityTableName,
        column1, value1,
        value2
    );
  }

  @Test
  void testSave() {
    abstractJdbcPlusRepository.save(entity, Entity.class);
    final var expectedQuery = abstractJdbcPlusRepository.sqlInsert(entity, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testUpdate() {
    final var selectQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        entityTableName,
        "id",
        entity.getId()
    );
    final var expectedQuery = abstractJdbcPlusRepository.sqlUpdate(entity, Entity.class);
    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));
    when(databaseConnection.queryTransaction(eq(expectedQuery), any(), any()))
        .thenReturn(Optional.of(entity));
    abstractJdbcPlusRepository.update(entity, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testRemove() {
    final var selectQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        entityTableName,
        "id",
        entity.getId()
    );
    final var expectedQuery = abstractJdbcPlusRepository.sqlRemove(entity, Entity.class);
    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));
    when(databaseConnection.queryTransaction(eq(expectedQuery), any(), any()))
        .thenReturn(Optional.of(entity));
    abstractJdbcPlusRepository.remove(entity, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFind() {
    abstractJdbcPlusRepository.find(entity, Entity.class);
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        entityTableName,
        getColumnNames(Entity.class),
        getColumnValues(entity, Entity.class)
    );
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindAll() {
    abstractJdbcPlusRepository.findAll(Entity.class);
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAll(entityTableName);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindByColumn() {
    final var column = "name";
    final var value = "SomeEntity";
    abstractJdbcPlusRepository.findByColumn(column, value, Entity.class);
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        entityTableName,
        column,
        value
    );
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindById() {
    final var entityId = entity.getId();
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        entityTableName,
        "id",
        entityId
    );
    when(databaseConnection.queryTransaction(eq(expectedQuery), any(), any()))
        .thenReturn(List.of(entity));
    abstractJdbcPlusRepository.findById(entityId, Entity.class);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testGetObject() {
    final var entityResultSet = TestUtils.getResultSet(entity);
    final var result = abstractJdbcPlusRepository.getObject(entityResultSet, Entity.class);
    assertThat(result).isEqualTo(entity);
  }

  @Test
  void testGetObjects() throws SQLException {
    final var entityResultSet = TestUtils.getResultSet(entity);
    when(entityResultSet.next())
        .thenReturn(true)
        .thenReturn(false);
    final var result = abstractJdbcPlusRepository.getObjects(entityResultSet, Entity.class);
    assertThat(result).containsOnly(entity);
  }

  @Test
  void testPopulateByColumnsMap() throws SQLException, IllegalAccessException, NoSuchFieldException {
    final var entityResultSet = TestUtils.getResultSet(entity);
    final var columnsMap = new HashMap<String, Field>();
    columnsMap.put("id", getIdColumn(Entity.class));
    columnsMap.put("name", Entity.class.getDeclaredField("name"));
    columnsMap.put("rank", Entity.class.getDeclaredField("rank"));
    final var newEntity = new Entity();
    abstractJdbcPlusRepository.populateByColumnsMap(entityResultSet, newEntity, columnsMap);
    assertThat(newEntity).isEqualTo(entity);
  }

  @Test
  void testPopulateColumnFields() throws SQLException, IllegalAccessException {
    final var entityResultSet = TestUtils.getResultSet(entity);
    final var newEntity = new Entity();
    abstractJdbcPlusRepository.populateColumnFields(entityResultSet, newEntity, Entity.class);
    assertThat(newEntity).isEqualTo(entity);
  }

  @Test
  void testPopulateField() throws IllegalAccessException {
    final var query = "query";
    when(databaseConnection.queryTransaction(eq(query), any(), any()))
        .thenReturn(2);
    abstractJdbcPlusRepository.populateField(
        getIdColumn(Entity.class),
        entity,
        "query",
        null,
        null
    );
    assertThat(entity.getId()).isEqualTo(2);
  }

  @Test
  void testPopulateOneToManyField() throws IllegalAccessException, NoSuchFieldException {
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity));
    abstractJdbcPlusRepository.populateOneToManyField(
        Entity.class.getDeclaredField("entities"),
        entity,
        Entity.class
    );
    assertThat(entity.getEntities()).containsOnly(entity);
  }

  @Test
  void testPopulateOneToManyFields() throws IllegalAccessException {
    abstractJdbcPlusRepository.populateOneToManyFields(entity, Entity.class);
    final var numberOfFields = getOneToManyFields(Entity.class).length;
    verify(databaseConnection, times(numberOfFields)).queryTransaction(any(), any(), any());
  }

  @Test
  @Disabled
  void testPopulateManyToManyField() throws IllegalAccessException {
    // TODO: Implement this test
    abstractJdbcPlusRepository.populateManyToManyField(entity, null, Entity.class);
  }

  @Test
  @Disabled
  void testPopulateManyToManyFields() throws IllegalAccessException {
    // TODO: Implement this test
    abstractJdbcPlusRepository.populateManyToManyFields(entity, Entity.class);
  }

  @Test
  @Disabled
  void testFetchRelatedEntities() {
    // TODO: Implement this test
    final var result = abstractJdbcPlusRepository.fetchRelatedEntities(
        null,
        "currentEntityIdColumnName",
        "targetEntityIdColumnName",
        Entity.class
    );
    assertThat(result).isEqualTo(singletonList(entity));
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

    final var actualResult = abstractJdbcPlusRepository.validManyToManyResultSet(
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
}
