package st4s1k.jdbcplus.exceptions;

public class JdbcPlusException extends RuntimeException {

  public JdbcPlusException() {
    super();
  }

  public JdbcPlusException(final Exception exception) {
    super(exception);
  }

  public JdbcPlusException(final String message) {
    super(message);
  }

  public JdbcPlusException(
      final String message,
      final Exception exception
  ) {
    super(message, exception);
  }
}
