package st4s1k.jdbcplus.exceptions;

public class JdbcPlusException extends RuntimeException {

  protected JdbcPlusException() {
    super();
  }

  protected JdbcPlusException(final Throwable cause) {
    super(cause);
  }

  protected JdbcPlusException(final String message) {
    super(message);
  }

  protected JdbcPlusException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static JdbcPlusException of(final Exception e) {
    return new JdbcPlusException(e);
  }

  public static JdbcPlusException of(final String msg) {
    return new JdbcPlusException(msg);
  }

  public static JdbcPlusException of(final String msg, final Throwable cause) {
    return new JdbcPlusException(msg, cause);
  }
}
