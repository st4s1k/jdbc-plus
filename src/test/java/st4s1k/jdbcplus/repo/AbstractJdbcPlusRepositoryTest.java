package st4s1k.jdbcplus.repo;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import st4s1k.jdbcplus.DatabaseConnectionTestUtils;
import st4s1k.jdbcplus.Function;
import st4s1k.jdbcplus.config.DatabaseConnection;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static st4s1k.jdbcplus.TestUtils.*;
import static st4s1k.jdbcplus.utils.EntityUtils.*;

@ExtendWith(MockitoExtension.class)
class AbstractJdbcPlusRepositoryTest {

  private Entity entity;
  private Entity1 entity1;

  private DatabaseConnection databaseConnection;
  private AbstractJdbcPlusRepository abstractJdbcPlusRepository;

  @SneakyThrows
  @BeforeEach
  void setUp() {
    databaseConnection = mock(DatabaseConnection.class);

    DatabaseConnectionTestUtils.setInstance(databaseConnection);

    abstractJdbcPlusRepository = AbstractJdbcPlusRepository.getInstance();

    entity = getEntity(1, "SomeEntity", 5);
    entity1 = getEntity1(10, "SomeEntity1", 6);

    final var entity2 = getEntity2(20, "SomeEntity2", 7);
    final var entity3 = getEntity3(30, "SomeEntity3", 8);
    final var entity4 = getEntity4(40, "SomeEntity4", 9);

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

  @AfterEach
  void tearDown() {
    DatabaseConnectionTestUtils.resetDatabaseConnection();
    ReflectionTestUtils.setField(AbstractJdbcPlusRepository.class, "instance", null);
  }

  @Test
  void testSqlRemove() {
    // Given
    final var tableName = getTableName(entity.getClass());

    // When
    final var result = abstractJdbcPlusRepository.sqlRemove(entity);

    // Then
    assertThat(result).isEqualTo(
        "delete from %s where id = %d",
        tableName,
        entity.getId()
    );
  }

  @Test
  void testSqlInsert() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var expectedStringTemplate = "insert into %s(id, name, rank, entity4) " +
        "values (%d, '%s', %d, %d)";

    // When
    final var result = abstractJdbcPlusRepository.sqlInsert(entity);

    // Then
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        tableName,
        entity.getId(),
        entity.getName(),
        entity.getRank(),
        entity.getEntity4().getId()
    );
  }

  @Test
  void testSqlUpdate() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var expectedStringTemplate = "update %s " +
        "set name = '%s', rank = %d, entity4 = %d " +
        "where id = %d";

    // When
    final var result = abstractJdbcPlusRepository.sqlUpdate(entity);

    // Then
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        tableName,
        entity.getName(),
        entity.getRank(),
        entity.getEntity4().getId(),
        entity.getId()
    );
  }

  @Test
  void testSqlSelectAll() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var expectedStringTemplate = "select * from %s";

    // When
    final var result = abstractJdbcPlusRepository.sqlSelectAll(tableName);

    // Then
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        tableName
    );
  }

  @Test
  void testSqlSelectAllByColumn() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var column = "name";
    final var value = "SomeEntity";
    final var expectedStringTemplate = "select * from %s where %s = '%s'";

    // When
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        tableName,
        column,
        value
    );

    // Then
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        tableName,
        column,
        value
    );
  }

  @Test
  void testSqlSelectAllByColumns() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var column1 = "column1";
    final var value1 = "value1";
    final var column2 = "column2";
    final var value2 = "value2";
    final var expectedStringTemplate = "select * from %s where %s = '%s', %s = '%s'";

    // When
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        tableName,
        new String[]{column1, column2},
        new Object[]{value1, value2}
    );

    // Then
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        tableName,
        column1, value1,
        column2, value2
    );
  }

  @Test
  void testSqlSelectAllByColumnsWhenNumberOfColumnsIsZero() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var expectedStringTemplate = "";

    // When
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        tableName,
        new String[]{},
        new Object[]{}
    );

    // Then
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        tableName
    );
  }

  @Test
  void testSqlSelectAllByColumnsWhenNumberOfColumnsDiffersFromNumberOfValues() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var column1 = "column1";
    final var value1 = "value1";
    final var value2 = "value2";
    final var expectedStringTemplate = "";

    // When
    final var result = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        tableName,
        new String[]{column1},
        new Object[]{value1, value2}
    );

    // Then
    assertThat(result).isEqualTo(
        expectedStringTemplate,
        tableName,
        column1, value1,
        value2
    );
  }

  @Test
  void testSave() {
    // Given
    final var expectedInsertQuery = abstractJdbcPlusRepository.sqlInsert(entity);
    final var expectedSelectQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        getTableName(entity.getClass()),
        getIdColumnName(entity.getClass()),
        getIdColumnValue(entity)
    );

    // When
    abstractJdbcPlusRepository.save(entity);

    // Then
    final var inOrder = inOrder(databaseConnection);
    inOrder.verify(databaseConnection).queryTransaction(eq(expectedSelectQuery), any(), any());
    inOrder.verify(databaseConnection).queryTransaction(eq(expectedInsertQuery));
    inOrder.verify(databaseConnection).queryTransaction(eq(expectedSelectQuery), any(), any());
  }

  @Test
  void testSaveWhenEntityAlreadyExists() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var selectQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        tableName,
        "id",
        entity.getId()
    );
    final var expectedQuery = abstractJdbcPlusRepository.sqlUpdate(entity);

    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));

    // When
    abstractJdbcPlusRepository.save(entity);

    // Then
    final var inOrder = inOrder(databaseConnection);
    inOrder.verify(databaseConnection).queryTransaction(eq(selectQuery), any(), any());
    inOrder.verify(databaseConnection).queryTransaction(eq(expectedQuery));
    inOrder.verify(databaseConnection).queryTransaction(eq(selectQuery), any(), any());
  }

  @Test
  void testRemove() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var selectQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        tableName,
        "id",
        entity.getId()
    );
    final var expectedQuery = abstractJdbcPlusRepository.sqlRemove(entity);

    when(databaseConnection.queryTransaction(eq(selectQuery), any(), any()))
        .thenReturn(List.of(entity));

    // When
    abstractJdbcPlusRepository.remove(entity);

    // Then
    verify(databaseConnection).queryTransaction(eq(selectQuery), any(), any());
    verify(databaseConnection).queryTransaction(eq(expectedQuery));
  }

  @Test
  void testFind() {
    // Given
    final var tableName = getTableName(entity.getClass());

    // When
    abstractJdbcPlusRepository.find(entity);

    // Then
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumns(
        tableName,
        getColumnNames(Entity.class),
        getColumnValues(entity)
    );
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindAll() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAll(tableName);

    // When
    abstractJdbcPlusRepository.findAll(Entity.class);

    // Then
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindByColumn() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var column = "name";
    final var value = "SomeEntity";
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        tableName,
        column,
        value
    );

    // When
    abstractJdbcPlusRepository.findByColumn(column, value, Entity.class);

    // Then
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testFindById() {
    // Given
    final var tableName = getTableName(entity.getClass());
    final var entityId = entity.getId();
    final var expectedQuery = abstractJdbcPlusRepository.sqlSelectAllByColumn(
        tableName,
        "id",
        entityId
    );

    when(databaseConnection.queryTransaction(eq(expectedQuery), any(), any()))
        .thenReturn(List.of(entity));

    // When
    abstractJdbcPlusRepository.findById(entityId, Entity.class);

    // Then
    verify(databaseConnection).queryTransaction(eq(expectedQuery), any(), any());
  }

  @Test
  void testGetObject() {
    // Given
    final var entityResultSet = getEntityResultSet(entity);

    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));

    // When
    final var result = abstractJdbcPlusRepository.getObject(entityResultSet, Entity.class);

    // Then
    assertEntitiesAreEqualByColumnValues(result, entity);
  }

  @Test
  void testGetObjects() throws SQLException {
    // Given
    final var entityResultSet = getEntityResultSet(entity);

    when(entityResultSet.next())
        .thenReturn(true)
        .thenReturn(false);
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));

    // When
    final var result = abstractJdbcPlusRepository.getObjects(entityResultSet, Entity.class);

    // Then
    assertThat(result).hasOnlyOneElementSatisfying(
        e -> assertEntitiesAreEqualByColumnValues(e, entity)
    );
  }

  @Test
  void testPopulateByColumnsMap() throws NoSuchFieldException {
    // Given
    final var entityResultSet = getEntityResultSet(entity);
    final var entityColumnsMap = getEntityColumnsMap();
    final var newEntity = new Entity();

    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));

    // When
    abstractJdbcPlusRepository.populateByColumnsMap(entityResultSet, newEntity, entityColumnsMap);

    // Then
    assertEntitiesAreEqualByColumnValues(newEntity, entity);
  }

  @Test
  void testPopulateColumnFields() {
    // Given
    final var entityResultSet = getEntityResultSet(entity);
    final var newEntity = new Entity();

    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(List.of(entity.getEntity4()));

    // When
    abstractJdbcPlusRepository.populateColumnFields(entityResultSet, newEntity);

    // Then
    assertEntitiesAreEqualByColumnValues(newEntity, entity);
  }

  @ParameterizedTest
  @MethodSource("paramsForPopulateField")
  void testPopulateField(
      final Object transactionResult,
      final Field entityField,
      final Function<Entity, Object> entityGetter
  ) {
    // Given
    final var query = "query";

    when(databaseConnection.queryTransaction(eq(query), any(), any()))
        .thenReturn(transactionResult);

    // When
    abstractJdbcPlusRepository.populateField(
        entityField,
        entity,
        "query",
        null,
        null
    );

    // Then
    assertThat(entityGetter.apply(entity)).isEqualTo(transactionResult);
  }

  static Stream<Arguments> paramsForPopulateField() throws NoSuchFieldException {
    return Stream.of(
        arguments(
            2,
            Entity.class.getDeclaredField("id"),
            Function.of(Entity::getId)
        ),
        arguments(
            "SomeEntity123",
            Entity.class.getDeclaredField("name"),
            Function.of(Entity::getName)
        ),
        arguments(
            13,
            Entity.class.getDeclaredField("rank"),
            Function.of(Entity::getRank)
        ),
        arguments(
            getEntity4(4, "SomeEntity4abc", 1234),
            Entity.class.getDeclaredField("entity4"),
            Function.of(Entity::getEntity4)
        ),
        arguments(
            List.of(getEntity1(5, "SomeEntity1abc", 2345)),
            Entity.class.getDeclaredField("entity1s"),
            Function.of(Entity::getEntity1s)
        ),
        arguments(
            List.of(getEntity2(6, "SomeEntity2abc", 3456)),
            Entity.class.getDeclaredField("entity2s"),
            Function.of(Entity::getEntity2s)
        )
    );
  }

  @ParameterizedTest
  @MethodSource("paramsForPopulateOneToManyField")
  void testPopulateOneToManyField(
      final List<Object> listOfElements,
      final Field declaredField,
      final Function<Entity, List<?>> entityGetter
  ) {
    // Given
    when(databaseConnection.queryTransaction(any(), any(), any()))
        .thenReturn(listOfElements);

    // When
    abstractJdbcPlusRepository.populateOneToManyField(
        declaredField,
        entity
    );

    // Then
    assertThat(entityGetter.apply(entity)).isEqualTo(listOfElements);
  }

  static Stream<Arguments> paramsForPopulateOneToManyField() throws NoSuchFieldException {
    return Stream.of(
        arguments(
            List.of(getEntity1(1, "SomeEntity1", 6)),
            Entity.class.getDeclaredField("entity1s"),
            Function.of(Entity::getEntity1s)
        ),
        arguments(
            List.of(getEntity2(2, "SomeEntity2", 7)),
            Entity.class.getDeclaredField("entity2s"),
            Function.of(Entity::getEntity2s)
        )
    );
  }

  @Test
  void testPopulateOneToManyFields() {
    // Given
    final var numberOfFields = getOneToManyFields(Entity.class).length;

    // When
    abstractJdbcPlusRepository.populateOneToManyFields(entity);

    // Then
    verify(databaseConnection, times(numberOfFields)).queryTransaction(any(), any(), any());
  }

  @Test
  void testPopulateManyToManyField() throws NoSuchFieldException {
    // Given
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

    // When
    abstractJdbcPlusRepository.populateManyToManyField(field, entity1);

    // Then
    assertThat(entity1.getEntity3s()).containsOnly(newEntity3);
  }

  @Test
  void testPopulateManyToManyFields() {
    // Given
    final var numberOfFields = getManyToManyFields(Entity1.class).length;

    // When
    abstractJdbcPlusRepository.populateManyToManyFields(entity1);

    // Then
    verify(databaseConnection, times(numberOfFields)).queryTransaction(any(), any(), any());
  }

  @Test
  void testFetchEntitiesByIdColumn() throws SQLException {
    // Given
    final var initialId = 100;
    final var numberOfEntities = 3;
    final var entities = getEntities(initialId, numberOfEntities);
    final var idColumnName = getIdColumnName(Entity.class);
    final var idColumnNumber = 1;
    final var resultSet = mock(ResultSet.class);

    when(resultSet.findColumn(eq(idColumnName)))
        .thenReturn(idColumnNumber);
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

    // When
    final var result = abstractJdbcPlusRepository.fetchEntitiesByIdColumn(
        resultSet,
        idColumnName,
        Entity.class
    );

    // Then
    assertThat(result).hasSameElementsAs(entities);
  }
}
