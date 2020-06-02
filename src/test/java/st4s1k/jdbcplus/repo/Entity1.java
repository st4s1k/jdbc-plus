package st4s1k.jdbcplus.repo;

import lombok.Data;
import lombok.ToString;
import st4s1k.jdbcplus.annotations.*;

import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
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
  @JoinTable(
      value = "entity1s_entity3s",
      joinColumn = @JoinColumn("id1"),
      inverseJoinColumn = @JoinColumn("id3"))
  private List<Entity3> entity3s;

  @OneToOne(targetEntity = Entity4.class)
  @JoinColumn("entity4")
  private Entity4 entity4;
}
