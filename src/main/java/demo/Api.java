package demo;

import retrofit.HttpCall;
import retrofit.http.HttpMethod;
import retrofit.http.Path;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public interface Api {
    @HttpMethod.GET("/users/{user}")
    HttpCall<User> getUser(@Path("user")String user);
}
