package st4s1k.jdbcplus.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import st4s1k.jdbcplus.DatabaseConnectionTestUtils;
import st4s1k.jdbcplus.exceptions.InstanceAlreadyInitializedException;
import st4s1k.jdbcplus.exceptions.InstanceNotInitializedException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

import static java.lang.System.Logger.Level.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseConnectionTest {

  private static final String QUERY = "query";

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private Statement statement;

  @Mock
  private ResultSet resultSet;

  @AfterEach
  void tearDown() {
    DatabaseConnectionTestUtils.resetDatabaseConnection();
  }

  @Test
  void testGetInstance() {
    final var expectedInstance = mock(DatabaseConnection.class);
    DatabaseConnectionTestUtils.setInstance(expectedInstance);
    final var actualInstance = DatabaseConnection.getInstance();
    assertThat(actualInstance).isEqualTo(expectedInstance);
  }

  @Test
  void testGetInstanceWhenInstanceIsNotInitializedThenThrows() {
    assertThrows(
        InstanceNotInitializedException.class,
        DatabaseConnection::getInstance
    );
  }

  @Test
  void testInit() {
    final var expectedDataSource = mock(DataSource.class);

    DatabaseConnection.init(expectedDataSource);

    final var instance = DatabaseConnection.getInstance();
    final var logger = DatabaseConnectionTestUtils.getLogger();
    final var actualDataSource = DatabaseConnectionTestUtils.getDataSource();

    assertThat(instance).isNotNull();
    assertThat(logger).isNotNull();
    assertThat(actualDataSource).isNotNull();
    assertThat(actualDataSource).isEqualTo(expectedDataSource);
  }

  @Test
  void testInitWhenDataSourceIsNullThenThrows() {
    assertThrows(
        NullPointerException.class,
        () -> DatabaseConnection.init(null)
    );
  }

  @Test
  void testInitWhenInstanceAlreadyInitializedThenThrows() {
    final var dataSource = mock(DataSource.class);
    DatabaseConnection.init(dataSource);
    assertThrows(
        InstanceAlreadyInitializedException.class,
        () -> DatabaseConnection.init(dataSource)
    );
  }

  @Test
  void testQueryTransaction() throws SQLException {
    doNothing().when(connection).setAutoCommit(false);
    doNothing().when(connection).commit();

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(eq(QUERY))).thenReturn(resultSet);

    DatabaseConnection.init(dataSource);

    final var databaseConnection = DatabaseConnection.getInstance();
    final var expectedResult = new Object();
    final var operation = (Function<ResultSet, Object>) r -> expectedResult;
    final var actualResult = databaseConnection.queryTransaction(QUERY, operation, Object::new);

    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @Test
  void testQueryTransactionWhenOperationThrowsThenRollbackAndLogException() throws SQLException {
    doNothing().when(connection).setAutoCommit(false);
    doNothing().when(connection).commit();

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(eq(QUERY))).thenReturn(resultSet);

    DatabaseConnection.init(dataSource);

    final var logger = mock(System.Logger.class);

    DatabaseConnectionTestUtils.setLogger(logger);

    final var databaseConnection = DatabaseConnection.getInstance();
    final var operation = (Function<ResultSet, Object>) r -> {
      throw new RuntimeException();
    };

    databaseConnection.queryTransaction(QUERY, operation, Object::new);

    verify(logger).log(eq(ERROR), nullable(String.class), isA(RuntimeException.class));
    verify(connection).rollback();
  }

  @Test
  void testQueryTransactionWhenDataSourceGetConnectionFailsThenLogException() throws SQLException {
    when(dataSource.getConnection()).thenThrow(SQLException.class);

    DatabaseConnection.init(dataSource);

    final var logger = mock(System.Logger.class);

    DatabaseConnectionTestUtils.setLogger(logger);

    final var databaseConnection = DatabaseConnection.getInstance();
    final var operation = (Function<ResultSet, Object>) r -> new Object();

    databaseConnection.queryTransaction(QUERY, operation, Object::new);

    verify(logger).log(eq(ERROR), nullable(String.class), isA(SQLException.class));
  }

  @Test
  void testQueryTransactionOptional() throws SQLException {
    doNothing().when(connection).setAutoCommit(false);
    doNothing().when(connection).commit();

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(eq(QUERY))).thenReturn(resultSet);

    DatabaseConnection.init(dataSource);

    final var databaseConnection = DatabaseConnection.getInstance();
    final var expectedResult = new Object();
    final var operation = (Function<ResultSet, Object>) r -> expectedResult;
    final var actualResult = databaseConnection.queryTransaction(QUERY, operation);

    assertThat(actualResult).hasValue(expectedResult);
  }
}
