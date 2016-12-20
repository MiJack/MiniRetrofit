package retrofit;

import okio.ByteString;
import retrofit.http.bean.MediaType;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public class RequestBody {
    private final MediaType mediaType;
    private final ByteString byteString;

    public RequestBody(MediaType mediaType, ByteString byteString) {
        this.mediaType = mediaType;
        this.byteString = byteString;
    }

    public static RequestBody create(MediaType mediaType, ByteString byteString) {
        return new RequestBody(mediaType,byteString);
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public ByteString getByteString() {
        return byteString;
    }
}
