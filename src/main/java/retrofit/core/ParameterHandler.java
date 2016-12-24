package retrofit.core;

import retrofit.RequestBuilder;

import java.io.IOException;
import java.lang.reflect.Array;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 * @param <T> 对应的值
 */
public abstract class ParameterHandler< T> {
    public abstract void apply(RequestBuilder builder, T value) throws IOException;

    public final ParameterHandler<Iterable<T>> iterable() {
        return new ParameterHandler<Iterable<T>>() {
            @Override
            public void apply(RequestBuilder builder, Iterable<T> values) throws IOException {
                if (values == null) return; // Skip null values.

                for (T value : values) {
                    ParameterHandler.this.apply(builder, value);
                }
            }
        };
    }

    public final ParameterHandler<Object> array() {
        return new ParameterHandler<Object>() {
            @Override
            public void apply(RequestBuilder builder, Object values) throws IOException {
                if (values == null) return; // Skip null values.

                for (int i = 0, size = Array.getLength(values); i < size; i++) {
                    //noinspection unchecked
                    ParameterHandler.this.apply(builder, (T) Array.get(values, i));
                }
            }
        };
    }

}
