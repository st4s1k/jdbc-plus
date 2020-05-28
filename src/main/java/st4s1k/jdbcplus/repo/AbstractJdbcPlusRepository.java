package st4s1k.jdbcplus.repo;

import st4s1k.jdbcplus.annotations.ManyToMany;
import st4s1k.jdbcplus.annotations.OneToMany;
import st4s1k.jdbcplus.config.DatabaseConnection;
import st4s1k.jdbcplus.exceptions.InvalidMappingException;
import st4s1k.jdbcplus.exceptions.InvalidResultSetException;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static st4s1k.jdbcplus.utils.EntityUtils.*;
import static st4s1k.jdbcplus.utils.JdbcPlusUtils.getClassInstance;

public interface AbstractJdbcPlusRepository {

  /**
   * Get database connection object.
   *
   * @return database connection
   */
  DatabaseConnection getDatabaseConnection();

  /**
   * Get {@link String} value for building SQL query.
   *
   * @return string value
   */
  default String getStringValueForSql(final Object value) {
    if (value == null) {
      return "NULL";
    } else if (!value.getClass().isArray()) {
      if (value instanceof String ||
          value instanceof Character) {
        return "'" + value + "'";
      }
      return String.valueOf(value);
    } else {
      if (value instanceof Object[]) {
        Object[] objArr = (Object[]) value;
        if (value instanceof String[] ||
            value instanceof Character[]) {
          final Object[] strArr = (Object[]) value;
          objArr = new String[strArr.length];
          for (int i = 0; i < strArr.length; i++) {
            objArr[i] = "'" + strArr[i] + "'";
          }
        }
        return Arrays.toString(objArr);
      } else if (value instanceof long[]) {
        return Arrays.toString((long[]) value);
      } else if (value instanceof int[]) {
        return Arrays.toString((int[]) value);
      } else if (value instanceof short[]) {
        return Arrays.toString((short[]) value);
      } else if (value instanceof char[]) {
        final char[] charArr = (char[]) value;
        final String[] newCharArr = new String[charArr.length];
        for (int i = 0; i < charArr.length; i++) {
          newCharArr[i] = "'" + charArr[i] + "'";
        }
        return Arrays.toString(newCharArr);
      } else if (value instanceof byte[]) {
        return Arrays.toString((byte[]) value);
      } else if (value instanceof boolean[]) {
        return Arrays.toString((boolean[]) value);
      } else if (value instanceof float[]) {
        return Arrays.toString((float[]) value);
      } else if (value instanceof double[]) {
        return Arrays.toString((double[]) value);
      }
      return "";
    }
  }

  /**
   * Generates REMOVE sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  default <X> String sqlRemove(
      final X entity,
      final Class<X> clazz
  ) {
    final String table = getTableName(clazz);
    final String id = getIdColumnName(clazz);
    final Object idColumnValue = getIdColumnValue(entity, clazz);
    final String value = getStringValueForSql(idColumnValue);
    return String.format("delete from %s where %s = %s returning *", table, id, value);
  }

  /**
   * Generates INSERT sql query for an entity.
   *
   * @param entity the entity
   * @return SQL query string
   */
  default <X> String sqlInsert(
      final X entity,
      final Class<X> clazz
  ) {
    final String table = getTableName(clazz);
    final StringBuilder columns = new StringBuilder();
    final StringBuilder values = new StringBuilder();
    final String[] fieldNames = getColumnNames(entity.getClass());
    final String[] fieldValues = getColumnValuesAsString(entity, clazz);
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
  default <X> String sqlUpdate(
      final X entity,
      final Class<X> clazz
  ) {
    final String table = getTableName(clazz);
    final String idColumnName = getIdColumnName(clazz);
    final Object idValue = getIdColumnValue(entity, clazz);
    final String idStringValue = getStringValueForSql(idValue);
    final String[] fieldNames = getColumnNames(clazz);
    final String[] fieldValues = getColumnValuesAsString(entity, clazz);
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
  default String sqlSelectAll(final String table) {
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
  default String sqlSelectAllByColumn(
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
  default String sqlSelectAllByColumns(
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
   * Create a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} saved entity
   */
  default <X> Optional<X> save(
      final X entity,
      final Class<X> clazz
  ) {
    return getDatabaseConnection()
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
  default <X> Optional<X> update(
      final X entity,
      final Class<X> clazz
  ) {
    return findById(getIdColumnValue(entity, clazz), clazz)
        .flatMap(e -> getDatabaseConnection()
            .queryTransaction(
                sqlUpdate(e, clazz),
                resultSet -> Optional.ofNullable(getObject(resultSet, clazz)),
                Optional::empty
            ));
  }

  /**
   * Remove a row in the table associated with the entity.
   *
   * @param entity the entity
   * @return {@link Optional} removed entity
   */
  default <X> Optional<X> remove(
      final X entity,
      final Class<X> clazz
  ) {
    return findById(getIdColumnValue(entity, clazz), clazz)
        .flatMap(e -> getDatabaseConnection()
            .queryTransaction(
                sqlRemove(e, clazz),
                resultSet -> Optional.ofNullable(getObject(resultSet, clazz)),
                Optional::empty
            ));
  }

  /**
   * Find all entities, having entity fields equal to related columns.
   *
   * @param entity the entity
   * @return a list of found entities
   */
  default <X> List<X> find(
      final X entity,
      final Class<X> clazz
  ) {
    return Optional.ofNullable(entity)
        .map(e -> getDatabaseConnection()
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
  default <X> List<X> findAll(final Class<X> clazz) {
    return getDatabaseConnection()
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
  default <X> List<X> findByColumn(
      final String column,
      final Object value,
      final Class<X> clazz
  ) {
    return Optional.ofNullable(column)
        .map(field -> getDatabaseConnection()
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
   * @param id entity id
   * @return {@link Optional} found entity
   */
  default <X> Optional<X> findById(
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
  default <X> X getObject(
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
  default <X> List<X> getObjects(
      final ResultSet resultSet,
      final Class<X> clazz
  ) {
    final List<X> list = new ArrayList<>();
    try {
      while (resultSet.next()) {
        Optional.ofNullable(getObject(resultSet, clazz)).ifPresent(list::add);
      }
    } catch (SQLException e) {
      e.printStackTrace();
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
   * @throws SQLException           if a database access error occurs
   *                                or a method is called on a closed result set
   * @throws IllegalAccessException when {@link Field#set(Object, Object)}
   *                                method is called
   *                                and it is enforcing Java language access
   *                                control and
   *                                the underlying field is either inaccessible
   *                                or final.
   */
  default <X> void populateByColumnsMap(
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
      e.printStackTrace();
    }
  }

  /**
   * Populates all the entity fields associated with a column
   * in a table associated with the entity.
   *
   * @param resultSet the result set
   * @param entity    the entity
   * @throws SQLException           if a database access error occurs
   *                                or a method is called on a closed result set
   * @throws IllegalAccessException when {@link Field#set(Object, Object)}
   *                                method is called
   *                                and it is enforcing Java language access
   *                                control and
   *                                the underlying field is either inaccessible
   *                                or final.
   */
  default <X> void populateColumnFields(
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
   * @param field  field that contains @OneToMany|@ManyToMany annotation
   *               and returns a table name
   * @throws IllegalAccessException when {@link Field#set(Object, Object)}
   *                                method is called
   *                                and it is enforcing Java language access
   *                                control and
   *                                the underlying field is either inaccessible
   *                                or final
   */
  default <X> void populateOneToManyField(
      final Field field,
      final X entity,
      final Class<X> clazz
  ) throws IllegalAccessException {

    final String query =
        sqlSelectAllByColumn(
            getTableName(clazz),
            getIdColumnName(clazz),
            getIdColumnValue(entity, clazz)
        );

    populateField(
        field, entity, query,
        resultSet -> getObjects(
            resultSet,
            getTargetEntity(field)
        ),
        Collections::emptyList
    );
  }

  default <X, R> void populateField(
      final Field field,
      final X entity,
      final String query,
      final Function<ResultSet, R> resultSetFunction,
      final Supplier<R> defaultResult
  ) throws IllegalAccessException {
    final R result = getDatabaseConnection()
        .queryTransaction(
            query,
            resultSetFunction,
            defaultResult
        );
    field.setAccessible(true);
    field.set(entity, result);
  }

  /**
   * Populate {@link OneToMany} fields.
   *
   * @param entity the entity
   * @throws IllegalAccessException when {@link Field#set(Object, Object)}
   *                                method is called
   *                                and it is enforcing Java language access
   *                                control and
   *                                the underlying field is either inaccessible
   *                                or final
   */
  default <X> void populateOneToManyFields(
      final X entity,
      final Class<X> clazz
  ) throws IllegalAccessException {
    for (Field field : getOneToManyFields(clazz)) {
      populateOneToManyField(field, entity, clazz);
    }
  }

  /**
   * Populate one {@link ManyToMany} field.
   *
   * @param entity the entity
   * @param field  field that contains @OneToMany|@ManyToMany annotation
   *               and returns a table name
   * @throws IllegalAccessException when {@link Field#set(Object, Object)}
   *                                method is called
   *                                and it is enforcing Java language access
   *                                control and
   *                                the underlying field is either inaccessible
   *                                or final
   */
  default <X> void populateManyToManyField(
      final X entity,
      final Field field,
      final Class<X> clazz
  ) throws IllegalAccessException {

    if (Collection.class.isAssignableFrom(field.getType())) {

      final Class<X> targetEntity = getTargetEntity(field);
      final String currentEntityIdColumnName = getIdColumnName(clazz);
      final String targetEntityIdColumnName = getTableName(clazz);
      final String query = sqlSelectAllByColumn(
          getJoinTableName(field),
          currentEntityIdColumnName,
          getIdColumnValue(entity, clazz)
      );

      populateField(
          field, entity, query,
          resultSet -> fetchRelatedEntities(
              resultSet,
              currentEntityIdColumnName,
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
   * @param resultSet                 the result set
   * @param currentEntityIdColumnName current entity id column name
   * @param targetEntityIdColumnName  target entity id column name
   * @param targetEntity              class object of the target entity
   * @param <X>                       specific type of the target entity
   * @return a list of related entities
   */
  default <X> List<X> fetchRelatedEntities(
      final ResultSet resultSet,
      final String currentEntityIdColumnName,
      final String targetEntityIdColumnName,
      final Class<X> targetEntity
  ) {
    final List<X> list = new ArrayList<>();
    try {
      final ResultSetMetaData metaData = resultSet.getMetaData();
      final boolean validManyToManyResultSet = validManyToManyResultSet(
          metaData,
          currentEntityIdColumnName,
          targetEntityIdColumnName
      );
      if (validManyToManyResultSet) {
        while (resultSet.next()) {
          Object targetEntityIdColumnValue =
              targetEntityIdColumnName.equals(metaData.getColumnName(1)) ?
                  resultSet.getObject(1) : resultSet.getObject(2);
          findById(targetEntityIdColumnValue, targetEntity).ifPresent(list::add);
        }
      } else {
        throw new InvalidResultSetException(
            "Result set columns do not match related entities' id columns!"
        );
      }
    } catch (SQLException | InvalidResultSetException e) {
      e.printStackTrace();
      return emptyList();
    }
    return list;
  }

  /**
   * Validate {@link ManyToMany} join-table.
   *
   * @param metaData                  result set meta-data
   * @param currentEntityIdColumnName current entity id column name
   * @param targetEntityIdColumnName  target entity id column name
   * @return TRUE if result set consists of 2 columns and
   * each column has an association with the entity
   * @throws SQLException if a database access error occurs
   *                      or a method is called on a closed result set
   */
  default boolean validManyToManyResultSet(
      final ResultSetMetaData metaData,
      final String currentEntityIdColumnName,
      final String targetEntityIdColumnName
  ) throws SQLException {
    return Objects.nonNull(metaData) && metaData.getColumnCount() == 2 && (
        (
            metaData.getColumnName(1).equals(currentEntityIdColumnName)
                && metaData.getColumnName(2).equals(targetEntityIdColumnName)
        ) || (
            metaData.getColumnName(2).equals(currentEntityIdColumnName)
                && metaData.getColumnName(1).equals(targetEntityIdColumnName)
        )
    );
  }

  /**
   * Populate {@link ManyToMany} fields.
   *
   * @param entity the entity
   * @throws IllegalAccessException when {@link Field#set(Object, Object)}
   *                                method is called
   *                                and it is enforcing Java language access
   *                                control and
   *                                the underlying field is either inaccessible
   *                                or final
   */
  default <X> void populateManyToManyFields(
      final X entity,
      final Class<X> clazz
  ) throws IllegalAccessException {
    for (Field field : getManyToManyFields(clazz)) {
      populateManyToManyField(entity, field, clazz);
    }
  }
}
