package retrofit.http.request;

public @interface Part {
    String value() default "";

    String encoding() default "binary";
}
