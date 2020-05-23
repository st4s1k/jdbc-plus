package st4s1k.jdbcplus.repo;

import lombok.Data;
import st4s1k.jdbcplus.annotations.Column;
import st4s1k.jdbcplus.annotations.Id;
import st4s1k.jdbcplus.annotations.Table;

@Data
@Table("entities")
public class Entity {

    @Id
    private Integer id;

    @Column("name")
    private String name;
}
