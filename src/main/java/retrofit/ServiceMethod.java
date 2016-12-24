package retrofit;

import retrofit.core.HttpCallAdapter;
import retrofit.core.HttpConverter;
import retrofit.core.MethodAnnotationHandler;
import retrofit.core.ParameterHandler;
import retrofit.core.HttpEngine;
import retrofit.http.*;
import retrofit.util.Utils;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Mr.Yuan
 * @since 2016/11/28.
 */
public class ServiceMethod<T,R> {
    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

    final HttpCallAdapter<T,R> callAdapter;

    public final HttpUrl baseUrl;
    public final HttpConverter<InputStream, T> responseConverter;
    public final String httpMethod;
    public final String relativeUrl;
    public final HttpHeaders headers;
    public final MediaType contentType;
    public final boolean hasBody;
    public final boolean isFormEncoded;
    public final boolean isMultipart;
    public final ParameterHandler<?>[] parameterHandlers;
    public final HttpEngine httpEngine;

    ServiceMethod(Builder<T,R> builder) {
        this.callAdapter = builder.callAdapter;
        this.baseUrl = builder.retrofit.baseUrl();
        this.responseConverter = builder.responseConverter;
        this.httpMethod = builder.httpMethod;
        this.relativeUrl = builder.relativeUrl;
        this.headers = builder.headers;
        this.contentType = builder.contentType;
        this.hasBody = builder.hasBody;
        this.isFormEncoded = builder.isFormEncoded;
        this.isMultipart = builder.isMultipart;
        this.parameterHandlers = builder.parameterHandlers;
        this.httpEngine = builder.retrofit.httpEngine;
    }

    public static final class Builder<T,R> {
        final Retrofit retrofit;
        final Method method;
        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;
        public Type responseType;
        public boolean gotField;
        public boolean gotPart;
        public boolean gotBody;
        public boolean gotPath;
        public boolean gotQuery;
        public boolean gotUrl;
        public String httpMethod;
        public boolean hasBody;
        public boolean isFormEncoded;
        public boolean isMultipart;
        public String relativeUrl;
        public HttpHeaders headers;
        public MediaType contentType;
        public Set<String> relativeUrlParamNames;
        public ParameterHandler<?>[] parameterHandlers;
        public HttpConverter<InputStream, T> responseConverter;
        public HttpCallAdapter<T,R> callAdapter;


        Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
        }

        public ServiceMethod<T,R> build() {
            callAdapter = createCallAdapter();
            responseType = callAdapter.responseType();
            responseConverter = createResponseConverter();
            //处理methodAnnotations
            for (Annotation annotation : methodAnnotations) {
                MethodAnnotationHandler methodAnnotationHandler = retrofit.getMethodAnnotationHandler(annotation);
                if (methodAnnotationHandler == null) {
                    System.err.println("missing annotation handler for annotation " + annotation);
                    continue;
                }
                methodAnnotationHandler.apply(annotation, this);
            }

            if (httpMethod == null) {
                throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
            }

            if (!hasBody) {
                if (isMultipart) {
                    throw methodError(
                            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
                }
                if (isFormEncoded) {
                    throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
                            + "request body (e.g., @POST).");
                }
            }

            //针对parameter Annotations进行校验
            int parameterCount = parameterAnnotationsArray.length;
            parameterHandlers = new ParameterHandler<?>[parameterCount];
            for (int p = 0; p < parameterCount; p++) {
                Type parameterType = parameterTypes[p];
                if (Utils.hasUnresolvableType(parameterType)) {
                    throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
                            parameterType);
                }

                Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
                if (parameterAnnotations == null) {
                    throw parameterError(p, "No Retrofit annotation found.");
                }

                parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
            }
            //进行校验
            if (relativeUrl == null && !gotUrl) {
                throw methodError("Missing either @%s URL or @Url parameter.", httpMethod);
            }
            if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
                throw methodError("Non-body HTTP method cannot contain @Body.");
            }
            if (isFormEncoded && !gotField) {
                throw methodError("Form-encoded method must contain at least one @Field.");
            }
            if (isMultipart && !gotPart) {
                throw methodError("Multipart method must contain at least one @Part.");
            }
            return new ServiceMethod<>(this);
        }

        private ParameterHandler<?> parseParameter(int p, Type parameterType, Annotation[] annotations) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotations) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(
                        p, parameterType, annotations, annotation);

                if (annotationAction == null) {
                    annotationAction = retrofit.httpEngine.getParameterHandler(retrofit, annotation);
                }
                if (annotationAction == null) {
                    continue;
                }

                if (result != null) {
                    throw parameterError(p, "Multiple Retrofit annotations found, only one allowed.");
                }

                result = annotationAction;
            }

            if (result == null) {
                throw parameterError(p, "No Retrofit annotation found.");
            }

            return result;
        }

        private ParameterHandler<?> parseParameterAnnotation(int p, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Url) {
                if (gotUrl) {
                    throw parameterError(p, "Multiple @Url method annotations found.");
                }
                if (gotPath) {
                    throw parameterError(p, "@Path parameters may not be used with @Url.");
                }
                if (gotQuery) {
                    throw parameterError(p, "A @Url parameter must not come after a @Query");
                }
                if (relativeUrl != null) {
                    throw parameterError(p, "@Url cannot be used with @%s URL", httpMethod);
                }

                gotUrl = true;

                if (type == HttpUrl.class
                        || type == String.class
                        || type == URI.class
                        || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
                    return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);
                } else {
                    throw parameterError(p,
                            "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
                }

            } else if (annotation instanceof Path) {
                if (gotQuery) {
                    throw parameterError(p, "A @Path parameter must not come after a @Query.");
                }
                if (gotUrl) {
                    throw parameterError(p, "@Path parameters may not be used with @Url.");
                }
                if (relativeUrl == null) {
                    throw parameterError(p, "@Path can only be used with relative url on @%s", httpMethod);
                }
                gotPath = true;

                Path path = (Path) annotation;
                String name = path.value();
                validatePathName(p, name);

//                HttpConverter<?, String> converter = retrofit.stringConverter(type, annotations);
//                return new ParameterHandler.Path<>(name, converter, path.encoded());
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);

            } else if (annotation instanceof Query) {
                gotQuery = true;
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);
//                Query query = (Query) annotation;
//                String name = query.value();
//                boolean encoded = query.encoded();
//
//                Class<?> rawParameterType = Utils.getRawType(type);
//                if (Iterable.class.isAssignableFrom(rawParameterType)) {
//                    if (!(type instanceof ParameterizedType)) {
//                        throw parameterError(p, rawParameterType.getSimpleName()
//                                + " must include generic type (e.g., "
//                                + rawParameterType.getSimpleName()
//                                + "<String>)");
//                    }
//                    ParameterizedType parameterizedType = (ParameterizedType) type;
//                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
//                    HttpConverter<?, String> converter =
//                            retrofit.stringConverter(iterableType, annotations);
//                    return new ParameterHandler.Query<>(name, converter, encoded).iterable();
//                    return retrofit.httpEngine.getParameterHandler(retrofit,type,annotations,annotation);
//                } else if (rawParameterType.isArray()) {
//                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
//                    HttpConverter<?, String> converter =
//                            retrofit.stringConverter(arrayComponentType, annotations);
//                    return new ParameterHandler.Query<>(name, converter, encoded).array();
//                } else {
//                    HttpConverter<?, String> converter =
//                            retrofit.stringConverter(type, annotations);
//                    return new ParameterHandler.Query<>(name, converter, encoded);
//                }

            } else if (annotation instanceof QueryMap) {
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(p, "@QueryMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(p, "@QueryMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                HttpConverter<?, String> valueConverter =
                        retrofit.stringConverter(valueType, annotations);
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);
//                return new ParameterHandler.QueryMap<>(valueConverter, ((QueryMap) annotation).encoded());

            } else if (annotation instanceof Header) {
                Header header = (Header) annotation;
                String name = header.value();

                Class<?> rawParameterType = Utils.getRawType(type);
                return getParameterHandler(p, type, annotations, annotation, rawParameterType);

            } else if (annotation instanceof HeaderMap) {
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(p, "@HeaderMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(p, "@HeaderMap keys must be of type String: " + keyType);
                }
//                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
//                HttpConverter<?, String> valueConverter =
//                        retrofit.stringConverter(valueType, annotations);

//                return new ParameterHandler.HeaderMap<>(valueConverter);
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);

            } else if (annotation instanceof Field) {
                if (!isFormEncoded) {
                    throw parameterError(p, "@Field parameters can only be used with form encoding.");
                }
                Field field = (Field) annotation;
                String name = field.value();
                boolean encoded = field.encoded();

                gotField = true;

                Class<?> rawParameterType = Utils.getRawType(type);
                return getParameterHandler(p, type, annotations, annotation, rawParameterType);

            } else if (annotation instanceof FieldMap) {
                if (!isFormEncoded) {
                    throw parameterError(p, "@FieldMap parameters can only be used with form encoding.");
                }
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(p, "@FieldMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(p,
                            "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(p, "@FieldMap keys must be of type String: " + keyType);
                }
//                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
//                HttpConverter<?, String> valueConverter =
//                        retrofit.stringConverter(valueType, annotations);

                gotField = true;
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);

//                return new ParameterHandler.FieldMap<>(valueConverter, ((FieldMap) annotation).encoded());

            } else if (annotation instanceof Part) {
                if (!isMultipart) {
                    throw parameterError(p, "@Part parameters can only be used with multipart encoding.");
                }
                Part part = (Part) annotation;
                gotPart = true;
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, methodAnnotations, annotation);
//                String partName = part.value();
//                Class<?> rawParameterType = Utils.getRawType(type);
//                if (partName.isEmpty()) {
//                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
//                        if (!(type instanceof ParameterizedType)) {
//                            throw parameterError(p, rawParameterType.getSimpleName()
//                                    + " must include generic type (e.g., "
//                                    + rawParameterType.getSimpleName()
//                                    + "<String>)");
//                        }
//                        ParameterizedType parameterizedType = (ParameterizedType) type;
//                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
//                        if (!MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
//                            throw parameterError(p,
//                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
//                        }
//                        return ParameterHandler.RawPart.INSTANCE.iterable();
//                    } else if (rawParameterType.isArray()) {
//                        Class<?> arrayComponentType = rawParameterType.getComponentType();
//                        if (!MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
//                            throw parameterError(p,
//                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
//                        }
//                        return ParameterHandler.RawPart.INSTANCE.array();
//                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
//                        return ParameterHandler.RawPart.INSTANCE;
//                    } else {
//                        throw parameterError(p,
//                                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
//                    }
//                } else {
//                    HttpHeaders headers =
//                            HttpHeaders.of("Content-Disposition", "form-data; name=\"" + partName + "\"",
//                                    "Content-Transfer-Encoding", part.encoding());
//
//                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
//                        if (!(type instanceof ParameterizedType)) {
//                            throw parameterError(p, rawParameterType.getSimpleName()
//                                    + " must include generic type (e.g., "
//                                    + rawParameterType.getSimpleName()
//                                    + "<String>)");
//                        }
//                        ParameterizedType parameterizedType = (ParameterizedType) type;
//                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
//                        if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
//                            throw parameterError(p, "@Part parameters using the MultipartBody.Part must not "
//                                    + "include a part name in the annotation.");
//                        }
//                        HttpConverter<?, RequestBody> converter =
//                                retrofit.requestBodyConverter(iterableType, annotations, methodAnnotations);
//                        return new ParameterHandler.Part<>(headers, converter).iterable();
//                    } else if (rawParameterType.isArray()) {
//                        Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
//                        if (MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
//                            throw parameterError(p, "@Part parameters using the MultipartBody.Part must not "
//                                    + "include a part name in the annotation.");
//                        }
//                        HttpConverter<?, RequestBody> converter =
//                                retrofit.requestBodyConverter(arrayComponentType, annotations, methodAnnotations);
//                        return new ParameterHandler.Part<>(headers, converter).array();
//                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
//                        throw parameterError(p, "@Part parameters using the MultipartBody.Part must not "
//                                + "include a part name in the annotation.");
//                    } else {
//                        HttpConverter<?, RequestBody> converter =
//                                retrofit.requestBodyConverter(type, annotations, methodAnnotations);
//                        return new ParameterHandler.Part<>(headers, converter);
//                    }
//                }

            } else if (annotation instanceof PartMap) {
                if (!isMultipart) {
                    throw parameterError(p, "@PartMap parameters can only be used with multipart encoding.");
                }
                gotPart = true;
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, methodAnnotations, annotation);
//                Class<?> rawParameterType = Utils.getRawType(type);
//                if (!Map.class.isAssignableFrom(rawParameterType)) {
//                    throw parameterError(p, "@PartMap parameter type must be Map.");
//                }
//                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
//                if (!(mapType instanceof ParameterizedType)) {
//                    throw parameterError(p, "Map must include generic types (e.g., Map<String, String>)");
//                }
//                ParameterizedType parameterizedType = (ParameterizedType) mapType;
//
//                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
//                if (String.class != keyType) {
//                    throw parameterError(p, "@PartMap keys must be of type String: " + keyType);
//                }
//
//                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
//                if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
//                    throw parameterError(p, "@PartMap values cannot be MultipartBody.Part. "
//                            + "Use @Part List<Part> or a different value type instead.");
//                }
//
//                HttpConverter<?, RequestBody> valueConverter =
//                        retrofit.requestBodyConverter(valueType, annotations, methodAnnotations);
//
//                PartMap partMap = (PartMap) annotation;
//                return new ParameterHandler.PartMap<>(valueConverter, partMap.encoding());
            } else if (annotation instanceof Body) {
                if (isFormEncoded || isMultipart) {
                    throw parameterError(p,
                            "@Body parameters cannot be used with form or multi-part encoding.");
                }
                if (gotBody) {
                    throw parameterError(p, "Multiple @Body method annotations found.");
                }

//                HttpConverter<?, RequestBody> converter;
//                try {
//                    converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
//                } catch (RuntimeException e) {
//                    // Wide exception range because factories are user code.
//                    throw parameterError(e, p, "Unable to create @Body converter for %s", type);
//                }
                ParameterHandler<?> parameterHandler = retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, methodAnnotations, annotation);
                gotBody = true;
                return parameterHandler;
//                return new ParameterHandler.Body<>(converter);
            }

            return null; // Not a Retrofit annotation
        }

        private ParameterHandler<?> getParameterHandler(int p, Type type, Annotation[] annotations, Annotation annotation, Class<?> rawParameterType) {
            if (Iterable.class.isAssignableFrom(rawParameterType)) {
                if (!(type instanceof ParameterizedType)) {
                    throw parameterError(p, rawParameterType.getSimpleName()
                            + " must include generic type (e.g., "
                            + rawParameterType.getSimpleName()
                            + "<String>)");
                }
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);

//                    ParameterizedType parameterizedType = (ParameterizedType) type;
//                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
//                    HttpConverter<?, String> converter =
//                            retrofit.stringConverter(iterableType, annotations);
//                    return new ParameterHandler.Field<>(name, converter, encoded).iterable();
            } else if (rawParameterType.isArray()) {
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);
//                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
//                    HttpConverter<?, String> converter =
//                            retrofit.stringConverter(arrayComponentType, annotations);
//                    return new ParameterHandler.Field<>(name, converter, encoded).array();
            } else {
                return retrofit.httpEngine.getParameterHandler(retrofit, type, annotations, annotation);
//                    HttpConverter<?, String> converter =
//                            retrofit.stringConverter(type, annotations);
//                    return new ParameterHandler.Field<>(name, converter, encoded);
            }
        }

        private HttpConverter<InputStream, T> createResponseConverter() {
            Annotation[] annotations = method.getAnnotations();
            try {
                return retrofit.responseBodyConverter(responseType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create converter for %s", responseType);
            }
        }

        private HttpCallAdapter<T,R> createCallAdapter() {
            Type returnType = method.getGenericReturnType();
            if (Utils.hasUnresolvableType(returnType)) {
                throw methodError(
                        "Method return type must not include a type variable or wildcard: %s", returnType);
            }
            if (returnType == void.class) {
                throw methodError("Service methods cannot return void.");
            }
            Annotation[] annotations = method.getAnnotations();
            try {
                //noinspection unchecked
                return (HttpCallAdapter<T,R>) retrofit.callAdapter(returnType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create call adapter for %s", returnType);
            }
        }

        private RuntimeException methodError(String message, Object... args) {
            return methodError(null, message, args);
        }

        private RuntimeException methodError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + method.getDeclaringClass().getSimpleName()
                    + "."
                    + method.getName(), cause);
        }

        private RuntimeException parameterError(
                Throwable cause, int p, String message, Object... args) {
            return methodError(cause, message + " (parameter #" + (p + 1) + ")", args);
        }

        private RuntimeException parameterError(int p, String message, Object... args) {
            return methodError(message + " (parameter #" + (p + 1) + ")", args);
        }

        private void validatePathName(int p, String name) {
            if (!PARAM_NAME_REGEX.matcher(name).matches()) {
                throw parameterError(p, "@Path parameter name must match %s. Found: %s",
                        PARAM_URL_REGEX.pattern(), name);
            }
            // Verify URL replacement name is actually present in the URL path.
            if (!relativeUrlParamNames.contains(name)) {
                throw parameterError(p, "URL \"%s\" does not contain \"{%s}\".", relativeUrl, name);
            }
        }


    }

    public static Class<?> boxIfPrimitive(Class<?> type) {
        if (boolean.class == type) return Boolean.class;
        if (byte.class == type) return Byte.class;
        if (char.class == type) return Character.class;
        if (double.class == type) return Double.class;
        if (float.class == type) return Float.class;
        if (int.class == type) return Integer.class;
        if (long.class == type) return Long.class;
        if (short.class == type) return Short.class;
        return type;
    }
}
