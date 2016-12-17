package retrofit;

import retrofit.core.HttpCallAdapter;
import retrofit.core.HttpConverter;
import retrofit.http.bean.HttpHeaders;
import retrofit.http.bean.HttpUrl;
import retrofit.http.bean.MediaType;
import retrofit.util.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Mr.Yuan
 * @since 2016/11/28.
 */
public class ServiceMethod<R, T> {

    final HttpCallAdapter<R, T> callAdapter;

    private final HttpUrl baseUrl;
    private final HttpConverter<ResponseBody, R> responseConverter;
    private final String httpMethod;
    private final String relativeUrl;
    private final HttpHeaders headers;
    private final MediaType contentType;
    private final boolean hasBody;
    private final boolean isFormEncoded;
    private final boolean isMultipart;
    private final ParameterHandler<?>[] parameterHandlers;
    private final HttpEngine httpEngine;

    ServiceMethod(Builder<R, T> builder) {
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

    static final class Builder<T, R> {
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
        public HttpConverter<ResponseBody, T> responseConverter;
        public HttpCallAdapter<T, R> callAdapter;


        Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
        }

        public ServiceMethod<?, ?> build() {
            callAdapter = createCallAdapter();
            responseType = callAdapter.responseType();
            responseConverter = createResponseConverter();
            //处理methodAnnotations
            for (Annotation annotation : methodAnnotations) {
                MethodAnnotationHandler methodAnnotationHandler = retrofit.getAnnotationHandler(annotation);
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

        private HttpConverter<ResponseBody, T> createResponseConverter() {
            Annotation[] annotations = method.getAnnotations();
            try {
                return retrofit.responseBodyConverter(responseType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create converter for %s", responseType);
            }
        }

        private HttpCallAdapter<T, R> createCallAdapter() {
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
                return (HttpCallAdapter<T, R>) retrofit.callAdapter(returnType, annotations);
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

    }
}
