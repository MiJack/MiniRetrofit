package retrofit.core;

import retrofit.RequestBody;
import retrofit.Retrofit;
import retrofit.http.Streaming;
import retrofit.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public final class BuiltInConverters extends HttpConverter.Factory {
    @Override
    public HttpConverter<InputStream, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                               Retrofit retrofit) {
        if (type == InputStream.class) {
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

    static final class VoidResponseBodyConverter implements HttpConverter<InputStream, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override public Void convert(InputStream value) throws IOException {
            value.close();
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
            implements HttpConverter<InputStream, InputStream> {
        static final StreamingResponseBodyConverter INSTANCE = new StreamingResponseBodyConverter();

        @Override public InputStream convert(InputStream value) throws IOException {
            return value;
        }
    }

    static final class BufferingResponseBodyConverter
            implements HttpConverter<InputStream, InputStream> {
        static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

        @Override public InputStream convert(InputStream value) throws IOException {
            try {
                // Buffer the entire body to avoid future I/O.
                return null;
            } finally {
                // FIXME: 2016/12/17
//                value.close();
            }
        }
    }

    public static final class ToStringConverter implements HttpConverter<Object, String> {
        public static final ToStringConverter INSTANCE = new ToStringConverter();

        @Override public String convert(Object value) {
            return value.toString();
        }
    }
}

