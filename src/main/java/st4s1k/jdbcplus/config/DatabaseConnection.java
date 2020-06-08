package st4s1k.jdbcplus.config;

import st4s1k.jdbcplus.exceptions.InstanceAlreadyInitializedException;
import st4s1k.jdbcplus.exceptions.InstanceNotInitializedException;
import st4s1k.jdbcplus.function.ConnectionFunction;

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
import static java.util.Objects.requireNonNull;

public class DatabaseConnection {

  private static volatile DatabaseConnection instance;
  private static volatile DataSource dataSource;
  private static volatile Logger logger;

  private DatabaseConnection() {
    if (instance != null) {
      throw new InstanceAlreadyInitializedException();
    }
  }

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
          DatabaseConnection.dataSource = requireNonNull(dataSource);
        }
      }
    } else {
      throw new InstanceAlreadyInitializedException();
    }
  }

  public <T> T queryTransaction(
      final String query,
      final Function<ResultSet, T> operation,
      final Supplier<T> defaultResult
  ) {
    return applyConnection(
        connection -> {
          try (final Statement statement = connection.createStatement();
               final ResultSet resultSet = statement.executeQuery(query)) {
            connection.commit();
            return operation.apply(resultSet);
          } catch (final Exception e) {
            logger.log(ERROR, e.getLocalizedMessage(), e);
            connection.rollback();
            return defaultResult.get();
          }
        },
        defaultResult
    );
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

  private <T> T applyConnection(
      final ConnectionFunction<T> connectionFunction,
      final Supplier<T> defaultResult
  ) {
    try (final Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      return connectionFunction.apply(connection);
    } catch (final SQLException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
      return defaultResult.get();
    }
  }
}
