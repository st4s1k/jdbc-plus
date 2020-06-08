package st4s1k.jdbcplus;

import org.springframework.test.util.ReflectionTestUtils;
import st4s1k.jdbcplus.config.DatabaseConnection;

import javax.sql.DataSource;

public class DatabaseConnectionTestUtils {

  public static void resetDatabaseConnection() {
    resetInstance();
    resetLogger();
    resetDataSource();
  }

  public static void setInstance(final DatabaseConnection databaseConnection) {
    ReflectionTestUtils.setField(DatabaseConnection.class, "instance", databaseConnection);
  }

  public static void resetInstance() {
    setInstance(null);
  }

  public static void setLogger(final System.Logger logger) {
    ReflectionTestUtils.setField(DatabaseConnection.class, "logger", logger);
  }

  public static void resetLogger() {
    setLogger(null);
  }

  public static System.Logger getLogger() {
    return (System.Logger) ReflectionTestUtils.getField(DatabaseConnection.class, "logger");
  }

  public static void resetDataSource() {
    ReflectionTestUtils.setField(DatabaseConnection.class, "dataSource", null);
  }

  public static DataSource getDataSource() {
    return (DataSource) ReflectionTestUtils.getField(DatabaseConnection.class, "dataSource");
  }
}
