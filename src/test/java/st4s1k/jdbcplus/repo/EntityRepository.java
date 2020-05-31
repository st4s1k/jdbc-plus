package st4s1k.jdbcplus.repo;

public class EntityRepository extends JdbcPlusRepository<Entity> {
  public EntityRepository(final AbstractJdbcPlusRepository abstractJdbcPlusRepository) {
    super(abstractJdbcPlusRepository);
  }
}
