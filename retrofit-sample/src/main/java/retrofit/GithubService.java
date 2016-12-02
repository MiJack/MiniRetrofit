package retrofit;

import retrofit.http.method.GET;
import retrofit.http.request.Path;

/**
 * @author Mr.Yuan
 * @since 2016/11/28.
 */
public interface GithubService {
    @GET
    User getUser(@Path("user") String user);
}
