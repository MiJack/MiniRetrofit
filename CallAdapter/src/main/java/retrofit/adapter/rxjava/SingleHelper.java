package retrofit.adapter.rxjava;

import retrofit.core.HttpCall;
import retrofit.core.HttpCallAdapter;
import rx.Observable;
import rx.Single;

import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
final class SingleHelper {
    SingleHelper() {
    }

    static <R> HttpCallAdapter<Single<?>, R> makeSingle(final HttpCallAdapter<Observable<?>, R> callAdapter) {
        return new HttpCallAdapter<Single<?>, R>() {
            public Type responseType() {
                return callAdapter.responseType();
            }

            @Override
            public Single<?> adapt(HttpCall<R> call) {
                Observable observable = callAdapter.adapt(call);
                return observable.toSingle();
            }
        };
    }
}

