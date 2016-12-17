package retrofit;

import retrofit.core.HttpConverter;
import retrofit.http.Streaming;
import retrofit.util.Utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
final class BuiltInConverters extends HttpConverter.Factory {
    @Override
    public HttpConverter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                                Retrofit retrofit) {
        if (type == ResponseBody.class) {
            return Utils.isAnnotationPresent(annotations, Streaming.class)
                    ? StreamingResponseBodyConverter.INSTANCE
                    : BufferingResponseBodyConverter.INSTANCE;
        }
        if (type == Void.class) {
            return VoidResponseBodyConverter.INSTANCE;
        }
        return null;
    }

    @Override
    public HttpConverter<?, RequestBody> requestBodyConverter(Type type,
                                                              Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        if (RequestBody.class.isAssignableFrom(Utils.getRawType(type))) {
            return RequestBodyConverter.INSTANCE;
        }
        return null;
    }

    static final class VoidResponseBodyConverter implements HttpConverter<ResponseBody, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override public Void convert(ResponseBody value) throws IOException {
//            value.close();
            return null;
        }
    }

    static final class RequestBodyConverter implements HttpConverter<RequestBody, RequestBody> {
        static final RequestBodyConverter INSTANCE = new RequestBodyConverter();

        @Override public RequestBody convert(RequestBody value) throws IOException {
            return value;
        }
    }

    static final class StreamingResponseBodyConverter
            implements HttpConverter<ResponseBody, ResponseBody> {
        static final StreamingResponseBodyConverter INSTANCE = new StreamingResponseBodyConverter();

        @Override public ResponseBody convert(ResponseBody value) throws IOException {
            return value;
        }
    }

    static final class BufferingResponseBodyConverter
            implements HttpConverter<ResponseBody, ResponseBody> {
        static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

        @Override public ResponseBody convert(ResponseBody value) throws IOException {
            try {
                // Buffer the entire body to avoid future I/O.
                return Utils.buffer(value);
            } finally {
                // FIXME: 2016/12/17
//                value.close();
            }
        }
    }

    static final class ToStringConverter implements HttpConverter<Object, String> {
        static final ToStringConverter INSTANCE = new ToStringConverter();

        @Override public String convert(Object value) {
            return value.toString();
        }
    }
}

