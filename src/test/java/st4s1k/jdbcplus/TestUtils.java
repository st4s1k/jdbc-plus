package st4s1k.jdbcplus;

import lombok.SneakyThrows;
import st4s1k.jdbcplus.repo.*;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static st4s1k.jdbcplus.utils.EntityUtils.getColumnValues;
import static st4s1k.jdbcplus.utils.EntityUtils.getIdColumn;

public class TestUtils {

  private TestUtils() {
  }

  public static void assertEntitiesAreEqualByColumnValues(
      final Entity entity1,
      final Entity entity2
  ) {
    final var entity1ColumnValues = getColumnValues(entity1, Entity.class);
    final var entity2ColumnValues = getColumnValues(entity2, Entity.class);
    assertThat(entity1ColumnValues).containsExactly(entity2ColumnValues);
  }


  public static HashMap<String, Field> getEntityColumnsMap() throws NoSuchFieldException {
    final HashMap<String, Field> columnsMap = new HashMap<>();
    columnsMap.put("id", getIdColumn(Entity.class));
    columnsMap.put("name", Entity.class.getDeclaredField("name"));
    columnsMap.put("rank", Entity.class.getDeclaredField("rank"));
    columnsMap.put("entity4", Entity.class.getDeclaredField("entity4"));
    return columnsMap;
  }

  @SneakyThrows
  public static ResultSet getEntityResultSet(final Entity entity) {
    final var entityResultSet = mock(ResultSet.class);
    final var entityMetaData = mock(ResultSetMetaData.class);
    when(entityResultSet.getObject(1, Integer.class)).thenReturn(entity.getId());
    when(entityResultSet.getObject(2, String.class)).thenReturn(entity.getName());
    when(entityResultSet.getObject(3, Integer.class)).thenReturn(entity.getRank());
    when(entityResultSet.getObject(4, Integer.class)).thenReturn(entity.getEntity4().getId());
    when(entityResultSet.getMetaData()).thenReturn(entityMetaData);
    when(entityMetaData.getColumnCount()).thenReturn(4);
    when(entityMetaData.getColumnName(1)).thenReturn("id");
    when(entityMetaData.getColumnName(2)).thenReturn("name");
    when(entityMetaData.getColumnName(3)).thenReturn("rank");
    when(entityMetaData.getColumnName(4)).thenReturn("entity4");
    return entityResultSet;
  }

  public static Entity getEntity(
      final int id,
      final String name,
      final int rank
  ) {
    final var random = new Random();
    final var entity4id = random.nextInt();
    final var entity4rank = random.nextInt();
    final var entity4name = "SomeEntity4" + entity4id;
    final var entity4 = getEntity4(entity4id, entity4name, entity4rank);
    final var entity = new Entity();
    entity.setId(id);
    entity.setName(name);
    entity.setRank(rank);
    entity.setEntity4(entity4);
    entity.setEntity1s(emptyList());
    entity.setEntity2s(emptyList());
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
      final var entity = getEntity(id, name, i);
      entities.add(entity);
    }
    return entities;
  }

  public static Entity1 getEntity1(
      final int id,
      final String name,
      final int rank
  ) {
    final var entity1 = new Entity1();
    entity1.setId(id);
    entity1.setName(name);
    entity1.setRank(rank);
    entity1.setEntity2s(emptyList());
    entity1.setEntity3s(emptyList());
    return entity1;
  }

  public static Entity2 getEntity2(
      final int id,
      final String name,
      final int rank
  ) {
    final var random = new Random();
    final var entity3id = random.nextInt();
    final var entity3name = "SomeEntity3" + entity3id;
    final var entity3rank = random.nextInt();
    final var entity3 = getEntity3(entity3id, entity3name, entity3rank);
    final var entityId = random.nextInt();
    final var entityName = "SomeEntity" + entityId;
    final var entityRank = random.nextInt();
    final var entity = getEntity(entityId, entityName, entityRank);
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
    entity3.setEntity1s(emptyList());
    entity3.setEntity2s(emptyList());
    return entity3;
  }

  public static Entity4 getEntity4(
      final int id,
      final String name,
      final int rank
  ) {
    final var random = new Random();
    final var entity1id = random.nextInt();
    final var entity1name = "SomeEntity1" + entity1id;
    final var entity1rank = random.nextInt();
    final var entity1 = getEntity1(entity1id, entity1name, entity1rank);
    final var entity4 = new Entity4();
    entity4.setId(id);
    entity4.setName(name);
    entity4.setRank(rank);
    entity4.setEntity1(entity1);
    return entity4;
  }
}