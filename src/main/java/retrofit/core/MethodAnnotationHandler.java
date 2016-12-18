package retrofit.core;

import retrofit.ServiceMethod;

import java.lang.annotation.Annotation;

/**
 * 对应于方法的annotation
 *
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public interface MethodAnnotationHandler {
    void apply(Annotation annotation, ServiceMethod.Builder builder);
}
