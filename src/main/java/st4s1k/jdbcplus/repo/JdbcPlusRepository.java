package st4s1k.jdbcplus.repo;

import st4s1k.jdbcplus.utils.EntityUtils;

import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class JdbcPlusRepository<T> {

  private final AbstractJdbcPlusRepository abstractJdbcPlusRepository;
  private final Class<T> entityClass;

  @SuppressWarnings("unchecked")
  public JdbcPlusRepository() {
    entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
        .getActualTypeArguments()[0];
    abstractJdbcPlusRepository = AbstractJdbcPlusRepository.getInstance();
  }

  /**
   * Get database table name associated with the entity.
   *
   * @return table name
   */
  public String getTableName() {
    return EntityUtils.getTableName(entityClass);
  }

  /**
   * Create a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} saved entity
   */
  public Optional<T> save(final T entity) {
    return abstractJdbcPlusRepository.save(entity);
  }

  /**
   * Update a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} updated entity
   */
  public Optional<T> update(final T entity) {
    return abstractJdbcPlusRepository.update(entity);
  }

  /**
   * Remove a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} removed entity
   */
  public Optional<T> remove(final T entity) {
    return abstractJdbcPlusRepository.remove(entity);
  }

  /**
   * Find all entities, having entity fields equal to related columns.
   *
   * @param entity the entity
   * @return a list of found entities
   */
  public List<T> find(final T entity) {
    return abstractJdbcPlusRepository.find(entity);
  }

  /**
   * Find entity by id.
   *
   * @param id entity id
   * @return {@link Optional} found entity
   */
  public Optional<T> findById(final Object id) {
    return abstractJdbcPlusRepository.findById(id, entityClass);
  }

  /**
   * Fetch all entities.
   *
   * @return a list of found entities
   */
  public List<T> findAll() {
    return abstractJdbcPlusRepository.findAll(entityClass);
  }

  /**
   * Fetch all entities where from associated table,
   * where column has specified value.
   *
   * @param column table column
   * @param value  specified value
   * @return a list of found entities
   */
  public List<T> findByColumn(
      final String column,
      final Object value
  ) {
    return abstractJdbcPlusRepository.findByColumn(column, value, entityClass);
  }

  /**
   * Extract entity from result set
   *
   * @param resultSet the result set
   * @return extracted entity
   */
  public T getObject(final ResultSet resultSet) {
    return abstractJdbcPlusRepository.getObject(resultSet, entityClass);
  }

  /**
   * Extract multiple entities from result set.
   *
   * @param resultSet the result set
   * @return a list of extracted entities
   */
  public List<T> getObjects(final ResultSet resultSet) {
    return new ArrayList<>(abstractJdbcPlusRepository.getObjects(resultSet, entityClass));
  }
}
