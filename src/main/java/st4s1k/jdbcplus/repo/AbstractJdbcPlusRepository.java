package st4s1k.jdbcplus.repo;

import st4s1k.jdbcplus.annotations.*;
import st4s1k.jdbcplus.config.DatabaseConnection;
import st4s1k.jdbcplus.exceptions.InvalidMappingException;
import st4s1k.jdbcplus.exceptions.InvalidResultSetException;
import st4s1k.jdbcplus.exceptions.JdbcPlusException;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static st4s1k.jdbcplus.utils.EntityUtils.*;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.getClassInstance;

public class AbstractJdbcPlusRepository {

  private static volatile AbstractJdbcPlusRepository instance;
  private static volatile DatabaseConnection databaseConnection;
  private static volatile Logger logger;

  public static AbstractJdbcPlusRepository getInstance() {
    if (instance == null) {
      synchronized (AbstractJdbcPlusRepository.class) {
        if (instance == null) {
          instance = new AbstractJdbcPlusRepository();
          databaseConnection = DatabaseConnection.getInstance();
          logger = getLogger("AbstractJdbcPlusRepository");
        }
      }
    }
    return instance;
  }

  private AbstractJdbcPlusRepository() {
  }

  @SuppressWarnings("unchecked")
  public <T> Class<T> getGenerifiedClass(final T entity) {
    return (Class<T>) entity.getClass();
  }

  /**
   * Generates REMOVE sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  public <T> String sqlRemove(final T entity) {
    final Class<?> clazz = entity.getClass();
    final String table = getTableName(clazz);
    final String id = getIdColumnName(clazz);
    final Object idColumnValue = getIdColumnValue(entity);
    final String value = getStringValueForSql(idColumnValue);
    return String.format("delete from %s where %s = %s", table, id, value);
  }

  /**
   * Generates INSERT sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  public <T> String sqlInsert(final T entity) {
    final Class<?> clazz = entity.getClass();
    final String table = getTableName(clazz);
    final String[] fieldNames = getColumnNames(clazz);
    final String[] fieldValues = getColumnValuesAsStringForSQL(entity, clazz);
    final String columns = String.join(", ", fieldNames);
    final String values = String.join(", ", fieldValues);
    return String.format("insert into %s(%s) values (%s)", table, columns, values);
  }

  /**
   * Generates UPDATE sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  public <T> String sqlUpdate(final T entity) {
    final Class<?> clazz = entity.getClass();
    final String table = getTableName(clazz);
    final String idColumnName = getIdColumnName(clazz);
    final Object idValue = getIdColumnValue(entity);
    final String idStringValue = getStringValueForSql(idValue);
    final String columns = getColumnsNameValueMap(entity).entrySet().stream()
        .filter(not(e -> e.getKey().equals(idColumnName)))
        .map(e -> e.getKey() + " = " + e.getValue())
        .collect(Collectors.joining(", "));
    return columns.length() == 0 ? "" :
        String.format(
            "update %s set %s where %s = %s",
            table,
            columns,
            idColumnName,
            idStringValue
        );
  }

  /**
   * Generates SELECT sql query for a table.
   *
   * @param table the database table
   * @return SQL query string
   */
  public String sqlSelectAll(final String table) {
    return String.format("select * from %s", table);
  }

  /**
   * Generates SELECT sql query for a table.
   *
   * @param table  the database table
   * @param column the column name
   * @param value  the column value
   * @return SQL query string
   */
  public String sqlSelectAllByColumn(
      final String table,
      final String column,
      final Object value
  ) {
    return sqlSelectAllByColumns(
        table,
        new String[]{column},
        new Object[]{value}
    );
  }

  /**
   * Generates SELECT sql query for a table.
   *
   * @param table   the database table
   * @param columns the column name
   * @param values  the column value
   * @return SQL query string
   */
  public String sqlSelectAllByColumns(
      final String table,
      final String[] columns,
      final Object[] values
  ) {
    if (columns.length > 0 && columns.length == values.length) {
      final StringBuilder conditions = new StringBuilder();
      for (int i = 0; i < columns.length; i++) {
        if (conditions.length() > 0) {
          conditions.append(", ");
        }
        final String column = columns[i];
        final String value = getStringValueForSql(values[i]);
        conditions.append(column).append(" = ").append(value);
      }
      return sqlSelectAll(table) + " where " + conditions;
    }
    return "";
  }

  /**
   * Create, or Update, a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} saved entity
   */
  public <T> Optional<T> save(final T entity) {
    findById(entity)
        .ifPresentOrElse(
            e -> databaseConnection.updateTransaction(sqlUpdate(e)),
            () -> databaseConnection.updateTransaction(sqlInsert(entity))
        );
    return findById(entity);
  }

  /**
   * Remove a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} removed entity
   */
  public <T> Optional<T> remove(final T entity) {
    final Optional<T> foundEntity = findById(entity);
    foundEntity.ifPresent(e -> databaseConnection.updateTransaction(sqlRemove(e)));
    return foundEntity;
  }

  /**
   * Find all entities, having entity fields equal to related columns.
   *
   * @param entity the entity
   * @return a list of found entities
   */
  public <T> List<T> find(final T entity) {
    return Optional.ofNullable(entity)
        .map(e -> databaseConnection.queryTransaction(
            sqlSelectAllByColumns(
                getTableName(entity.getClass()),
                getColumnNames(entity.getClass()),
                getColumnValues(entity)
            ),
            resultSet -> getObjects(resultSet, getGenerifiedClass(entity)),
            Collections::<T>emptyList
        ))
        .orElse(emptyList());
  }

  /**
   * Fetch all entities.
   *
   * @return a list of found entities
   */
  public <T> List<T> findAll(final Class<T> clazz) {
    return databaseConnection.queryTransaction(
        sqlSelectAll(getTableName(clazz)),
        resultSet -> getObjects(resultSet, clazz),
        Collections::emptyList
    );
  }

  /**
   * Fetch all entities where from associated table,
   * where column has specified value.
   *
   * @param column table column
   * @param value  specified value
   * @return a list of found entities
   */
  public <T> List<T> findByColumn(
      final String column,
      final Object value,
      final Class<T> clazz
  ) {
    return Optional.ofNullable(column)
        .map(field -> databaseConnection.queryTransaction(
            sqlSelectAllByColumn(getTableName(clazz), field, value),
            resultSet -> getObjects(resultSet, clazz),
            Collections::<T>emptyList
        ))
        .orElse(emptyList());
  }

  /**
   * Find entity by id and given Class.
   *
   * @param idValue entity id
   * @param clazz   entity class
   * @return {@link Optional} found entity
   */
  public <X> Optional<X> findById(
      final Object idValue,
      final Class<X> clazz
  ) {
    final List<X> entityList = findByColumn(getIdColumnName(clazz), idValue, clazz);
    return entityList.stream().findFirst();
  }

  /**
   * Find entity by id.
   *
   * @param entity entity
   * @return {@link Optional} found entity
   */
  public <T> Optional<T> findById(final T entity) {
    final Class<T> clazz = getGenerifiedClass(entity);
    final String idColumnName = getIdColumnName(clazz);
    final Object idColumnValue = getIdColumnValue(entity);
    final List<T> entityList = findByColumn(idColumnName, idColumnValue, clazz);
    return Optional.ofNullable(entityList)
        .filter(not(List::isEmpty))
        .filter(list -> list.size() == 1)
        .map(list -> list.get(0));
  }

  /**
   * Extract entity from result set with given Class.
   *
   * @param resultSet the result set
   * @param clazz     entity class object
   * @return extracted entity
   */
  public <T> T getObject(
      final ResultSet resultSet,
      final Class<T> clazz
  ) {
    final T entity = getClassInstance(clazz);
    populateColumnFields(resultSet, entity);
    return entity;
  }

  /**
   * Extract multiple entities from result set with a given entity Class.
   *
   * @param resultSet the result set
   * @param clazz     entity class object
   * @return a list of extracted entities
   */
  public <T> List<T> getObjects(
      final ResultSet resultSet,
      final Class<T> clazz
  ) {
    final List<T> list = new ArrayList<>();
    try {
      while (resultSet.next()) {
        Optional.ofNullable(getObject(resultSet, clazz)).ifPresent(list::add);
      }
    } catch (SQLException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
      return emptyList();
    }
    return list;
  }

  /**
   * Populates entity fields from result set
   * using a map of column names and fields.
   *
   * @param resultSet  the result set
   * @param entity     the entity
   * @param columnsMap a map of column names and fields
   */
  public <T> void populateByColumnsMap(
      final ResultSet resultSet,
      final T entity,
      final Map<String, Field> columnsMap
  ) {
    try {
      final int columnCount = resultSet.getMetaData().getColumnCount();
      for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
        final String columnName =
            resultSet.getMetaData().getColumnName(columnIndex);
        if (columnsMap.containsKey(columnName)) {
          final Field column = columnsMap.get(columnName);
          column.setAccessible(true);
          final Class<?> columnType = column.getType();
          if (columnType.isAnnotationPresent(Table.class)) {
            final Class<?> relatedObjectClass = getTargetEntity(column);
            final Field relatedObjectIdColumn = getIdColumn(relatedObjectClass);
            final Class<?> relatedObjectIdType = relatedObjectIdColumn.getType();
            final Object relatedObjectId = resultSet.getObject(columnIndex, relatedObjectIdType);
            final Object relatedObject = findById(relatedObjectId, relatedObjectClass)
                .orElseThrow(() -> new InvalidResultSetException(String.format(
                    "Cannot find entity of type: %s, with given id: %s",
                    relatedObjectClass.getSimpleName(),
                    relatedObjectId
                )));
            column.set(entity, relatedObject);
          } else {
            column.set(entity, resultSet.getObject(columnIndex, columnType));
          }
        }
      }
    } catch (SQLException | IllegalAccessException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
    }
  }

  /**
   * Populates all the entity fields associated with a column
   * in a table associated with the entity.
   *
   * @param resultSet the result set
   * @param entity    the entity
   */
  public <T> void populateColumnFields(
      final ResultSet resultSet,
      final T entity
  ) {
    populateByColumnsMap(resultSet, entity, getColumnsMap(entity.getClass()));
  }

  /**
   * Populate one {@link OneToMany} field.
   *
   * @param field  field that contains @OneToMany annotation
   * @param entity the entity
   */
  public <T> void populateOneToManyField(
      final Field field,
      final T entity
  ) {
    final Class<?> targetEntity = getTargetEntity(field);
    final String targetEntityTableName = getTableName(targetEntity);
    final Field manyToOneField = getRelationalField(
        targetEntity,
        entity.getClass(),
        ManyToOne.class
    );
    final String targetEntityManyToOneColumnName = getColumnName(manyToOneField);
    final Object currentEntityIdColumnValue = getIdColumnValue(entity);
    final String query = sqlSelectAllByColumn(
        targetEntityTableName,
        targetEntityManyToOneColumnName,
        currentEntityIdColumnValue
    );

    populateField(
        field, entity, query,
        resultSet -> getObjects(resultSet, targetEntity),
        Collections::emptyList
    );
  }

  public <T, R> void populateField(
      final Field field,
      final T entity,
      final String query,
      final Function<ResultSet, R> resultSetFunction,
      final Supplier<R> defaultResult
  ) {
    final R result = databaseConnection.queryTransaction(
        query,
        resultSetFunction,
        defaultResult
    );
    field.setAccessible(true);
    try {
      field.set(entity, result);
    } catch (IllegalAccessException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
    }
  }

  /**
   * Populate {@link OneToMany} fields.
   *
   * @param entity the entity
   */
  public <T> void populateOneToManyFields(final T entity) {
    for (Field field : getOneToManyFields(entity.getClass())) {
      populateOneToManyField(field, entity);
    }
  }

  /**
   * Populate one {@link ManyToMany} field.
   *
   * @param <T>    entity type
   * @param field  field that contains @OneToMany|@ManyToMany annotation
   *               and returns a table name
   * @param entity the entity
   */
  public <T> void populateManyToManyField(
      final Field field,
      final T entity
  ) {
    if (Collection.class.isAssignableFrom(field.getType())) {
      final Class<?> targetEntity = getTargetEntity(field);
      final JoinTable joinTable = getJoinTable(field);
      final JoinColumn joinColumn = joinTable.joinColumn();
      final JoinColumn inverseJoinColumn = joinTable.inverseJoinColumn();
      final Class<T> entityClass = getGenerifiedClass(entity);
      final String currentEntityIdColumnName = getEntityJoinColumnName(entityClass, joinColumn);
      final String targetEntityIdColumnName = getEntityJoinColumnName(
          targetEntity,
          inverseJoinColumn
      );
      final String query = sqlSelectAllByColumn(
          joinTable.name(),
          currentEntityIdColumnName,
          getIdColumnValue(entity)
      );
      populateField(
          field, entity, query,
          resultSet -> fetchEntitiesByIdColumn(
              resultSet,
              targetEntityIdColumnName,
              targetEntity
          ),
          Collections::emptyList
      );
    } else {
      throw new InvalidMappingException(
          "@ManyToMany annotated field is not of a collection type!"
      );
    }
  }

  /**
   * Fetch all the entities related to the current entity.
   *
   * @param resultSet    the result set
   * @param idColumnName target entity id column name
   * @param clazz        class object of the target entity
   * @param <T>          specific type of the target entity
   * @return a list of related entities
   */
  public <T> List<T> fetchEntitiesByIdColumn(
      final ResultSet resultSet,
      final String idColumnName,
      final Class<T> clazz
  ) {
    final List<T> list = new ArrayList<>();
    try {
      while (resultSet.next()) {
        final int idColumnNumber = getColumnNumber(resultSet, idColumnName);
        final Object idColumnValue = resultSet.getObject(idColumnNumber);
        final T foundEntity = findById(idColumnValue, clazz)
            .orElseThrow(() -> new InvalidResultSetException(String.format(
                "Cannot find entity %s by id: %s", clazz.getName(), idColumnValue
            )));
        list.add(foundEntity);
      }
    } catch (SQLException e) {
      logger.log(ERROR, e.getLocalizedMessage(), e);
      return emptyList();
    }
    return list;
  }

  private int getColumnNumber(
      final ResultSet resultSet,
      final String columnName
  ) {
    try {
      return resultSet.findColumn(columnName);
    } catch (SQLException e) {
      throw new JdbcPlusException(e);
    }
  }

  /**
   * Populate {@link ManyToMany} fields.
   *
   * @param entity the entity
   */
  public <T> void populateManyToManyFields(final T entity) {
    for (Field field : getManyToManyFields(entity.getClass())) {
      populateManyToManyField(field, entity);
    }
  }
}
