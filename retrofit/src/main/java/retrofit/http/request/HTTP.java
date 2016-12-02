package retrofit.http.request;


public @interface HTTP {
    String method();

    String path() default "";

    boolean hasBody() default false;
}
