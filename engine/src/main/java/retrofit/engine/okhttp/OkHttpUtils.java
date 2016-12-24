package retrofit.engine.okhttp;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import retrofit.HttpHeaders;

import java.io.IOException;

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

    public static okhttp3.MediaType toContentType(retrofit.MediaType mediaType) {
        return okhttp3.MediaType.parse(mediaType.type());
    }

    public static okhttp3.RequestBody toOkHttpRequestBody(final retrofit.RequestBody body) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return OkHttpUtils.toContentType(body.getMediaType());
            }

            @Override
            public void writeTo(BufferedSink bufferedSink) throws IOException {
                bufferedSink.write(body.getByteString());
            }
        };
    }
}
