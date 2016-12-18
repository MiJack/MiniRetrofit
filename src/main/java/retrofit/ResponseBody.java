package retrofit;

import java.io.Closeable;
import java.io.Reader;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 * ==inputStream
 */
public class ResponseBody implements Closeable {
    public Reader charStream() {
        return null;
    }

    public void close() {
    }
}
