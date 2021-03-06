package retrofit.core;

import retrofit.HttpResponse;
import retrofit.ServiceMethod;

import java.io.IOException;
import java.io.InputStream;

/**
 * @param <T> response对应的目标类型
 *            // * @param <Q>  在engine中request的类型
 *            // * @param <P1> 在engine中的response的类型
 *            // * @param <P2> 在engine中的responsebody的类型
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public abstract class HttpCall<T> {

    public final ServiceMethod<T,?> serviceMethod;
    public final Object[] args;
    protected HttpEngine httpEngine;
    protected volatile boolean canceled;

    public Throwable creationFailure; // Either a RuntimeException or IOException.
    public boolean executed;

    public HttpCall(ServiceMethod<T,?> serviceMethod, Object[] args) {
        this.serviceMethod = serviceMethod;
        this.args = args;
        httpEngine = serviceMethod.httpEngine;
    }

    public T toResponseBody(InputStream inputStream) throws IOException {
        return serviceMethod.responseConverter.convert(inputStream);
    }

    public HttpResponse<T, ?, ?> execute() throws IOException {
        return httpEngine.execute(this);
    }

    public void enqueue(HttpCallback<T> callback) {
        httpEngine.execute(this, callback);
    }

    public HttpCall<T> httpEngine(HttpEngine httpEngine) {
        return httpEngine.newHttpCall(serviceMethod, args);
    }

    public boolean isExecuted() {
        return executed;
    }

    public void cancel() {
        canceled = true;
        httpEngine.cancel(this);
    }

    public boolean isCanceled() {
        return canceled;
    }

    public abstract HttpCall<T> clone();

    public void setHttpEngine(HttpEngine httpEngine) {
        this.httpEngine = httpEngine;
    }

    public void check() throws IOException {
        if (executed) throw new IllegalStateException("Already executed.");
        executed = true;

        if (creationFailure != null) {
            if (creationFailure instanceof IOException) {
                throw (IOException) creationFailure;
            } else {
                throw (RuntimeException) creationFailure;
            }
        }
    }
}
