package retrofit.adapter.rxjava;

import retrofit.core.HttpCall;
import retrofit.core.HttpCallAdapter;
import retrofit.http.bean.HttpResponse;
import rx.Completable;
import rx.Scheduler;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.lang.reflect.Type;

/**
 * @author Mr.Yuan
 * @since 2016/12/18.
 */
final class CompletableHelper {
    CompletableHelper() {
    }

    static HttpCallAdapter<Completable,?> createCallAdapter(Scheduler scheduler) {
        return new CompletableHelper.CompletableCallAdapter(scheduler);
    }

    static class CompletableCallAdapter implements HttpCallAdapter<Completable,HttpCall> {
        private final Scheduler scheduler;

        CompletableCallAdapter(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        public Type responseType() {
            return Void.class;
        }

        public Completable adapt(HttpCall call) {
            Completable completable = Completable.create(new CompletableHelper.CompletableCallOnSubscribe(call));
            return this.scheduler != null?completable.subscribeOn(this.scheduler):completable;
        }
    }

    private static final class CompletableCallOnSubscribe implements Completable.CompletableOnSubscribe {
        private final HttpCall originalCall;

        CompletableCallOnSubscribe(HttpCall originalCall) {
            this.originalCall = originalCall;
        }

        public void call(Completable.CompletableSubscriber subscriber) {
            final HttpCall call = this.originalCall.clone();
            Subscription subscription = Subscriptions.create(new Action0() {
                public void call() {
                    call.cancel();
                }
            });
            subscriber.onSubscribe(subscription);

            try {
                HttpResponse t = call.execute();
                if(!subscription.isUnsubscribed()) {
                    if(t.isSuccessful()) {
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(new HttpException(t));
                    }
                }
            } catch (Throwable var5) {
                Exceptions.throwIfFatal(var5);
                if(!subscription.isUnsubscribed()) {
                    subscriber.onError(var5);
                }
            }

        }
    }
}

