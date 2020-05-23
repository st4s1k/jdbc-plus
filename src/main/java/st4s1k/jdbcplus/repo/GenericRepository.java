package st4s1k.jdbcplus.repo;

import st4s1k.jdbcplus.utils.EntityUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

interface GenericRepository<T> extends AbstractRepository {

  /**
   * Get the entity class.
   *
   * @return the class of the entity
   */
  Class<T> getEntityClass();

  /**
   * Get database table name associated with the entity.
   *
   * @return table name
   */
  default String getTableName() {
    return EntityUtils.getTableName(getEntityClass());
  }

  /**
   * Create a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} saved entity
   */
  default Optional<T> save(final T entity) {
    return save(entity, getEntityClass());
  }

  /**
   * Update a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} updated entity
   */
  default Optional<T> update(final T entity) {
    return update(entity, getEntityClass());
  }

  /**
   * Remove a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} removed entity
   */
  default Optional<T> remove(final T entity) {
    return remove(entity, getEntityClass());
  }

  /**
   * Find all entities, having entity fields equal to related columns.
   *
   * @param entity the entity
   * @return a list of found entities
   */
  default List<T> find(final T entity) {
    return find(entity, getEntityClass());
  }

  /**
   * Find entity by id.
   *
   * @param id entity id
   * @return {@link Optional} found entity
   */
  default Optional<T> findById(final Object id) {
    return findById(id, getEntityClass());
  }

  /**
   * Fetch all entities.
   *
   * @return a list of found entities
   */
  default List<T> findAll() {
    return findAll(getEntityClass());
  }

  /**
   * Fetch all entities where from associated table,
   * where column has specified value.
   *
   * @param column table column
   * @param value  specified value
   * @return a list of found entities
   */
  default List<T> findByColumn(
      final String column,
      final Object value) {
    return findByColumn(column, value, getEntityClass());
  }

  /**
   * Extract entity from result set
   *
   * @param resultSet the result set
   * @return extracted entity
   */
  default T getObject(final ResultSet resultSet) {
    return getObject(resultSet, getEntityClass());
  }

  /**
   * Extract multiple entities from result set.
   *
   * @param resultSet the result set
   * @return a list of extracted entities
   */
  default List<T> getObjects(final ResultSet resultSet) {
    return new ArrayList<>(getObjects(resultSet, getEntityClass()));
  }
}
