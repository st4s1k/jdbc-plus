package st4s1k.jdbcplus.repo;

import lombok.Data;
import st4s1k.jdbcplus.annotations.*;

import java.util.List;

@Data
@Table("entity1s")
public class Entity1 {

  @Id
  private Integer id;

  @Column("name")
  private String name;

  @Column("rank")
  private Integer rank;

  @ManyToOne
  @JoinColumn("entity")
  private Entity entity;

  @ManyToMany(targetEntity = Entity2.class)
  @JoinTable("entity1s_entity2s")
  private List<Entity2> entity2s;

  @ManyToMany
  @JoinTable("entity1s_entity3s")
  private List<Entity3> entity3s;
}
