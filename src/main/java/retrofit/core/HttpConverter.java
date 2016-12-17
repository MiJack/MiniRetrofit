package retrofit.core;

import retrofit.RequestBody;
import retrofit.ResponseBody;
import retrofit.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public interface HttpConverter<F, T> {
    T convert(F value) throws IOException;

    class Factory {
        public HttpConverter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                                    Retrofit retrofit) {
            return null;
        }

        public HttpConverter<?, RequestBody> requestBodyConverter(Type type,
                                                                  Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
            return null;
        }

        public HttpConverter<?, String> stringConverter(Type type, Annotation[] annotations,
                                                        Retrofit retrofit) {
            return null;
        }
    }
}

