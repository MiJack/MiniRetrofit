package retrofit;

import retrofit.core.*;
import retrofit.core.HttpEngine;
import retrofit.http.FormUrlEncoded;
import retrofit.http.Headers;
import retrofit.http.HttpMethod;
import retrofit.http.Multipart;
import retrofit.util.Utils;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static retrofit.util.Utils.checkNotNull;

/**
 * @author Mr.Yuan
 * @since 2016/11/28.
 */
public class Retrofit {
    private final Map<Method, ServiceMethod<?,?>> serviceMethodCache = new ConcurrentHashMap<>();

    final HttpUrl baseUrl;
    final List<HttpConverter.Factory> converterFactories;
    final List<HttpCallAdapter.Factory> adapterFactories;
    final Map<Class, MethodAnnotationHandler> annotationHandlerMap;
    final Executor callbackExecutor;
    final boolean validateEagerly;
    final HttpEngine httpEngine;


    Retrofit(HttpUrl baseUrl, List<HttpConverter.Factory> converterFactories,
             List<HttpCallAdapter.Factory> adapterFactories, Executor callbackExecutor, boolean validateEagerly,
             HttpEngine httpEngine, Map<Class, MethodAnnotationHandler> annotationHandlerMap) {
        this.annotationHandlerMap = annotationHandlerMap;
        this.baseUrl = baseUrl;
        this.converterFactories = converterFactories;
        this.adapterFactories = adapterFactories;
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
        this.httpEngine = httpEngine;
    }

    public <T> T create(final Class<T> service) {
        Utils.validateServiceInterface(service);
        if (validateEagerly) {
            eagerlyValidateMethods(service);
        }
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object... args)
                            throws Throwable {
                        // If the method is a method from Object then defer to normal invocation.
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }
                        //加载ServiceMethod
                        ServiceMethod<Object,Object> serviceMethod =
                                (ServiceMethod<Object, Object>) loadServiceMethod(method);
                        // 生成HttpCall对象
                        HttpCall<Object> call = serviceMethod.httpEngine.newHttpCall(serviceMethod, args);
                        return serviceMethod.callAdapter.adapt(call);
                    }
                });
    }

    private void eagerlyValidateMethods(Class<?> service) {
        for (Method method : service.getDeclaredMethods()) {
            loadServiceMethod(method);
        }
    }

    ServiceMethod<?, ?> loadServiceMethod(Method method) {
        ServiceMethod<?, ?> result = serviceMethodCache.get(method);
        if (result != null) return result;

        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                result = new ServiceMethod.Builder<>(this, method).build();
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }


    public HttpUrl baseUrl() {
        return baseUrl;
    }

    public List<HttpCallAdapter.Factory> callAdapterFactories() {
        return adapterFactories;
    }

    public HttpCallAdapter<?,?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    public HttpCallAdapter<?,?> nextCallAdapter(HttpCallAdapter.Factory skipPast, Type returnType,
                                                 Annotation[] annotations) {
        checkNotNull(returnType, "returnType == null");
        checkNotNull(annotations, "annotations == null");

        int start = adapterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = adapterFactories.size(); i < count; i++) {
            HttpCallAdapter<?,?> adapter = adapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = adapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }


    public List<HttpConverter.Factory> converterFactories() {
        return converterFactories;
    }


    public <T> HttpConverter<T, RequestBody> requestBodyConverter(Type type,
                                                                  Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
    }

    public <T> HttpConverter<T, RequestBody> nextRequestBodyConverter(HttpConverter.Factory skipPast,
                                                                      Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        checkNotNull(type, "type == null");
        checkNotNull(parameterAnnotations, "parameterAnnotations == null");
        checkNotNull(methodAnnotations, "methodAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            HttpConverter.Factory factory = converterFactories.get(i);
            HttpConverter<?, RequestBody> converter =
                    factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (HttpConverter<T, RequestBody>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate RequestBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    public <T> HttpConverter<InputStream, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    public <T> HttpConverter<InputStream, T> nextResponseBodyConverter(HttpConverter.Factory skipPast,
                                                                        Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            HttpConverter<InputStream, ?> converter =
                    converterFactories.get(i).responseBodyConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (HttpConverter<InputStream, T>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    public <T> HttpConverter<T, String> stringConverter(Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            HttpConverter<?, String> converter =
                    converterFactories.get(i).stringConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (HttpConverter<T, String>) converter;
            }
        }

        return (HttpConverter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    public Executor callbackExecutor() {
        return callbackExecutor;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public MethodAnnotationHandler getMethodAnnotationHandler(Annotation annotation) {
        return annotationHandlerMap.get(annotation.annotationType());
    }

    public static final class Builder {
        private HttpUrl baseUrl;
        private final List<HttpConverter.Factory> converterFactories = new ArrayList<>();
        private final List<HttpCallAdapter.Factory> adapterFactories = new ArrayList<>();
        private Executor callbackExecutor;
        private boolean validateEagerly;
        private HttpEngine httpEngine;
        private Map<Class, MethodAnnotationHandler> annotationHandlerMap;

        public Builder() {
            // Add the built-in converter factory first. This prevents overriding its behavior but also
            // ensures correct behavior when using converters that consume all types.

            converterFactories.add(new BuiltInConverters());

            annotationHandlerMap = new HashMap<>();
            HttpMethodAnnotationHandler handler = new HttpMethodAnnotationHandler();
            addMethodAnnotationHandler(HttpMethod.POST.class, handler);
            addMethodAnnotationHandler(HttpMethod.PUT.class, handler);
            addMethodAnnotationHandler(HttpMethod.PATCH.class, handler);
            addMethodAnnotationHandler(HttpMethod.OPTIONS.class, handler);
            addMethodAnnotationHandler(HttpMethod.HTTP.class, handler);
            addMethodAnnotationHandler(HttpMethod.GET.class, handler);
            addMethodAnnotationHandler(HttpMethod.DELETE.class, handler);
            addMethodAnnotationHandler(HttpMethod.HEAD.class, handler);
            addMethodAnnotationHandler(Headers.class,handler);
            addMethodAnnotationHandler(Multipart.class,handler);
            addMethodAnnotationHandler(FormUrlEncoded.class,handler);
        }

        public Builder(Retrofit retrofit) {
            this();
            baseUrl = retrofit.baseUrl;
            converterFactories.addAll(retrofit.converterFactories);
            adapterFactories.addAll(retrofit.adapterFactories);
            // Remove the default, platform-aware call adapter added by build().
            adapterFactories.remove(adapterFactories.size() - 1);
            callbackExecutor = retrofit.callbackExecutor;
            validateEagerly = retrofit.validateEagerly;
            httpEngine = retrofit.httpEngine;
        }

        public Builder engine(HttpEngine engine) {
            checkNotNull(engine, "client == null");
            this.httpEngine = engine;
            return this;
        }

        public Builder addMethodAnnotationHandler(Class annotationClass, MethodAnnotationHandler methodAnnotationHandler) {
            if (!annotationClass.isAnnotation()) {
                throw new IllegalArgumentException(Utils.format("%s isn't an annotation", annotationClass));
            }
            annotationHandlerMap.put(annotationClass, methodAnnotationHandler);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            HttpUrl httpUrl = HttpUrl.parse(baseUrl);
            if (httpUrl == null) {
                throw new IllegalArgumentException("Illegal URL: " + baseUrl);
            }
            return baseUrl(httpUrl);
        }


        public Builder baseUrl(HttpUrl baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            List<String> pathSegments = baseUrl.pathSegments();
            if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
            }
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder addConverterFactory(HttpConverter.Factory factory) {
            converterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        public Builder addCallAdapterFactory(HttpCallAdapter.Factory factory) {
            adapterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = checkNotNull(executor, "executor == null");
            return this;
        }

        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        public Retrofit build() {
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }
            checkNotNull(httpEngine, "http engine required");

            Executor callbackExecutor = this.callbackExecutor;

            // Make a defensive copy of the adapters and add the default Call adapter.
            List<HttpCallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
            //remove it
            adapterFactories.add(DefaultCallAdapterFactory.getInstance());

            // Make a defensive copy of the converters.
            List<HttpConverter.Factory> converterFactories = new ArrayList<>(this.converterFactories);

            return new Retrofit(baseUrl, converterFactories, adapterFactories,
                    callbackExecutor, validateEagerly, httpEngine, annotationHandlerMap);
        }
    }
}
