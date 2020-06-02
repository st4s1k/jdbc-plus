package st4s1k.jdbcplus.repo;

import lombok.Data;
import lombok.ToString;
import st4s1k.jdbcplus.annotations.*;

@Data
@ToString(onlyExplicitlyIncluded = true)
@Table("entity4s")
public class Entity4 {

  @Id
  private Integer id;

  @Column("name")
  private String name;

  @Column("rank")
  private Integer rank;

  @OneToOne(targetEntity = Entity.class)
  @JoinColumn("entity")
  private Entity entity;

  @OneToOne(mappedBy = "entity4")
  @JoinColumn("entity1")
  private Entity1 entity1;
}
