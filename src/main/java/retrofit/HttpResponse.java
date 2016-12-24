package retrofit;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 * @param <T> response对应的目标类型
 * @param <P1> 在engine中的response的类型
 * @param <P2> 在engine中的responsebody的类型
 */
public class HttpResponse<T, P1, P2> {
    P1 rawResponse;
    P2 errorBody;

    int code;
    HttpHeaders headers;
    String message;
    T body;

    public HttpResponse(P1 rawResponse, P2 errorBody, int code, HttpHeaders headers,
                        T body, String message) {
        this.rawResponse = rawResponse;
        this.errorBody = errorBody;
        this.code = code;
        this.headers = headers;
        this.body = body;
        this.message = message;
    }

    public T body() {
        return body;
    }

    public boolean isSuccessful() {
        return this.code >= 200 && this.code < 300;
    }

    public int code() {
        return this.code;
    }

    public String message() {
        return message;
    }
}
