package retrofit;

import retrofit.core.HttpCallAdapter;
import retrofit.util.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */

final class DefaultCallAdapterFactory extends HttpCallAdapter.Factory {
    static final HttpCallAdapter.Factory INSTANCE = new DefaultCallAdapterFactory();

    @Override
    public HttpCallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != HttpCall.class) {
            return null;
        }

        final Type responseType = Utils.getCallResponseType(returnType);
        return new HttpCallAdapter<Object, HttpCall<?>>() {

            public Type responseType() {
                return responseType;
            }

            public HttpCall<Object> adapt(HttpCall<Object> call) {
                return call;
            }
        };
    }
}

