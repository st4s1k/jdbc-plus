package st4s1k.jdbcplus.repo;

import lombok.SneakyThrows;
import org.mockito.AdditionalAnswers;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

  private TestUtils() {
  }

  /**
   * This method overcomes the issue with the original Mockito.spy when passing a lambda which fails with an error
   * saying that the passed class is final.
   */
  public static <T> T spyLambda(final Class<T> lambdaType, final T lambda) {
    return mock(lambdaType, AdditionalAnswers.delegatesTo(lambda));
  }

  @SneakyThrows
  public static ResultSet getResultSet(final Entity entity) {
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
}