package retrofit.adapter.rxjava;

import retrofit.HttpResponse;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public final class HttpException extends Exception {
    private final int code;
    private final String message;
    private final transient HttpResponse<?,?,?> response;

    public HttpException(HttpResponse<?,?,?> response) {
        super("HTTP " + response.code() + " " + response.message());
        this.code = response.code();
        this.message = response.message();
        this.response = response;
    }

    public int code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }

    public HttpResponse<?,?,?> response() {
        return this.response;
    }
}
