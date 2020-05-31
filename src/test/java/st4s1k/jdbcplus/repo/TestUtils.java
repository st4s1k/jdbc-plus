package st4s1k.jdbcplus.repo;

import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static st4s1k.jdbcplus.utils.EntityUtils.getColumnValues;

public class TestUtils {

  private TestUtils() {
  }

  public static void assertEntitiesAreEqualByColumnValues(
      final Entity entity1,
      final Entity entity2
  ) {
    final Object[] entity1ColumnValues = getColumnValues(entity1, Entity.class);
    final Object[] entity2ColumnValues = getColumnValues(entity2, Entity.class);
    assertThat(entity1ColumnValues).containsExactly(entity2ColumnValues);
  }

  @SneakyThrows
  public static ResultSet getEntityResultSet(final Entity entity) {
    final var entityResultSet = mock(ResultSet.class);
    final var entityMetaData = mock(ResultSetMetaData.class);
    when(entityResultSet.getMetaData()).thenReturn(entityMetaData);
    when(entityResultSet.getObject(1, Integer.class)).thenReturn(entity.getId());
    when(entityResultSet.getObject(2, String.class)).thenReturn(entity.getName());
    when(entityResultSet.getObject(3, Integer.class)).thenReturn(entity.getRank());
    when(entityMetaData.getColumnCount()).thenReturn(3);
    when(entityMetaData.getColumnName(1)).thenReturn("id");
    when(entityMetaData.getColumnName(2)).thenReturn("name");
    when(entityMetaData.getColumnName(3)).thenReturn("rank");
    return entityResultSet;
  }

  public static Entity getEntity(
      final int id,
      final String name,
      final int rank,
      final List<Entity1> entity1s,
      final List<Entity2> entity2s
  ) {
    final var entity = new Entity();
    entity.setId(id);
    entity.setName(name);
    entity.setRank(rank);
    if (!entity1s.isEmpty()) {
      entity.setEntity1s(entity1s);
    }
    if (!entity2s.isEmpty()) {
      entity.setEntity2s(entity2s);
    }
    return entity;
  }

  public static List<Entity> getEntities(
      final int initialId,
      final int numberOfEntities
  ) {
    final var entities = new ArrayList<Entity>(numberOfEntities);
    for (var i = 0; i < numberOfEntities; i++) {
      final var id = initialId + i;
      final var name = "SomeEntity" + id;
      final var entity1s = Collections.<Entity1>emptyList();
      final var entity2s = Collections.<Entity2>emptyList();
      final var entity = getEntity(id, name, i, entity1s, entity2s);
      entities.add(entity);
    }
    return entities;
  }

  public static Entity1 getEntity1(
      final int id,
      final String name,
      final int rank,
      final List<Entity2> entity2s,
      final List<Entity3> entity3s
  ) {
    final var entity1 = new Entity1();
    entity1.setId(id);
    entity1.setName(name);
    entity1.setRank(rank);
    if (!entity2s.isEmpty()) {
      entity1.setEntity2s(entity2s);
    }
    if (!entity3s.isEmpty()) {
      entity1.setEntity3s(entity3s);
    }
    return entity1;
  }

  public static Entity2 getEntity2(
      final int id,
      final String name,
      final int rank,
      final Entity entity,
      final Entity3 entity3
  ) {
    final var entity2 = new Entity2();
    entity2.setId(id);
    entity2.setName(name);
    entity2.setRank(rank);
    entity2.setEntity(entity);
    entity2.setEntity3(entity3);
    return entity2;
  }

  public static Entity3 getEntity3(
      final int id,
      final String name,
      final int rank
  ) {
    final var entity3 = new Entity3();
    entity3.setId(id);
    entity3.setName(name);
    entity3.setRank(rank);
    return entity3;
  }
}