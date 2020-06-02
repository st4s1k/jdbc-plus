package st4s1k.jdbcplus.repo;

import lombok.Data;
import lombok.ToString;
import st4s1k.jdbcplus.annotations.*;

import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
@Table("entities")
public class Entity {

  @Id
  private Integer id;

  @Column("name")
  private String name;

  @Column("rank")
  private Integer rank;

  @OneToMany(targetEntity = Entity1.class)
  private List<Entity1> entity1s;

  @OneToMany
  private List<Entity2> entity2s;

  @OneToOne(targetEntity = Entity4.class)
  @JoinColumn("entity4")
  private Entity4 entity4;
}
