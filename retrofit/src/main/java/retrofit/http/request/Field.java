package retrofit.http.request;

public @interface Field {
    String value();

    boolean encoded() default false;
}
