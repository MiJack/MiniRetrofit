package retrofit.core;

import retrofit.ServiceMethod;
import retrofit.http.*;
import retrofit.http.bean.HttpHeaders;
import retrofit.http.bean.MediaType;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public class HttpMethodAnnotationHandler implements MethodAnnotationHandler {

    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);


    @Override
    public void apply(Annotation annotation, ServiceMethod.Builder builder) {
        if (annotation instanceof HttpMethod.DELETE) {
            parseHttpMethodAndPath(builder, "DELETE", ((HttpMethod.DELETE) annotation).value(), false);
        } else if (annotation instanceof HttpMethod.GET) {
            parseHttpMethodAndPath(builder, "GET", ((HttpMethod.GET) annotation).value(), false);
        } else if (annotation instanceof HttpMethod.HEAD) {
            parseHttpMethodAndPath(builder, "HEAD", ((HttpMethod.HEAD) annotation).value(), false);
            if (!Void.class.equals(builder.responseType)) {
                throw methodError("HEAD method must use Void as response type.");
            }
        } else if (annotation instanceof HttpMethod.PATCH) {
            parseHttpMethodAndPath(builder, "PATCH", ((HttpMethod.PATCH) annotation).value(), true);
        } else if (annotation instanceof HttpMethod.POST) {
            parseHttpMethodAndPath(builder, "POST", ((HttpMethod.POST) annotation).value(), true);
        } else if (annotation instanceof HttpMethod.PUT) {
            parseHttpMethodAndPath(builder, "PUT", ((HttpMethod.PUT) annotation).value(), true);
        } else if (annotation instanceof HttpMethod.OPTIONS) {
            parseHttpMethodAndPath(builder, "OPTIONS", ((HttpMethod.OPTIONS) annotation).value(), false);
        } else if (annotation instanceof HttpMethod.HTTP) {
            HttpMethod.HTTP http = (HttpMethod.HTTP) annotation;
            parseHttpMethodAndPath(builder, http.method(), http.path(), http.hasBody());
        } else if (annotation instanceof Headers) {
            String[] headersToParse = ((Headers) annotation).value();
            if (headersToParse.length == 0) {
                throw methodError("@Headers annotation is empty.");
            }
            builder.headers = parseHeaders(builder, headersToParse);
        } else if (annotation instanceof Multipart) {
            if (builder.isFormEncoded) {
                throw methodError("Only one encoding annotation is allowed.");
            }
            builder.isMultipart = true;
        } else if (annotation instanceof FormUrlEncoded) {
            if (builder.isMultipart) {
                throw methodError("Only one encoding annotation is allowed.");
            }
            builder.isFormEncoded = true;
        }
    }

    private void parseHttpMethodAndPath(ServiceMethod.Builder builder, String httpMethod, String value, boolean hasBody) {
        if (builder.httpMethod != null) {
            methodError("Only one HTTP method is allowed. Found: %s and %s.",
                    builder.httpMethod, httpMethod);
        }
        builder.httpMethod = httpMethod;
        builder.hasBody = hasBody;

        if (value.isEmpty()) {
            return;
        }

        // Get the relative URL path and existing query string, if present.
        int question = value.indexOf('?');
        if (question != -1 && question < value.length() - 1) {
            // Ensure the query string does not have any named parameters.
            String queryParams = value.substring(question + 1);
            Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
            if (queryParamMatcher.find()) {
                throw methodError("URL query string \"%s\" must not have replace block. "
                        + "For dynamic query parameters use @Query.", queryParams);
            }
        }

        builder.relativeUrl = value;
        builder.relativeUrlParamNames = parsePathParameters(value);
    }

    static Set<String> parsePathParameters(String path) {
        Matcher m = PARAM_URL_REGEX.matcher(path);
        Set<String> patterns = new LinkedHashSet<>();
        while (m.find()) {
            patterns.add(m.group(1));
        }
        return patterns;
    }

    private HttpHeaders parseHeaders(ServiceMethod.Builder method, String[] headers) {
        HttpHeaders.Builder builder = new HttpHeaders.Builder();
        for (String header : headers) {
            int colon = header.indexOf(':');
            if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                throw methodError(
                        "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
            }
            String headerName = header.substring(0, colon);
            String headerValue = header.substring(colon + 1).trim();
            if ("Content-Type".equalsIgnoreCase(headerName)) {
                MediaType type = MediaType.parse(headerValue);
                if (type == null) {
                    throw methodError("Malformed content type: %s", headerValue);
                }
                method.contentType = type;
            } else {
                builder.add(headerName, headerValue);
            }
        }
        return builder.build();
    }


    private RuntimeException methodError(String message, Object... args) {
        message = String.format(message, args);
        return new IllegalArgumentException(message);
    }


}
