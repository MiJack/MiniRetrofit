package retrofit.core;

import retrofit.Retrofit;
import retrofit.util.Utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */

public interface HttpCallAdapter<T, R> {

    Type responseType();

    T adapt(HttpCall<R> call);

    abstract class Factory {
        public abstract HttpCallAdapter<?, ?> get(Type returnType, Annotation[] annotations,
                                                  Retrofit retrofit);

        protected static Type getParameterUpperBound(int index, ParameterizedType type) {
            return Utils.getParameterUpperBound(index, type);
        }

        protected static Class<?> getRawType(Type type) {
            return Utils.getRawType(type);
        }
    }
}
