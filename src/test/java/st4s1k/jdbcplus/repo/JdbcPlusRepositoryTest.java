package st4s1k.jdbcplus.repo;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    setField(DatabaseConnection.class, "instance", mock(DatabaseConnection.class));
    setField(AbstractJdbcPlusRepository.class, "instance", mock(AbstractJdbcPlusRepository.class));
    abstractJdbcPlusRepository = AbstractJdbcPlusRepository.getInstance();
    jdbcPlusRepository = new EntityRepository();
  }

  @Test
  void testGetTableName() {
    final String expected = EntityUtils.getTableName(Entity.class);
    final String actual = jdbcPlusRepository.getTableName();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testSave() {
    jdbcPlusRepository.save(entity);
    verify(abstractJdbcPlusRepository).save(entity);
  }

  @Test
  void testUpdate() {
    jdbcPlusRepository.update(entity);
    verify(abstractJdbcPlusRepository).update(entity);
  }

  @Test
  void testRemove() {
    jdbcPlusRepository.remove(entity);
    verify(abstractJdbcPlusRepository).remove(entity);
  }

  @Test
  void testFind() {
    jdbcPlusRepository.find(entity);
    verify(abstractJdbcPlusRepository).find(entity);
  }

  @Test
  void testFindById() {
    jdbcPlusRepository.findById(entity);
    verify(abstractJdbcPlusRepository).findById(entity, Entity.class);
  }

  @Test
  void testFindAll() {
    jdbcPlusRepository.findAll();
    verify(abstractJdbcPlusRepository).findAll(Entity.class);
  }

  @Test
  void testFindByColumn() {
    final var column = "column";
    final var value = new Object();
    jdbcPlusRepository.findByColumn(column, value);
    verify(abstractJdbcPlusRepository).findByColumn(column, value, Entity.class);
  }

  @Test
  void testGetObject() {
    final var resultSet = mock(ResultSet.class);
    jdbcPlusRepository.getObject(resultSet);
    verify(abstractJdbcPlusRepository).getObject(resultSet, Entity.class);
  }

  @Test
  void testGetObjects() {
    final var resultSet = mock(ResultSet.class);
    jdbcPlusRepository.getObjects(resultSet);
    verify(abstractJdbcPlusRepository).getObjects(resultSet, Entity.class);
  }
}
