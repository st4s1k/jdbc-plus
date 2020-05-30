package st4s1k.jdbcplus.repo;

import lombok.Data;
import st4s1k.jdbcplus.annotations.*;

import java.util.List;

@Data
@Table("entity3s")
public class Entity3 {

  @Id
  private Integer id;

  @Column("name")
  private String name;

  @Column("rank")
  private Integer rank;

  @ManyToMany
  @JoinTable("entity1s_entity3s")
  private List<Entity1> entity1s;

  @OneToMany(targetEntity = Entity2.class)
  private List<Entity2> entity2s;
}
