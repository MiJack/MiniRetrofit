package retrofit.http.request;

public @interface Path {
    String value();

    boolean encoded() default false;
}
