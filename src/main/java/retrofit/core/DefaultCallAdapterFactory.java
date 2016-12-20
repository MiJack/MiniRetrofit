package retrofit.core;

import retrofit.Retrofit;
import retrofit.http.bean.HttpResponse;
import retrofit.util.Utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */

public final class DefaultCallAdapterFactory extends HttpCallAdapter.Factory {
    static final HttpCallAdapter.Factory INSTANCE = new DefaultCallAdapterFactory();

    private DefaultCallAdapterFactory() {
    }

    public static HttpCallAdapter.Factory getInstance() {
        return INSTANCE;
    }

    @Override
    public HttpCallAdapter<?, ?> get(final Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != HttpCall.class
                && !getRawType(returnType).getName().equals(HttpResponse.class.getName())) {
            return null;
        }

        final Type responseType = Utils.getCallResponseType(returnType);
        return new HttpCallAdapter<Object, Object>() {

            public Type responseType() {
                return responseType;
            }

            public Object adapt(HttpCall<Object> call)  {
                if (getRawType(returnType) == HttpCall.class) {
                    return call;
                }
                    try {
                        return call.execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

        };
    }
}

