package retrofit.adapter.rxjava;

import retrofit.http.bean.HttpResponse;
import rx.Observable;
import rx.Subscriber;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
final class OperatorMapResponseToBodyOrError<T> implements Observable.Operator<T, HttpResponse<T,?,?>> {
    private static final OperatorMapResponseToBodyOrError<Object> INSTANCE = new OperatorMapResponseToBodyOrError();

    OperatorMapResponseToBodyOrError() {
    }

    static  OperatorMapResponseToBodyOrError<Object> instance() {
        return INSTANCE;
    }

    public Subscriber<? super HttpResponse<T,?,?>> call(final Subscriber<? super T> child) {
        return new Subscriber<HttpResponse<T,?,?>>(child) {
            public void onNext(HttpResponse<T,?,?> response) {
                if(response.isSuccessful()) {
                    child.onNext(response.body());
                } else {
                    child.onError(new HttpException(response));
                }

            }

            public void onCompleted() {
                child.onCompleted();
            }

            public void onError(Throwable e) {
                child.onError(e);
            }
        };
    }
}
