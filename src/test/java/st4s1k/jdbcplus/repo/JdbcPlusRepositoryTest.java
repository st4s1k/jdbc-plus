package st4s1k.jdbcplus.repo;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import st4s1k.jdbcplus.DatabaseConnectionTestUtils;
import st4s1k.jdbcplus.config.DatabaseConnection;
import st4s1k.jdbcplus.utils.EntityUtils;

import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class JdbcPlusRepositoryTest {

  @Mock
  private Entity entity;

  private AbstractJdbcPlusRepository abstractJdbcPlusRepository;
  private EntityRepository jdbcPlusRepository;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    DatabaseConnectionTestUtils.setInstance(mock(DatabaseConnection.class));
    abstractJdbcPlusRepository = mock(AbstractJdbcPlusRepository.class);
    setField(AbstractJdbcPlusRepository.class, "instance", abstractJdbcPlusRepository);
    jdbcPlusRepository = new EntityRepository();
  }

  @AfterEach
  void tearDown() {
    DatabaseConnectionTestUtils.resetDatabaseConnection();
  }

  @Test
  void testGetTableName() {
    // Given
    final String expected = EntityUtils.getTableName(Entity.class);

    // When
    final String actual = jdbcPlusRepository.getTableName();

    // Then
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testSave() {
    jdbcPlusRepository.save(entity);
    verify(abstractJdbcPlusRepository).save(entity);
  }

  @Test
  void testUpdate() {
    // When
    jdbcPlusRepository.update(entity);

    // Then
    verify(abstractJdbcPlusRepository).save(entity);
  }

  @Test
  void testRemove() {
    // When
    jdbcPlusRepository.remove(entity);

    // Then
    verify(abstractJdbcPlusRepository).remove(entity);
  }

  @Test
  void testFind() {
    // When
    jdbcPlusRepository.find(entity);

    // Then
    verify(abstractJdbcPlusRepository).find(entity);
  }

  @Test
  void testFindById() {
    // When
    jdbcPlusRepository.findById(entity);

    // Then
    verify(abstractJdbcPlusRepository).findById(entity, Entity.class);
  }

  @Test
  void testFindAll() {
    // When
    jdbcPlusRepository.findAll();

    // Then
    verify(abstractJdbcPlusRepository).findAll(Entity.class);
  }

  @Test
  void testFindByColumn() {
    // Given
    final var column = "column";
    final var value = new Object();

    // When
    jdbcPlusRepository.findByColumn(column, value);

    // Then
    verify(abstractJdbcPlusRepository).findByColumn(column, value, Entity.class);
  }

  @Test
  void testGetObject() {
    // Given
    final var resultSet = mock(ResultSet.class);

    // When
    jdbcPlusRepository.getObject(resultSet);

    // Then
    verify(abstractJdbcPlusRepository).getObject(resultSet, Entity.class);
  }

  @Test
  void testGetObjects() {
    // Given
    final var resultSet = mock(ResultSet.class);

    // When
    jdbcPlusRepository.getObjects(resultSet);

    // Then
    verify(abstractJdbcPlusRepository).getObjects(resultSet, Entity.class);
  }
}
