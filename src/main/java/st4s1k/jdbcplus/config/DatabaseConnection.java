package st4s1k.jdbcplus.config;

import st4s1k.jdbcplus.exceptions.InstanceAlreadyInitializedException;
import st4s1k.jdbcplus.exceptions.InstanceNotInitializedException;

import javax.sql.DataSource;
import java.lang.System.Logger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.System.Logger.Level.ERROR;

public class DatabaseConnection {

  private static volatile DatabaseConnection instance;
  private static volatile DataSource dataSource;
  private static volatile Logger logger;

  public static DatabaseConnection getInstance() {
    if (instance == null) {
      throw new InstanceNotInitializedException();
    }
    return instance;
  }

  public static void init(final DataSource dataSource) {
    if (instance == null) {
      synchronized (DatabaseConnection.class) {
        if (instance == null) {
          instance = new DatabaseConnection();
          logger = System.getLogger("DatabaseConnection");
          DatabaseConnection.dataSource = dataSource;
        }
      }
    } else {
      throw new InstanceAlreadyInitializedException();
    }
  }

  private DatabaseConnection() {
  }

  public <T> T queryTransaction(
      final String query,
      final Function<ResultSet, T> operation,
      final Supplier<T> defaultResult
  ) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement();
           ResultSet resultSet = statement.executeQuery(query)) {
        connection.commit();
        return operation.apply(resultSet);
      } catch (Exception e) {
        logger.log(ERROR, e.getLocalizedMessage(), e);
        connection.rollback();
      }
    } catch (SQLException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
    }
    return defaultResult.get();
  }

  public <T> Optional<T> queryTransaction(
      final String query,
      final Function<ResultSet, T> operation
  ) {
    return queryTransaction(
        query,
        resultSet -> Optional.ofNullable(operation.apply(resultSet)),
        Optional::empty
    );
  }
}
