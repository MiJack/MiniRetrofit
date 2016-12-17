package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public class HttpMethod {
@Target(METHOD)@Retention(RUNTIME)public @interface PUT{String value() default "";}
@Target(METHOD)@Retention(RUNTIME)public @interface POST{String value() default "";}
@Target(METHOD)@Retention(RUNTIME)public @interface PATCH{String value() default "";}
@Target(METHOD)@Retention(RUNTIME)public @interface OPTIONS{String value() default "";}
@Target(METHOD)@Retention(RUNTIME)public @interface HTTP{
    String method();
    String path() default "";
    boolean hasBody() default false;}
@Target(METHOD)@Retention(RUNTIME)public @interface GET{String value() default "";}
@Target(METHOD)@Retention(RUNTIME)public @interface DELETE{String value() default "";}
@Target(METHOD)@Retention(RUNTIME)public @interface HEAD{String value() default "";}
}
