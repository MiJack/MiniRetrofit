package retrofit.engine;

import retrofit.HttpCallback;
import retrofit.Retrofit;
import retrofit.ServiceMethod;
import retrofit.core.HttpCall;
import retrofit.core.ParameterHandler;
import retrofit.http.bean.HttpResponse;

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

    public abstract ParameterHandler<?> getParameterHandler(Retrofit retrofit, Type type, Annotation[] annotations,
                                                            Annotation annotation);

    public ParameterHandler<?> getParameterHandler(Retrofit retrofit, Annotation annotation) {
        return parameterHandlerMap.get(annotation.annotationType());
    }

    public abstract ParameterHandler<?> getParameterHandler(Retrofit retrofit, Type type, Annotation[] annotations, Annotation[] methodAnnotations, Annotation annotation);
}
