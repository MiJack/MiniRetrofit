package retrofit;

import java.util.Map;

/**
 * @author Mr.Yuan
 * @since 2016/11/28.
 */
public class Response {
    private final int code;
    private final String status;
    private final Map<String, String> header;
    private final String body;

    public Response(int code, String status, Map<String, String> header, String body) {
        this.code = code;
        this.status = status;
        this.header = header;
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }
}
