package st4s1k.jdbcplus.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import st4s1k.jdbcplus.config.DatabaseConnection;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static st4s1k.jdbcplus.repo.TestUtils.*;
import static st4s1k.jdbcplus.utils.EntityUtils.*;

@ExtendWith(MockitoExtension.class)
class AbstractJdbcPlusRepositoryTest {

  @Mock
  private DatabaseConnection databaseConnection;

  private Entity entity;
  private Entity1 entity1;
  private Entity2 entity2;
  private Entity3 entity3;
  private Entity4 entity4;

  private AbstractJdbcPlusRepository abstractJdbcPlusRepository;

  @BeforeEach
  void setUp() {
    abstractJdbcPlusRepository = new AbstractJdbcPlusRepository(databaseConnection);

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
  void testSqlRemove() {
    final var result = abstractJdbcPlusRepository.sqlRemove(entity);
    final var expectedStringTemplate = "delete from %s where id = %d returning *";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass()),
        entity.getId()
    );
  }

  @Test
  void testSqlInsert() {
    final var result = abstractJdbcPlusRepository.sqlInsert(entity);
    final var expectedStringTemplate =
        "insert into %s(id, name, rank, entity4) values (%d, '%s', %d, %d) returning *";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass()),
        entity.getId(),
        entity.getName(),
        entity.getRank(),
        entity.getEntity4().getId()
    );
  }

  @Test
  void testSqlUpdate() {
    final var result = abstractJdbcPlusRepository.sqlUpdate(entity);
    final var expectedStringTemplate =
        "update %s set name = '%s', rank = %d, entity4 = %d where id = %d returning *";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass()),
        entity.getName(),
        entity.getRank(),
        entity.getEntity4().getId(),
        entity.getId()
    );
  }

  @Test
  void testSqlSelectAll() {
    final var result = abstractJdbcPlusRepository.sqlSelectAll(getTableName(entity.getClass()));
    final var expectedStringTemplate = "select * from %s";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass())
    );
  }

  @Test
  void testSqlSelectAllByColumn() {
    final var column = "name";
    final var value = "SomeEntity";
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        getTableName(entity.getClass()),
        column,
        value
    );
    final var expectedStringTemplate = "select * from %s where %s = '%s'";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass()),
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
        getTableName(entity.getClass()),
        new String[]{column1, column2},
        new Object[]{value1, value2}
    );
    final var expectedStringTemplate = "select * from %s where %s = '%s', %s = '%s'";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass()),
        column1, value1,
        column2, value2
    );
  }

  @Test
  void testSqlSelectAllByColumnsWhenNumberOfColumnsIsZero() {
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        getTableName(entity.getClass()),
        new String[]{},
        new Object[]{}
    );
    final var expectedStringTemplate = "";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass())
    );
  }

  @Test
  void testSqlSelectAllByColumnsWhenNumberOfColumnsDiffersFromNumberOfValues() {
    final var column1 = "column1";
    final var value1 = "value1";
    final var value2 = "value2";
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        getTableName(entity.getClass()),
        new String[]{column1},
        new Object[]{value1, value2}
    );
    final var expectedStringTemplate = "";
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        getTableName(entity.getClass()),
        column1, value1,
        value2
    );
  }

  @Test
  void testSave() {
    abstractJdbcPlusRepository.save(entity);
    final var expectedQuery = abstractJdbcPlusRepository.sqlInsert(entity);
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testUpdate() {
    final var selectQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        getTableName(entity.getClass()),
        "id",
        entity.getId()
    );
    final var expectedQuery = abstractJdbcPlusRepository.sqlUpdate(entity);
    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));
    when(databaseConnection.queryTransaction(eq(expectedQuery), any()))
        .thenReturn(Optional.of(entity));
    abstractJdbcPlusRepository.update(entity);
    verify(databaseConnection).queryTransaction(eq(selectQuery), any(), any());
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any());
  }

  @Test
  void testRemove() {
    final var selectQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        getTableName(entity.getClass()),
        "id",
        entity.getId()
    );
    final var expectedQuery = abstractJdbcPlusRepository.sqlRemove(entity);
    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));
    when(databaseConnection.queryTransaction(eq(expectedQuery), any()))
        .thenReturn(Optional.of(entity));
    abstractJdbcPlusRepository.remove(entity);
    verify(databaseConnection).queryTransaction(eq(selectQuery), any(), any());
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any());
  }

  @Test
  void testFind() {
    abstractJdbcPlusRepository.find(entity);
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        getTableName(entity.getClass()),
        getColumnNames(Entity.class),
        getColumnValues(entity, Entity.class)
    );
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindAll() {
    abstractJdbcPlusRepository.findAll(Entity.class);
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAll(getTableName(entity.getClass()));
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindByColumn() {
    final var column = "name";
    final var value = "SomeEntity";
    abstractJdbcPlusRepository.findByColumn(column, value, Entity.class);
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        getTableName(entity.getClass()),
        column,
        value
    );
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindById() {
    final var entityId = entity.getId();
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        getTableName(entity.getClass()),
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
    final var entityResultSet = getEntityResultSet(entity);
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));
    final var result = abstractJdbcPlusRepository.getObject(entityResultSet, Entity.class);
    assertEntitiesAreEqualByColumnValues(result, entity);
  }

  @Test
  void testGetObjects() throws SQLException {
    final var entityResultSet = getEntityResultSet(entity);
    when(entityResultSet.next())
        .thenReturn(true)
        .thenReturn(false);
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));
    final var result = abstractJdbcPlusRepository.getObjects(entityResultSet, Entity.class);
    assertThat(result).hasOnlyOneElementSatisfying(
        e -> assertEntitiesAreEqualByColumnValues(e, entity)
    );
  }

  @Test
  void testPopulateByColumnsMap() throws NoSuchFieldException {
    final var entityResultSet = getEntityResultSet(entity);
    final var columnsMap = new HashMap<String, Field>();
    columnsMap.put("id", getIdColumn(Entity.class));
    columnsMap.put("name", Entity.class.getDeclaredField("name"));
    columnsMap.put("rank", Entity.class.getDeclaredField("rank"));
    columnsMap.put("entity4", Entity.class.getDeclaredField("entity4"));
    final var newEntity = new Entity();
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));
    abstractJdbcPlusRepository.populateByColumnsMap(entityResultSet, newEntity, columnsMap);
    assertEntitiesAreEqualByColumnValues(newEntity, entity);
  }

  @Test
  void testPopulateColumnFields() {
    final var entityResultSet = getEntityResultSet(entity);
    final var newEntity = new Entity();
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));
    abstractJdbcPlusRepository.populateColumnFields(entityResultSet, newEntity, Entity.class);
    assertEntitiesAreEqualByColumnValues(newEntity, entity);
  }

  @Test
  void testPopulateField() {
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
  void testPopulateOneToManyField() throws NoSuchFieldException {
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity1))
        .thenReturn(List.of(entity2));
    abstractJdbcPlusRepository.populateOneToManyField(
        Entity.class.getDeclaredField("entity1s"),
        entity,
        Entity.class
    );
    abstractJdbcPlusRepository.populateOneToManyField(
        Entity.class.getDeclaredField("entity2s"),
        entity,
        Entity.class
    );
    assertThat(entity.getEntity1s()).containsOnly(entity1);
    assertThat(entity.getEntity2s()).containsOnly(entity2);
  }

  @Test
  void testPopulateOneToManyFields() {
    abstractJdbcPlusRepository.populateOneToManyFields(entity);
    final var numberOfFields = getOneToManyFields(Entity.class).length;
    verify(databaseConnection, times(numberOfFields)).queryTransaction(any(), any(), any());
  }

  @Test
  void testPopulateManyToManyField() throws NoSuchFieldException {
    final var field = Entity1.class.getDeclaredField("entity3s");
    final var joinTable = getJoinTable(field);
    final var joinColumn = joinTable.joinColumn();
    final var query = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        getJoinTableName(field),
        getEntityJoinColumnName(Entity1.class, joinColumn),
        getIdColumnValue(entity1)
    );
    final var newEntity3 = getEntity3(40, "SomeEntity4", 9);
    when(databaseConnection.queryTransaction(eq(query), any(), any()))
        .thenReturn(List.of(newEntity3));
    abstractJdbcPlusRepository.populateManyToManyField(field, entity1);
    assertThat(entity1.getEntity3s()).containsOnly(newEntity3);
  }

  @Test
  void testPopulateManyToManyFields() {
    abstractJdbcPlusRepository.populateManyToManyFields(entity1);
    final var numberOfFields = getManyToManyFields(Entity1.class).length;
    verify(databaseConnection, times(numberOfFields)).queryTransaction(any(), any(), any());
  }

  @Test
  void testFetchEntitiesByIdColumn() throws SQLException {
    final var initialId = 100;
    final var numberOfEntities = 3;
    final var entities = getEntities(initialId, numberOfEntities);
    final var idColumnName = getIdColumnName(Entity.class);
    final var idColumnNumber = 1;
    final var resultSet = mock(ResultSet.class);

    when(resultSet.findColumn(eq(idColumnName))).thenReturn(idColumnNumber);

    when(resultSet.next())
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(false);

    when(resultSet.getObject(idColumnNumber))
        .thenReturn(entities.get(0).getId())
        .thenReturn(entities.get(1).getId())
        .thenReturn(entities.get(2).getId());

    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entities.get(0)))
        .thenReturn(List.of(entities.get(1)))
        .thenReturn(List.of(entities.get(2)));

    final var result = abstractJdbcPlusRepository.fetchEntitiesByIdColumn(
        resultSet,
        idColumnName,
        Entity.class
    );

    assertThat(result).hasSameElementsAs(entities);
  }
}
