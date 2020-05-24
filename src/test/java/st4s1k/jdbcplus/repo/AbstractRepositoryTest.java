package st4s1k.jdbcplus.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import st4s1k.jdbcplus.utils.EntityUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractRepositoryTest {

  private AbstractRepository abstractRepository;
  private Entity entity;
  private String entityTableName;
  private String entityIdFieldName;

  @BeforeEach
  void setup() {
    abstractRepository = () -> null;
    entity = new Entity();
    entity.setId(1);
    entity.setName("SomeEntity");
    entityTableName = EntityUtils.getTableName(entity.getClass());
    entityIdFieldName = EntityUtils.getIdColumnName(entity.getClass());
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
        + " where " + entityIdFieldName + " = " + entity.getId()
        + " returning *");
  }

  @Test
  void testSqlInsert() {
    final var result = abstractRepository.sqlInsert(entity, Entity.class);
    assertThat(result).isEqualTo("insert into " + entityTableName
        + "(name) values "
        + "('" + entity.getName() + "') "
        + "returning *");
  }

  @Test
  void testSqlUpdate() {
    final var result = abstractRepository.sqlUpdate(entity, Entity.class);
    assertThat(result).isEqualTo(
        "update " + entityTableName + " set"
            + " name = '" + entity.getName() + "'"
            + " where " + entityIdFieldName + " = " + entity.getId()
            + " returning *");
  }

  @Test
  void testSqlSelectAll() {
    final var result = abstractRepository.sqlSelectAll(entityTableName);
    assertThat(result).isEqualTo("select * from " + entityTableName);
  }

  @Test
  void testSqlSelectAllByColumn() {
    final var result = abstractRepository.sqlSelectAllByColumn(
        entityTableName,
        "name",
        "SomeEntity"
    );
    assertThat(result).isEqualTo("replaceMeWithExpectedResult");
  }

  @Test
  void testSqlSelectAllByColumns() {
    final var result = abstractRepository.sqlSelectAllByColumns(
        entityTableName,
        new String[]{"columns"},
        new Object[]{"values"}
    );
    assertThat(result).isEqualTo("replaceMeWithExpectedResult");
  }

  @Test
  void testSave() {
    final var result = abstractRepository.save(entity, Entity.class);
    assertThat(result).isEqualTo(null);
  }

  @Test
  void testUpdate() {
    final var result = abstractRepository.update(entity, null);
    assertThat(result).isEqualTo(null);
  }

  @Test
  void testRemove() {
    final var result = abstractRepository.remove(entity, null);
    assertThat(result).isEqualTo(null);
  }

  @Test
  void testFind() {
    final var result = abstractRepository.find(entity, null);
    assertThat(result).isEqualTo(singletonList(entity));
  }

  @Test
  void testFindAll() {
    final var result = abstractRepository.findAll(null);
    assertThat(result).isEqualTo(singletonList(entity));
  }

  @Test
  void testFindByColumn() {
    final var result = abstractRepository
        .findByColumn("name", "SomeEntity", null);
    assertThat(result).isEqualTo(singletonList(entity));
  }

  @Test
  void testFindById() {
    final var result = abstractRepository.findById("id", null);
    assertThat(result).isEqualTo(null);
  }

  @Test
  void testGetObject() {
    Entity result = abstractRepository.getObject(null, null);
    assertThat(result).isEqualTo(entity);
  }

  @Test
  void testGetObjects() {
    final var result = abstractRepository.getObjects(null, null);
    assertThat(result).isEqualTo(singletonList(entity));
  }

  @Test
  void testPopulateByColumnsMap() throws SQLException, IllegalAccessException {
    abstractRepository.populateByColumnsMap(null, entity, new HashMap<>() {{
      put("String", null);
    }});
  }

  @Test
  void testPopulateColumnFields() throws SQLException, IllegalAccessException {
    abstractRepository.populateColumnFields(null, entity, null);
  }

  @Test
  void testPopulateOneToManyField() throws IllegalAccessException {
    abstractRepository.populateOneToManyField(null, entity, null);
  }

  @Test
  void testPopulateField() throws IllegalAccessException {
    abstractRepository.populateField(null, entity, "query", null, null);
  }

  @Test
  void testPopulateOneToManyFields() throws IllegalAccessException {
    abstractRepository.populateOneToManyFields(entity, null);
  }

  @Test
  void testPopulateManyToManyField() throws IllegalAccessException {
    abstractRepository.populateManyToManyField(entity, null, null);
  }

  @Test
  void testFetchRelatedEntities() {
    final var result = abstractRepository.fetchRelatedEntities(null,
        "currentEntityIdColumnName", "targetEntityIdColumnName", null);
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
    when(metaData.getColumnName(1)).thenReturn(actualCurrentEntityIdColumnName);
    when(metaData.getColumnName(2)).thenReturn(actualTargetEntityIdColumnName);

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
  }
}