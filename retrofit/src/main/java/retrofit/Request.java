package retrofit;

import java.util.Map;

/**
 * @author Mr.Yuan
 * @since 2016/11/28.
 */
public class Request {
    private final String method;
    private final String url;
    private final Map<String, String> header;
    private final String body;

    public Request(String method, String url, Map<String, String> header, String body) {
        this.method = method;
        this.url = url;
        this.header = header;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }
}
