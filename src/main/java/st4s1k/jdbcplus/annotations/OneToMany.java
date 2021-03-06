package st4s1k.jdbcplus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToMany {

  Class<?> targetEntity() default void.class;

  // TODO: Implement handling for this parameter
  String mappedBy() default "";
}
