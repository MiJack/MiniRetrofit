package retrofit.http.bean;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public final class MediaType {
    private static final String TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)";
    private static final String QUOTED = "\"([^\"]*)\"";
    private static final Pattern TYPE_SUBTYPE = Pattern.compile(TOKEN + "/" + TOKEN);
    private static final Pattern PARAMETER = Pattern.compile(
            ";\\s*(?:" + TOKEN + "=(?:" + TOKEN + "|" + QUOTED + "))?");

    private final String mediaType;
    private final String type;
    private final String subtype;
    private final String charset;

    private MediaType(String mediaType, String type, String subtype, String charset) {
        this.mediaType = mediaType;
        this.type = type;
        this.subtype = subtype;
        this.charset = charset;
    }

    public static MediaType parse(String string) {
        Matcher typeSubtype = TYPE_SUBTYPE.matcher(string);
        if (!typeSubtype.lookingAt()) return null;
        String type = typeSubtype.group(1).toLowerCase(Locale.US);
        String subtype = typeSubtype.group(2).toLowerCase(Locale.US);

        String charset = null;
        Matcher parameter = PARAMETER.matcher(string);
        for (int s = typeSubtype.end(); s < string.length(); s = parameter.end()) {
            parameter.region(s, string.length());
            if (!parameter.lookingAt()) return null; // This is not a well-formed media type.

            String name = parameter.group(1);
            if (name == null || !name.equalsIgnoreCase("charset")) continue;
            String charsetParameter;
            String token = parameter.group(2);
            if (token != null) {
                // If the token is 'single-quoted' it's invalid! But we're lenient and strip the quotes.
                charsetParameter = (token.startsWith("'") && token.endsWith("'") && token.length() > 2)
                        ? token.substring(1, token.length() - 1)
                        : token;
            } else {
                // Value is "double-quoted". That's valid and our regex group already strips the quotes.
                charsetParameter = parameter.group(3);
            }
            if (charset != null && !charsetParameter.equalsIgnoreCase(charset)) {
                throw new IllegalArgumentException("Multiple different charsets: " + string);
            }
            charset = charsetParameter;
        }

        return new MediaType(string, type, subtype, charset);
    }

    public String type() {
        return type;
    }

    public String subtype() {
        return subtype;
    }

    public Charset charset() {
        return charset != null ? Charset.forName(charset) : null;
    }

    public Charset charset(Charset defaultValue) {
        return charset != null ? Charset.forName(charset) : defaultValue;
    }

    @Override public String toString() {
        return mediaType;
    }

    @Override public boolean equals(Object o) {
        return o instanceof MediaType && ((MediaType) o).mediaType.equals(mediaType);
    }

    @Override public int hashCode() {
        return mediaType.hashCode();
    }
}

