package st4s1k.jdbcplus.config;

import st4s1k.jdbcplus.exceptions.InstanceAlreadyInitializedException;
import st4s1k.jdbcplus.exceptions.InstanceNotInitializedException;
import st4s1k.jdbcplus.function.ConnectionConsumer;
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

  protected static volatile DatabaseConnection instance;
  protected static volatile DataSource dataSource;
  protected static volatile Logger logger;

  protected DatabaseConnection() {
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
        connection -> queryTransaction(connection, query, operation),
        defaultResult
    );
  }

  protected <T> T queryTransaction(
      final Connection connection,
      final String query,
      final Function<ResultSet, T> operation
  ) throws SQLException {
    final boolean initialAutocommit = connection.getAutoCommit();
    connection.setAutoCommit(false);
    try (final Statement statement = connection.createStatement();
         final ResultSet resultSet = statement.executeQuery(query)) {
      final T result = operation.apply(resultSet);
      connection.commit();
      return result;
    } catch (final Exception e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
      connection.rollback();
      throw e;
    } finally {
      if (initialAutocommit) {
        connection.setAutoCommit(true);
      }
    }
  }

  public void updateTransaction(final String updateQuery) {
    applyConnection(
        connection -> updateTransaction(connection, updateQuery)
    );
  }

  protected void updateTransaction(
      final Connection connection,
      final String updateQuery
  ) throws SQLException {
    final boolean initialAutocommit = connection.getAutoCommit();
    connection.setAutoCommit(false);
    try (final Statement statement = connection.createStatement()) {
      statement.executeUpdate(updateQuery);
      connection.commit();
    } catch (final Exception e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
      connection.rollback();
      throw e;
    } finally {
      if (initialAutocommit) {
        connection.setAutoCommit(true);
      }
    }
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
      return connectionFunction.apply(connection);
    } catch (final SQLException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
      return defaultResult.get();
    }
  }

  private void applyConnection(final ConnectionConsumer connectionFunction) {
    try (final Connection connection = dataSource.getConnection()) {
      connectionFunction.accept(connection);
    } catch (final SQLException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
    }
  }
}
