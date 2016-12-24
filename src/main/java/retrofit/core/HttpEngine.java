package retrofit.core;

import retrofit.HttpResponse;
import retrofit.RequestBuilder;
import retrofit.Retrofit;
import retrofit.ServiceMethod;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public abstract class HttpEngine {
    Map<Class, ParameterHandler> parameterHandlerMap;

    public HttpEngine() {
        parameterHandlerMap = new HashMap<>();
    }

    public abstract HttpCall newHttpCall(ServiceMethod serviceMethod, Object[] args);

    public abstract <T> void cancel(HttpCall httpCall);

    public abstract <T> HttpResponse execute(HttpCall<T> tHttpCall) throws IOException;

    public abstract <T> void execute(HttpCall<T> tHttpCall, HttpCallback<T> callback);

    public abstract ParameterHandler<?> getParameterHandler(
            Retrofit retrofit, Type type,
            Annotation[] annotations, Annotation annotation);

    public ParameterHandler<?> getParameterHandler(Retrofit retrofit, Annotation annotation) {
        return parameterHandlerMap.get(annotation.annotationType());
    }

    public abstract ParameterHandler<?> getParameterHandler(
            Retrofit retrofit, Type type, Annotation[] annotations,
            Annotation[] methodAnnotations, Annotation annotation);
    public Object toRequest(ServiceMethod serviceMethod, Object[] args) throws IOException {
        RequestBuilder builder = newRequestBuilder(serviceMethod);
        ParameterHandler[] handlers = serviceMethod.parameterHandlers;
        int argumentCount = args != null ? args.length : 0;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException("Argument count (" + argumentCount
                    + ") doesn't match expected count (" + handlers.length + ")");
        }

        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(builder, args[p]);
        }
        return  builder.build();
    }

    protected abstract RequestBuilder newRequestBuilder(ServiceMethod  serviceMethod);

}
