package retrofit.engine.okhttp;

import okhttp3.Call;
import retrofit.ServiceMethod;
import retrofit.core.HttpCall;
import retrofit.http.bean.HttpRequest;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public class OkHttpCall<T> extends HttpCall<T> {
    public Call rawCall;

    public OkHttpCall(ServiceMethod<T, Object> serviceMethod, Object[] args) {
        super(serviceMethod, args);
    }

    @Override
    public HttpCall<T> clone() {
        return new OkHttpCall<T>((ServiceMethod<T, Object>) serviceMethod, args);
    }

    @Override
    public HttpRequest request() {
        return null;
    }
}
