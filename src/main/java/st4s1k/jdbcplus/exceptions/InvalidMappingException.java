package st4s1k.jdbcplus.exceptions;

public class InvalidMappingException extends JdbcPlusException {
  protected InvalidMappingException(String message) {
    super(message);
  }

  public static InvalidMappingException of(String message) {
    return new InvalidMappingException(message);
  }
}
