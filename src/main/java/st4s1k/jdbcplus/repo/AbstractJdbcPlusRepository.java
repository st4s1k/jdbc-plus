package st4s1k.jdbcplus.repo;

import st4s1k.jdbcplus.annotations.*;
import st4s1k.jdbcplus.config.DatabaseConnection;
import st4s1k.jdbcplus.exceptions.InvalidMappingException;
import st4s1k.jdbcplus.exceptions.InvalidResultSetException;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static st4s1k.jdbcplus.utils.EntityUtils.*;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.getClassInstance;

public class AbstractJdbcPlusRepository {

  private final Logger logger;

  private final DatabaseConnection databaseConnection;

  public AbstractJdbcPlusRepository(final DatabaseConnection databaseConnection) {
    this.databaseConnection = databaseConnection;
    logger = getLogger("AbstractJdbcPlusRepository");
  }

  public AbstractJdbcPlusRepository(
      final DatabaseConnection databaseConnection,
      final Logger logger
  ) {
    this.logger = logger;
    this.databaseConnection = databaseConnection;
  }

  /**
   * Generates REMOVE sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  public <X> String sqlRemove(
      final X entity,
      final Class<X> clazz
  ) {
    final String table = getTableName(clazz);
    final String id = getIdColumnName(clazz);
    final Object idColumnValue = getIdColumnValue(entity, clazz);
    final String value = getStringValueForSQL(idColumnValue);
    return String.format("delete from %s where %s = %s returning *", table, id, value);
  }

  /**
   * Generates INSERT sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  public <X> String sqlInsert(
      final X entity,
      final Class<X> clazz
  ) {
    final String table = getTableName(clazz);
    final StringBuilder columns = new StringBuilder();
    final StringBuilder values = new StringBuilder();
    final String[] fieldNames = getColumnNames(entity.getClass());
    final String[] fieldValues = getColumnValuesAsStringForSQL(entity, clazz);
    for (int i = 0; i < fieldNames.length; i++) {
      if (Optional.ofNullable(fieldValues[i]).isPresent()) {
        if (columns.length() > 0) {
          columns.append(", ");
          values.append(", ");
        }
        columns.append(fieldNames[i]);
        values.append(fieldValues[i]);
      }
    }
    return String.format("insert into %s(%s) values (%s) returning *", table, columns, values);
  }

  /**
   * Generates UPDATE sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  public <X> String sqlUpdate(
      final X entity,
      final Class<X> clazz
  ) {
    final String table = getTableName(clazz);
    final String idColumnName = getIdColumnName(clazz);
    final Object idValue = getIdColumnValue(entity, clazz);
    final String idStringValue = getStringValueForSQL(idValue);
    final String[] fieldNames = getColumnNames(clazz);
    final String[] fieldValues = getColumnValuesAsStringForSQL(entity, clazz);
    final StringBuilder columns = new StringBuilder();
    for (int i = 0; i < fieldNames.length; i++) {
      if (fieldNames[i].equals(idColumnName)) {
        continue;
      }
      if (columns.length() > 0) {
        columns.append(", ");
      }
      columns.append(fieldNames[i]).append(" = ").append(fieldValues[i]);
    }
    return columns.length() == 0 ? "" :
        String.format(
            "update %s set %s where %s = %s returning *",
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
        final String value = getStringValueForSQL(values[i]);
        conditions.append(column).append(" = ").append(value);
      }
      return sqlSelectAll(table) + " where " + conditions;
    }
    return "";
  }

  /**
   * Create a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} saved entity
   */
  public <X> Optional<X> save(
      final X entity,
      final Class<X> clazz
  ) {
    return databaseConnection
        .queryTransaction(
            sqlInsert(entity, clazz),
            resultSet -> Optional.ofNullable(getObject(resultSet, clazz)),
            Optional::empty
        );
  }

  /**
   * Update a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} updated entity
   */
  public <X> Optional<X> update(
      final X entity,
      final Class<X> clazz
  ) {
    return findById(getIdColumnValue(entity, clazz), clazz)
        .flatMap(e -> databaseConnection.queryTransaction(
            sqlUpdate(e, clazz),
            resultSet -> getObject(resultSet, clazz)
        ));
  }

  /**
   * Remove a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} removed entity
   */
  public <X> Optional<X> remove(
      final X entity,
      final Class<X> clazz
  ) {
    return findById(getIdColumnValue(entity, clazz), clazz)
        .flatMap(e -> databaseConnection.queryTransaction(
            sqlRemove(e, clazz),
            resultSet -> getObject(resultSet, clazz)
        ));
  }

  /**
   * Find all entities, having entity fields equal to related columns.
   *
   * @param entity the entity
   * @return a list of found entities
   */
  public <X> List<X> find(
      final X entity,
      final Class<X> clazz
  ) {
    return Optional.ofNullable(entity)
        .map(e -> databaseConnection
            .queryTransaction(
                sqlSelectAllByColumns(
                    getTableName(clazz),
                    getColumnNames(clazz),
                    getColumnValues(entity, clazz)
                ),
                resultSet -> getObjects(resultSet, clazz),
                Collections::<X>emptyList
            ))
        .orElse(emptyList());
  }

  /**
   * Fetch all entities.
   *
   * @return a list of found entities
   */
  public <X> List<X> findAll(final Class<X> clazz) {
    return databaseConnection
        .queryTransaction(
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
  public <X> List<X> findByColumn(
      final String column,
      final Object value,
      final Class<X> clazz
  ) {
    return Optional.ofNullable(column)
        .map(field -> databaseConnection
            .queryTransaction(
                sqlSelectAllByColumn(
                    getTableName(clazz),
                    field,
                    value
                ),
                resultSet -> getObjects(resultSet, clazz),
                Collections::<X>emptyList
            ))
        .orElse(emptyList());
  }

  /**
   * Find entity by id and given Class.
   *
   * @param id    entity id
   * @param clazz entity class
   * @return {@link Optional} found entity
   */
  public <X> Optional<X> findById(
      final Object id,
      final Class<X> clazz
  ) {
    final List<X> entityList = findByColumn(getIdColumnName(clazz), id, clazz);
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
  public <X> X getObject(
      final ResultSet resultSet,
      final Class<X> clazz
  ) {
    final X entity = getClassInstance(clazz);
    populateColumnFields(resultSet, entity, clazz);
    return entity;
  }

  /**
   * Extract multiple entities from result set with a given entity Class.
   *
   * @param resultSet the result set
   * @param clazz     entity class object
   * @return a list of extracted entities
   */
  public <X> List<X> getObjects(
      final ResultSet resultSet,
      final Class<X> clazz
  ) {
    final List<X> list = new ArrayList<>();
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
  public <X> void populateByColumnsMap(
      final ResultSet resultSet,
      final X entity,
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
          column.set(entity, resultSet.getObject(columnIndex, column.getType()));
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
  public <X> void populateColumnFields(
      final ResultSet resultSet,
      final X entity,
      final Class<X> clazz
  ) {
    populateByColumnsMap(resultSet, entity, getColumnsMap(clazz));
  }

  /**
   * Populate one {@link OneToMany} field.
   *
   * @param entity the entity
   * @param field  field that contains @OneToMany annotation
   */
  public <X> void populateOneToManyField(
      final Field field,
      final X entity,
      final Class<X> clazz
  ) {
    final Class<?> targetEntity = getTargetEntity(field);
    final String targetEntityTableName = getTableName(targetEntity);
    final Field manyToOneField = getRelationalField(targetEntity, clazz, ManyToOne.class);
    final String targetEntityManyToOneColumnName = getColumnName(manyToOneField);
    final Object currentEntityIdColumnValue = getIdColumnValue(entity, clazz);
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

  public <X, R> void populateField(
      final Field field,
      final X entity,
      final String query,
      final Function<ResultSet, R> resultSetFunction,
      final Supplier<R> defaultResult
  ) {
    final R result = databaseConnection
        .queryTransaction(
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
  public <X> void populateOneToManyFields(
      final X entity,
      final Class<X> clazz
  ) {
    for (Field field : getOneToManyFields(clazz)) {
      populateOneToManyField(field, entity, clazz);
    }
  }

  /**
   * Populate one {@link ManyToMany} field.
   *
   * @param field  field that contains @OneToMany|@ManyToMany annotation
   *               and returns a table name
   * @param entity the entity
   * @param clazz  entity class
   * @param <X>    entity type
   */
  public <X> void populateManyToManyField(
      final Field field,
      final X entity,
      final Class<X> clazz
  ) {
    if (Collection.class.isAssignableFrom(field.getType())) {
      final Class<?> targetEntity = getTargetEntity(field);
      final JoinTable joinTable = getJoinTable(field, targetEntity);
      final JoinColumn joinColumn = joinTable.joinColumn();
      final JoinColumn inverseJoinColumn = joinTable.inverseJoinColumn();
      final String currentEntityIdColumnName = getEntityJoinColumnName(clazz, joinColumn);
      final String targetEntityIdColumnName = getEntityJoinColumnName(
          targetEntity,
          inverseJoinColumn
      );
      final String query = sqlSelectAllByColumn(
          joinTable.value(),
          currentEntityIdColumnName,
          getIdColumnValue(entity, clazz)
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
   * @param <X>          specific type of the target entity
   * @return a list of related entities
   */
  public <X> List<X> fetchEntitiesByIdColumn(
      final ResultSet resultSet,
      final String idColumnName,
      final Class<X> clazz
  ) {
    final List<X> list = new ArrayList<>();
    try {
      while (resultSet.next()) {
        final int idColumnNumber = getColumnNumber(resultSet, idColumnName);
        final Object idColumnValue = resultSet.getObject(idColumnNumber);
        final X foundEntity = findById(idColumnValue, clazz)
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

  private int getColumnNumber(final ResultSet resultSet, final String columnName) {
    try {
      return resultSet.findColumn(columnName);
    } catch (SQLException e) {
      throw new InvalidResultSetException(String.format(
          "Result set column does not exist: %s",
          columnName
      ));
    }
  }

  /**
   * Populate {@link ManyToMany} fields.
   *
   * @param entity the entity
   */
  public <X> void populateManyToManyFields(
      final X entity,
      final Class<X> clazz
  ) {
    for (Field field : getManyToManyFields(clazz)) {
      populateManyToManyField(field, entity, clazz);
    }
  }
}
