package demo;

import retrofit.core.HttpCall;
import retrofit.http.HttpMethod;
import retrofit.http.Path;
import rx.Observable;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public interface Api {
    @HttpMethod.GET("/users/{user}")
    HttpCall<User> getUser(@Path("user")String user);
    @HttpMethod.GET("/users/{user}")
    Observable<User> getUser2(@Path("user")String user);
    @HttpMethod.GET("/users/{user}")
    HttpCall<User> getUser3(@Path("user")String user);
}
