package st4s1k.jdbcplus.repo;

import lombok.Data;
import lombok.ToString;
import st4s1k.jdbcplus.annotations.*;

import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
@Table("entity3s")
public class Entity3 {

  @Id
  private Integer id;

  @Column("name")
  private String name;

  @Column("rank")
  private Integer rank;

  @ManyToMany(mappedBy = "entity3s")
  private List<Entity1> entity1s;

  @OneToMany(targetEntity = Entity2.class)
  private List<Entity2> entity2s;
}
