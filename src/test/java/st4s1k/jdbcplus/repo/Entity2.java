package st4s1k.jdbcplus.repo;

import lombok.Data;
import st4s1k.jdbcplus.annotations.*;

@Data
@Table("entity2s")
public class Entity2 {

  @Id
  private Integer id;

  @Column("name")
  private String name;

  @Column("rank")
  private Integer rank;

  @ManyToOne
  @JoinColumn("entity")
  private Entity entity;

  @ManyToOne(targetEntity = Entity3.class)
  @JoinColumn("entity3")
  private Entity3 entity3;
}
