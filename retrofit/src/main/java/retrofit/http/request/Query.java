package retrofit.http.request;

public @interface Query {
    String value();

    boolean encoded() default false;
}
