package st4s1k.jdbcplus.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import st4s1k.jdbcplus.exceptions.InvalidColumnTypeException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class EntityUtilsTest {

  @Test
  @Disabled
  void testGetFieldsAnnotatedWith() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetFieldsMap() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetTableName() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetIdColumn() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetIdColumnName() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testTestGetIdColumnName() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumns() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumnFields() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumnName() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumnNames() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetJoinColumnName() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumnsMap() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetOneToOneFields() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetManyToOneFields() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetOneToManyFields() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetTargetEntity() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetOneToOneTargetEntity() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetManyToOneTargetEntity() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetOneToManyTargetEntity() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetJoinTableName() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetManyToManyFields() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetToManyFields() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumnValue() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumnValues() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetColumnValuesAsStringForSQL() {
    // TODO: Implement this test
  }

  @Test
  @Disabled
  void testGetIdColumnValue() {
    // TODO: Implement this test
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
        arguments('a', "'a'"),
        arguments("1", "'1'"),
        arguments(null, "NULL"),
        arguments("abc", "'abc'")
    );
  }

  @ParameterizedTest
  @MethodSource("parametersGetStringValueForSqlWhenInvalidTypeThenThrow")
  void testGetStringValueForSqlWhenInvalidTypeThenThrow(final Object value) {
    assertThrows(
        InvalidColumnTypeException.class,
        () -> EntityUtils.getStringValueForSQL(value)
    );
  }

  private static Stream<Arguments> parametersGetStringValueForSqlWhenInvalidTypeThenThrow() {
    return Stream.of(
        arguments(new long[]{1, 2, 3}),
        arguments(new int[]{1, 2, 3}),
        arguments(new short[]{1, 2, 3}),
        arguments(new char[]{'a', 'b', 'c'}),
        arguments(new byte[]{1, 2, 3}),
        arguments(new boolean[]{true, false, true}),
        arguments(new float[]{1.0f, 2.0f, 3.0f}),
        arguments(new double[]{1.0d, 2.0d, 3.0d}),
        arguments((Object) new String[]{"a", "b", "c"})
    );
  }
}