package retrofit.engine;

import retrofit.core.HttpCall;
import retrofit.http.bean.HttpResponse;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public interface HttpCallback<T> {
    void onResponse(HttpCall<T> call, HttpResponse<T,?,?> response);

    void onFailure(HttpCall<T> call, Throwable t);
}
