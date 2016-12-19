package retrofit.adapter.rxjava;

import retrofit.http.bean.HttpResponse;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public final class Result<T> {
    private final HttpResponse<T, ?, ?> response;
    private final Throwable error;

    public static <T> Result<T> error(Throwable error) {
        if (error == null) {
            throw new NullPointerException("error == null");
        } else {
            return new Result(null, error);
        }
    }

    public static <T> Result<T> response(HttpResponse<T, ?, ?> response) {
        if (response == null) {
            throw new NullPointerException("response == null");
        } else {
            return new Result(response, (Throwable) null);
        }
    }

    private Result(HttpResponse<T, ?, ?> response, Throwable error) {
        this.response = response;
        this.error = error;
    }

    public HttpResponse<T, ?, ?> response() {
        return this.response;
    }

    public Throwable error() {
        return this.error;
    }

    public boolean isError() {
        return this.error != null;
    }
}
