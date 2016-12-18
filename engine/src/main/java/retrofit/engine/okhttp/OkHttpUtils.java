package retrofit.engine.okhttp;

import okhttp3.Headers;
import retrofit.http.bean.HttpHeaders;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public class OkHttpUtils {
    public static Headers toHeaders(HttpHeaders headers) {
        Headers.Builder headerBuilder = new Headers.Builder();
        for (String name : headers.names()) {
            headerBuilder.add(name, headers.get(name));
        }
        return headerBuilder.build();
    }

    public static HttpHeaders toHttpHeaders(Headers headers) {
        HttpHeaders.Builder builder = new HttpHeaders.Builder();
        for (String name : headers.names()) {
            builder.add(name, headers.get(name));
        }
        return builder.build();
    }
}
