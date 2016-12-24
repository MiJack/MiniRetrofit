package retrofit.core;

import retrofit.HttpResponse;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public interface HttpCallback<T> {
    void onResponse(HttpCall<T> call, HttpResponse<T,?,?> response);

    void onFailure(HttpCall<T> call, Throwable t);
}
