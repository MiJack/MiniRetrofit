package retrofit.engine.okhttp;

import okhttp3.MultipartBody;
import retrofit.RequestBody;
import retrofit.RequestBuilder;
import retrofit.core.HttpConverter;
import retrofit.core.ParameterHandler;
import retrofit.HttpHeaders;

import java.io.IOException;
import java.util.Map;

import static retrofit.util.Utils.checkNotNull;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public class OKHttpParameterHandler {
    public static final class RelativeUrl extends ParameterHandler<Object> {
        @Override
        public void apply(RequestBuilder builder, Object value) {
            builder.setRelativeUrl(value);
        }
    }

    public static final class Header<T> extends ParameterHandler<T> {
        private final String name;
        private final HttpConverter<T, String> valueConverter;

        public Header(String name, HttpConverter<T, String> valueConverter) {
            this.name = checkNotNull(name, "name == null");
            this.valueConverter = valueConverter;
        }

        @Override
        public void apply(RequestBuilder builder, T value) throws IOException {
            if (value == null) return; // Skip null values.
            builder.addHeader(name, valueConverter.convert(value));
        }
    }

    public static final class Path<T> extends ParameterHandler<T> {
        private final String name;
        private final HttpConverter<T, String> valueConverter;
        private final boolean encoded;

        public Path(String name, HttpConverter<T, String> valueConverter, boolean encoded) {
            this.name = checkNotNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        public void apply(RequestBuilder builder, T value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException(
                        "Path parameter \"" + name + "\" value must not be null.");
            }
            builder.addPathParam(name, valueConverter.convert(value), encoded);
        }
    }

    public static final class Query<T> extends ParameterHandler<T> {
        private final String name;
        private final HttpConverter<T, String> valueConverter;
        private final boolean encoded;

        public Query(String name, HttpConverter<T, String> valueConverter, boolean encoded) {
            this.name = checkNotNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        public void apply(RequestBuilder builder, T value) throws IOException {
            if (value == null) return; // Skip null values.
            builder.addQueryParam(name, valueConverter.convert(value), encoded);
        }
    }


    public static final class QueryMap<T> extends ParameterHandler<Map<String, T>> {
        private final HttpConverter<T, String> valueConverter;
        private final boolean encoded;

        public QueryMap(HttpConverter<T, String> valueConverter, boolean encoded) {
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException("Query map was null.");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey == null) {
                    throw new IllegalArgumentException("Query map contained null key.");
                }
                T entryValue = entry.getValue();
                if (entryValue == null) {
                    throw new IllegalArgumentException(
                            "Query map contained null value for key '" + entryKey + "'.");
                }
                builder.addQueryParam(entryKey, valueConverter.convert(entryValue), encoded);
            }
        }
    }


    public static final class HeaderMap<T> extends ParameterHandler<Map<String, T>> {
        private final HttpConverter<T, String> valueConverter;

        public HeaderMap(HttpConverter<T, String> valueConverter) {
            this.valueConverter = valueConverter;
        }

        @Override
        public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException("Header map was null.");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String headerName = entry.getKey();
                if (headerName == null) {
                    throw new IllegalArgumentException("Header map contained null key.");
                }
                T headerValue = entry.getValue();
                if (headerValue == null) {
                    throw new IllegalArgumentException(
                            "Header map contained null value for key '" + headerName + "'.");
                }
                builder.addHeader(headerName, valueConverter.convert(headerValue));
            }
        }
    }

    public
    static final class Field<T> extends ParameterHandler<T> {
        private final String name;
        private final HttpConverter<T, String> valueConverter;
        private final boolean encoded;

        public Field(String name, HttpConverter<T, String> valueConverter, boolean encoded) {
            this.name = checkNotNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        public void apply(RequestBuilder builder, T value) throws IOException {
            if (value == null) return; // Skip null values.
            builder.addFormField(name, valueConverter.convert(value), encoded);
        }
    }


    public static final class FieldMap<T> extends ParameterHandler<Map<String, T>> {
        private final HttpConverter<T, String> valueConverter;
        private final boolean encoded;

        public FieldMap(HttpConverter<T, String> valueConverter, boolean encoded) {
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException("Field map was null.");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey == null) {
                    throw new IllegalArgumentException("Field map contained null key.");
                }
                T entryValue = entry.getValue();
                if (entryValue == null) {
                    throw new IllegalArgumentException(
                            "Field map contained null value for key '" + entryKey + "'.");
                }
                builder.addFormField(entryKey, valueConverter.convert(entryValue), encoded);
            }
        }
    }


    public static final class Part<T> extends ParameterHandler<T> {
        private final HttpHeaders headers;
        private final HttpConverter<T, RequestBody> converter;

        public Part(HttpHeaders headers, HttpConverter<T, RequestBody> converter) {
            this.headers = headers;
            this.converter = converter;
        }

        @Override
        public void apply(RequestBuilder builder, T value) {
            if (value == null) return; // Skip null values.

            RequestBody body;
            try {
                body = converter.convert(value);
            } catch (IOException e) {
                throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
            }
            builder.addPart(headers, body);
        }
    }

    public static final class RawPart extends ParameterHandler<MultipartBody.Part> {
        public static final RawPart INSTANCE = new RawPart();

        private RawPart() {
        }

        @Override
        public void apply(RequestBuilder builder, MultipartBody.Part value) throws IOException {
            if (value != null && builder instanceof OkHttpRequestBuilder) { // Skip null values.
                ((OkHttpRequestBuilder)builder).addPart(value);
            }
        }
    }

    public static final class PartMap<T> extends ParameterHandler<Map<String, T>> {
        private final HttpConverter<T, RequestBody> valueConverter;
        private final String transferEncoding;

        public PartMap(HttpConverter<T, RequestBody> valueConverter, String transferEncoding) {
            this.valueConverter = valueConverter;
            this.transferEncoding = transferEncoding;
        }

        @Override
        public void apply(RequestBuilder builder, Map<String, T> value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException("Part map was null.");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey == null) {
                    throw new IllegalArgumentException("Part map contained null key.");
                }
                T entryValue = entry.getValue();
                if (entryValue == null) {
                    throw new IllegalArgumentException(
                            "Part map contained null value for key '" + entryKey + "'.");
                }

                HttpHeaders headers = HttpHeaders.of(
                        "Content-Disposition", "form-data; name=\"" + entryKey + "\"",
                        "Content-Transfer-Encoding", transferEncoding);

                builder.addPart(headers, valueConverter.convert(entryValue));
            }
        }
    }


    public static final class Body<T> extends ParameterHandler<T> {
        private final HttpConverter<T, RequestBody> converter;

        public Body(HttpConverter<T, RequestBody> converter) {
            this.converter = converter;
        }

        @Override
        public void apply(RequestBuilder builder, T value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException("Body parameter value must not be null.");
            }
            RequestBody body;
            try {
                body = converter.convert(value);
            } catch (IOException e) {
                throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
            }
            builder.setBody(body);
        }
    }
}
