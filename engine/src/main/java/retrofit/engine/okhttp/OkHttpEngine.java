package retrofit.engine.okhttp;

import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import retrofit.Retrofit;
import retrofit.ServiceMethod;
import retrofit.core.HttpCall;
import retrofit.core.HttpConverter;
import retrofit.core.ParameterHandler;
import retrofit.engine.HttpCallback;
import retrofit.engine.HttpEngine;
import retrofit.http.*;
import retrofit.http.bean.HttpHeaders;
import retrofit.http.bean.HttpResponse;
import retrofit.util.Utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import static retrofit.ServiceMethod.boxIfPrimitive;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public class OkHttpEngine extends HttpEngine {
    OkHttpClient okHttpClient;

    public OkHttpEngine() {
        okHttpClient = new OkHttpClient.Builder()
                .build();
    }

    @Override
    public HttpCall newHttpCall(ServiceMethod serviceMethod, Object[] args) {
        HttpCall okHttpCall = new OkHttpCall<>(serviceMethod, args);
        okHttpCall.setHttpEngine(this);
        return okHttpCall;
    }

    @Override
    public <T> void cancel(HttpCall httpCall) {

    }

    @Override
    public <T> HttpResponse execute(HttpCall<T> httpCall) throws IOException {
        okhttp3.Call call;
        synchronized (this) {
            OkHttpCall<T> okHttpCall = (OkHttpCall<T>) httpCall;
            httpCall.check();

            call = okHttpCall.rawCall;
            if (call == null) {
                try {
                    call = okHttpCall.rawCall = createRawCall(okHttpCall);
                } catch (IOException | RuntimeException e) {
                    httpCall.creationFailure = e;
                    throw e;
                }
            }
        }

        if (httpCall.isCanceled()) {
            call.cancel();
        }

        return parseResponse(httpCall, call.execute());
    }

    private Call createRawCall(OkHttpCall okHttpCall) throws IOException {
        okhttp3.Request request = toRequest(okHttpCall.serviceMethod, okHttpCall.args);
        okhttp3.Call call = okHttpClient.newCall(request);
        return call;
    }

    private Request toRequest(ServiceMethod serviceMethod, Object[] args) throws IOException {
        retrofit.RequestBuilder builder = new OkHttpRequestBuilder(serviceMethod.httpMethod,
                serviceMethod.baseUrl, serviceMethod.relativeUrl, serviceMethod.headers,
                serviceMethod.contentType, serviceMethod.hasBody, serviceMethod.isFormEncoded,
                serviceMethod.isMultipart);
        ParameterHandler[] handlers = serviceMethod.parameterHandlers;
        int argumentCount = args != null ? args.length : 0;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException("Argument count (" + argumentCount
                    + ") doesn't match expected count (" + handlers.length + ")");
        }

        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(builder, args[p]);
        }
        return (Request) builder.build();
    }

    private <T> HttpResponse<T, okhttp3.Response, okhttp3.ResponseBody> parseResponse(
            HttpCall<T> httpCall, Response rawResponse) throws IOException {

        ResponseBody rawBody = rawResponse.body();

        // Remove the body's source (the only stateful object) so we can pass the response along.
        rawResponse = rawResponse.newBuilder()
                .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
                .build();

        int code = rawResponse.code();
        if (code < 200 || code >= 300) {
            try {
                // Buffer the entire body to avoid future I/O.
                ResponseBody bufferedBody = buffer(rawBody);
                return error(bufferedBody, rawResponse);
            } finally {
                rawBody.close();
            }
        }

        if (code == 204 || code == 205) {
            return success(null, rawResponse);
        }

        ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(rawBody);
        try {
            T body = httpCall.serviceMethod.responseConverter.convert(catchingBody.byteStream());// httpCall.serviceMethod.toResponse(catchingBody);
            return success(body, rawResponse);
        } catch (RuntimeException e) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            catchingBody.throwIfCaught();
            throw e;
        }
    }

    private <T> HttpResponse<T, Response, ResponseBody> success(T t, Response rawResponse) {
        HttpHeaders httpHeaders = OkHttpUtils.toHttpHeaders(rawResponse.headers());
        HttpResponse<T, Response, ResponseBody> httpResponse = new HttpResponse<>(
                rawResponse, null, rawResponse.code(), httpHeaders, t, rawResponse.message());
        return httpResponse;
    }

    private <T> HttpResponse<T, Response, ResponseBody> error(ResponseBody bufferedBody, Response rawResponse) {
        HttpHeaders httpHeaders = OkHttpUtils.toHttpHeaders(rawResponse.headers());
        HttpResponse<T, Response, ResponseBody> httpResponse = new HttpResponse<>(
                rawResponse, rawResponse.body(), rawResponse.code(), httpHeaders, null, rawResponse.message());
        return httpResponse;
    }

    ResponseBody buffer(final ResponseBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.source().readAll(buffer);
        return ResponseBody.create(body.contentType(), body.contentLength(), buffer);
    }

    @Override
    public <T> void execute(HttpCall<T> c, final HttpCallback<T> callback) {
        if (callback == null) throw new NullPointerException("callback == null");

        Call call;
        final OkHttpCall<T> okHttpCall = (OkHttpCall) c;
        Throwable failure;

        synchronized (this) {
            if (okHttpCall.executed) throw new IllegalStateException("Already executed.");
            okHttpCall.executed = true;

            call = okHttpCall.rawCall;
            failure = okHttpCall.creationFailure;
            if (call == null && failure == null) {
                try {
                    call = okHttpCall.rawCall = createRawCall(okHttpCall);
                } catch (Throwable t) {
                    failure = okHttpCall.creationFailure = t;
                }
            }
        }

        if (failure != null) {
            callback.onFailure(okHttpCall, failure);
            return;
        }

        if (okHttpCall.isCanceled()) {
            call.cancel();
        }

        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse)
                    throws IOException {
                HttpResponse<T, ?, ?> response;
                try {
                    response = parseResponse(okHttpCall, rawResponse);
                } catch (Throwable e) {
                    callFailure(e);
                    return;
                }
                callSuccess(response);
            }

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                try {
                    callback.onFailure(okHttpCall, e);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void callFailure(Throwable e) {
                try {
                    callback.onFailure(okHttpCall, e);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void callSuccess(HttpResponse<T, ?, ?> response) {
                try {
                    callback.onResponse(okHttpCall, response);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    @Override
    public ParameterHandler<?> getParameterHandler(Retrofit retrofit, Type type, Annotation[] annotations,
                                                   Annotation annotation) {
        if (annotation instanceof Url) {
            return new OKHttpParameterHandler.RelativeUrl();
        } else if (annotation instanceof Path) {
            Path path = (Path) annotation;
            return new OKHttpParameterHandler.Path<>(path.value(), retrofit.stringConverter(type, annotations), path.encoded());
        } else if (annotation instanceof Query) {

            Query query = (Query) annotation;
            String name = query.value();
            boolean encoded = query.encoded();

            Class<?> rawParameterType = Utils.getRawType(type);
            if (Iterable.class.isAssignableFrom(rawParameterType)) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(iterableType, annotations);
                return new OKHttpParameterHandler.Query<>(name, converter, encoded).iterable();
            } else if (rawParameterType.isArray()) {
                Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(arrayComponentType, annotations);
                return new OKHttpParameterHandler.Query<>(name, converter, encoded).array();
            } else {
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(type, annotations);
                return new OKHttpParameterHandler.Query<>(name, converter, encoded);
            }
        } else if (annotation instanceof QueryMap) {
            Class<?> rawParameterType = Utils.getRawType(type);
            Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
            ParameterizedType parameterizedType = (ParameterizedType) mapType;
            Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
            HttpConverter<?, String> valueConverter =
                    retrofit.stringConverter(valueType, annotations);
            return new OKHttpParameterHandler.QueryMap<>(valueConverter, ((QueryMap) annotation).encoded());
        } else if (annotation instanceof Header) {
            Header header = (Header) annotation;
            String name = header.value();

            Class<?> rawParameterType = Utils.getRawType(type);
            if (Iterable.class.isAssignableFrom(rawParameterType)) {

                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(iterableType, annotations);
                return new OKHttpParameterHandler.Header<>(name, converter).iterable();
            } else if (rawParameterType.isArray()) {
                Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(arrayComponentType, annotations);
                return new OKHttpParameterHandler.Header<>(name, converter).array();
            } else {
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(type, annotations);
                return new OKHttpParameterHandler.Header<>(name, converter);
            }

        } else if (annotation instanceof HeaderMap) {
            Class<?> rawParameterType = Utils.getRawType(type);
            Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
            ParameterizedType parameterizedType = (ParameterizedType) mapType;
            Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
            HttpConverter<?, String> valueConverter =
                    retrofit.stringConverter(valueType, annotations);
            return new OKHttpParameterHandler.HeaderMap<>(valueConverter);

        } else if (annotation instanceof Field) {
//            if (!isFormEncoded) {
//                throw parameterError(p, "@Field parameters can only be used with form encoding.");
//            }
            Field field = (Field) annotation;
            String name = field.value();
            boolean encoded = field.encoded();

//            gotField = true;

            Class<?> rawParameterType = Utils.getRawType(type);
            if (Iterable.class.isAssignableFrom(rawParameterType)) {
//                if (!(type instanceof ParameterizedType)) {
//                    throw parameterError(p, rawParameterType.getSimpleName()
//                            + " must include generic type (e.g., "
//                            + rawParameterType.getSimpleName()
//                            + "<String>)");
//                }
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(iterableType, annotations);
                return new OKHttpParameterHandler.Field<>(name, converter, encoded).iterable();
            } else if (rawParameterType.isArray()) {
                Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(arrayComponentType, annotations);
                return new OKHttpParameterHandler.Field<>(name, converter, encoded).array();
            } else {
                HttpConverter<?, String> converter =
                        retrofit.stringConverter(type, annotations);
                return new OKHttpParameterHandler.Field<>(name, converter, encoded);
            }

        } else if (annotation instanceof FieldMap) {
//            if (!isFormEncoded) {
//                throw parameterError(p, "@FieldMap parameters can only be used with form encoding.");
//            }
            Class<?> rawParameterType = Utils.getRawType(type);
            Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
//            if (!(mapType instanceof ParameterizedType)) {
//                throw parameterError(p,
//                        "Map must include generic types (e.g., Map<String, String>)");
//            }
            ParameterizedType parameterizedType = (ParameterizedType) mapType;
//            Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
//            if (String.class != keyType) {
//                throw parameterError(p, "@FieldMap keys must be of type String: " + keyType);
//            }
            Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
            HttpConverter<?, String> valueConverter =
                    retrofit.stringConverter(valueType, annotations);

//            gotField = true;
            return new OKHttpParameterHandler.FieldMap<>(valueConverter, ((FieldMap) annotation).encoded());

        }

        return null;
    }

    @Override
    public ParameterHandler<?> getParameterHandler(Retrofit retrofit, Type type, Annotation[] annotations, Annotation[] methodAnnotations, Annotation annotation) {
        if (annotation instanceof Part) {
//            if (!isMultipart) {
//                throw parameterError(p, "@Part parameters can only be used with multipart encoding.");
//            }
            Part part = (Part) annotation;
//            gotPart = true;

            String partName = part.value();
            Class<?> rawParameterType = Utils.getRawType(type);
            if (partName.isEmpty()) {
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw new IllegalArgumentException(rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    if (!MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                        throw new IllegalArgumentException(
                                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                    }
                    return OKHttpParameterHandler.RawPart.INSTANCE.iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = rawParameterType.getComponentType();
                    if (!MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                        throw new IllegalArgumentException(
                                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                    }
                    return OKHttpParameterHandler.RawPart.INSTANCE.array();
                } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                    return OKHttpParameterHandler.RawPart.INSTANCE;
                } else {
                    throw new IllegalArgumentException(
                            "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                }
            } else {
                HttpHeaders headers =
                        HttpHeaders.of("Content-Disposition", "form-data; name=\"" + partName + "\"",
                                "Content-Transfer-Encoding", part.encoding());

                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw new IllegalArgumentException(rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                        throw new IllegalArgumentException("@Part parameters using the MultipartBody.Part must not "
                                + "include a part name in the annotation.");
                    }
                    HttpConverter<?, retrofit.RequestBody> converter =
                            retrofit.requestBodyConverter(iterableType, annotations, methodAnnotations);
                    return new OKHttpParameterHandler.Part<>(headers, converter).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    if (MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                        throw new IllegalArgumentException("@Part parameters using the MultipartBody.Part must not "
                                + "include a part name in the annotation.");
                    }
                    HttpConverter<?, retrofit.RequestBody> converter =
                            retrofit.requestBodyConverter(arrayComponentType, annotations, methodAnnotations);
                    return new OKHttpParameterHandler.Part<>(headers, converter).array();
                } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                    throw new IllegalArgumentException("@Part parameters using the MultipartBody.Part must not "
                            + "include a part name in the annotation.");
                } else {
                    HttpConverter<?, retrofit.RequestBody> converter =
                            retrofit.requestBodyConverter(type, annotations, methodAnnotations);
                    return new OKHttpParameterHandler.Part<>(headers, converter);
                }
            }
        } else if (annotation instanceof PartMap) {
//            if (!isMultipart) {
//                throw parameterError(p, "@PartMap parameters can only be used with multipart encoding.");
//            }
//            gotPart = true;
            Class<?> rawParameterType = Utils.getRawType(type);
            if (!Map.class.isAssignableFrom(rawParameterType)) {
                throw new IllegalArgumentException("@PartMap parameter type must be Map.");
            }
            Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
            if (!(mapType instanceof ParameterizedType)) {
                throw new IllegalArgumentException("Map must include generic types (e.g., Map<String, String>)");
            }
            ParameterizedType parameterizedType = (ParameterizedType) mapType;

            Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
            if (String.class != keyType) {
                throw new IllegalArgumentException("@PartMap keys must be of type String: " + keyType);
            }

            Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
            if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
                throw new IllegalArgumentException("@PartMap values cannot be MultipartBody.Part. "
                        + "Use @Part List<Part> or a different value type instead.");
            }

            HttpConverter<?, retrofit.RequestBody> valueConverter =
                    retrofit.requestBodyConverter(valueType, annotations, methodAnnotations);

            PartMap partMap = (PartMap) annotation;
            return new OKHttpParameterHandler.PartMap<>(valueConverter, partMap.encoding());

        } else if (annotation instanceof Body) {
//            if (isFormEncoded || isMultipart) {
//                throw parameterError(p,
//                        "@Body parameters cannot be used with form or multi-part encoding.");
//            }
//            if (gotBody) {
//                throw parameterError(p, "Multiple @Body method annotations found.");
//            }

            HttpConverter<?, retrofit.RequestBody> converter;
            try {
                converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
            } catch (RuntimeException e) {
                // Wide exception range because factories are user code.
                throw new IllegalStateException(Utils.format("Unable to create @Body converter for %s", type));
            }
//            gotBody = true;
            return new OKHttpParameterHandler.Body<>(converter);
        }
        return null;
    }

    static final class NoContentResponseBody extends ResponseBody {
        private final MediaType contentType;
        private final long contentLength;

        NoContentResponseBody(MediaType contentType, long contentLength) {
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public BufferedSource source() {
            throw new IllegalStateException("Cannot read raw response body of a converted body.");
        }
    }

    static final class ExceptionCatchingRequestBody extends ResponseBody {
        private final ResponseBody delegate;
        IOException thrownException;

        ExceptionCatchingRequestBody(ResponseBody delegate) {
            this.delegate = delegate;
        }

        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() {
            return delegate.contentLength();
        }

        @Override
        public BufferedSource source() {
            return Okio.buffer(new ForwardingSource(delegate.source()) {
                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    try {
                        return super.read(sink, byteCount);
                    } catch (IOException e) {
                        thrownException = e;
                        throw e;
                    }
                }
            });
        }

        @Override
        public void close() {
            delegate.close();
        }

        void throwIfCaught() throws IOException {
            if (thrownException != null) {
                throw thrownException;
            }
        }
    }
}
