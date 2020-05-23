package st4s1k.jdbcplus.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st4s1k.jdbcplus.utils.EntityUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

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

  @Test
  void testValidManyToManyResultSet() throws SQLException {
    ResultSetMetaData metaData;
    boolean result = abstractRepository.validManyToManyResultSet(metaData,
        "currentEntityIdColumnName", "targetEntityIdColumnName");
    assertThat(result).isEqualTo(true);
  }

  @Test
  void testPopulateManyToManyFields() throws IllegalAccessException {
    abstractRepository.populateManyToManyFields(entity, null);
  }
}