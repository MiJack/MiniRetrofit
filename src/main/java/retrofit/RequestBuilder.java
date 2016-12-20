package retrofit;

import retrofit.http.bean.HttpHeaders;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
public abstract class RequestBuilder {

    public RequestBuilder(String httpMethod, retrofit.http.bean.HttpUrl baseUrl, String relativeUrl,
                          retrofit.http.bean.HttpHeaders headers, retrofit.http.bean.MediaType contentType,
                          boolean hasBody, boolean isFormEncoded, boolean isMultipart) {}

    public abstract void setRelativeUrl(Object relativeUrl);

    public abstract void addHeader(String name, String value);

    public abstract void addPathParam(String name, String value, boolean encoded);

    public abstract void addQueryParam(String name, String value, boolean encoded);

    public abstract void addFormField(String name, String value, boolean encoded);

    public abstract void addPart(HttpHeaders headers, RequestBody body);

    public abstract void addPart(RequestBody part);

    public abstract void setBody(RequestBody body);

    public abstract <T> T build();
}
