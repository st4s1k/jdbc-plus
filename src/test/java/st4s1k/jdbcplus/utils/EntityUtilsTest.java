package st4s1k.jdbcplus.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class EntityUtilsTest {

  @Test
  void testGetFieldsAnnotatedWith() {
  }

  @Test
  void testGetFieldsMap() {
  }

  @Test
  void testGetTableName() {
  }

  @Test
  void testGetIdColumn() {
  }

  @Test
  void testGetIdColumnName() {
  }

  @Test
  void testTestGetIdColumnName() {
  }

  @Test
  void testGetColumns() {
  }

  @Test
  void testGetColumnFields() {
  }

  @Test
  void testGetColumnName() {
  }

  @Test
  void testGetColumnNames() {
  }

  @Test
  void testGetJoinColumnName() {
  }

  @Test
  void testGetColumnsMap() {
  }

  @Test
  void testGetOneToOneFields() {
  }

  @Test
  void testGetManyToOneFields() {
  }

  @Test
  void testGetOneToManyFields() {
  }

  @Test
  void testGetTargetEntity() {
  }

  @Test
  void testGetOneToOneTargetEntity() {
  }

  @Test
  void testGetManyToOneTargetEntity() {
  }

  @Test
  void testGetOneToManyTargetEntity() {
  }

  @Test
  void testGetJoinTableName() {
  }

  @Test
  void testGetManyToManyFields() {
  }

  @Test
  void testGetToManyFields() {
  }

  @Test
  void testGetColumnValue() {
  }

  @Test
  void testGetColumnValues() {
  }

  @Test
  void testGetColumnValuesAsStringForSQL() {
  }

  @Test
  void testGetIdColumnValue() {
  }

  @ParameterizedTest
  @MethodSource("parametersForGetStringValueForSql")
  void testGetStringValueForSql(final Object value, final String expectedValue) {
    final var stringValue = EntityUtils.getStringValueForSQL(value);
    assertThat(stringValue).isEqualTo(expectedValue);
  }

  private static Stream<Arguments> parametersForGetStringValueForSql() {
    return Stream.of(
        arguments(1, "1"),
        arguments("1", "'1'"),
        arguments(null, "NULL"),
        arguments(new long[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new int[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new short[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new char[]{'a', 'b', 'c'}, "['a', 'b', 'c']"),
        arguments(new byte[]{1, 2, 3}, "[1, 2, 3]"),
        arguments(new boolean[]{true, false, true}, "[true, false, true]"),
        arguments(new float[]{1.0f, 2.0f, 3.0f}, "[1.0, 2.0, 3.0]"),
        arguments(new double[]{1.0d, 2.0d, 3.0d}, "[1.0, 2.0, 3.0]"),
        arguments(new String[]{"a", "b", "c"}, "['a', 'b', 'c']"),
        arguments("abc", "'abc'")
    );
  }
}