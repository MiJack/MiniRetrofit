package retrofit.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Streaming {
}
