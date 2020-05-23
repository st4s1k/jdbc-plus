package st4s1k.jdbcplus.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class DatabaseConnection {

  private final DataSource dataSource;

  public DatabaseConnection(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public <T> T queryTransaction(
      final String query,
      final Function<ResultSet, T> operation,
      final Supplier<T> defaultResult) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement();
           ResultSet resultSet = statement.executeQuery(query)) {
        connection.commit();
        return operation.apply(resultSet);
      } catch (Exception e) {
        connection.rollback();
        e.printStackTrace();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return defaultResult.get();
  }

  public <T> Optional<T> queryTransaction(
      final String query,
      final Function<ResultSet, T> operation) {
    return queryTransaction(query,
        resultSet -> Optional.ofNullable(operation.apply(resultSet)),
        Optional::empty);
  }
}
