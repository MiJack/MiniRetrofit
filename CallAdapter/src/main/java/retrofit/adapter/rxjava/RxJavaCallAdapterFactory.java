package retrofit.adapter.rxjava;


import retrofit.Retrofit;
import retrofit.core.HttpCall;
import retrofit.core.HttpCallAdapter;
import retrofit.http.bean.HttpResponse;
import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.Func1;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RxJavaCallAdapterFactory extends HttpCallAdapter.Factory {
    private final Scheduler scheduler;

    public static RxJavaCallAdapterFactory create() {
        return new RxJavaCallAdapterFactory((Scheduler) null);
    }

    public static RxJavaCallAdapterFactory createWithScheduler(Scheduler scheduler) {
        if (scheduler == null) {
            throw new NullPointerException("scheduler == null");
        } else {
            return new RxJavaCallAdapterFactory(scheduler);
        }
    }

    private RxJavaCallAdapterFactory(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public HttpCallAdapter<?,?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class rawType = getRawType(returnType);
        String canonicalName = rawType.getCanonicalName();
        boolean isSingle = "rx.Single".equals(canonicalName);
        boolean isCompletable = "rx.Completable".equals(canonicalName);
        if (rawType != Observable.class && !isSingle && !isCompletable) {
            return null;
        } else if (!isCompletable && !(returnType instanceof ParameterizedType)) {
            String callAdapter1 = isSingle ? "Single" : "Observable";
            throw new IllegalStateException(callAdapter1 + " return type must be parameterized as " + callAdapter1 + "<Foo> or " + callAdapter1 + "<? extends Foo>");
        } else if (isCompletable) {
            return CompletableHelper.createCallAdapter(this.scheduler);
        } else {
            HttpCallAdapter callAdapter = this.getCallAdapter(returnType, this.scheduler);
            return isSingle ? SingleHelper.makeSingle(callAdapter) : callAdapter;
        }
    }

    private HttpCallAdapter<Observable<?>,?> getCallAdapter(Type returnType, Scheduler scheduler) {
        Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
        Class rawObservableType = getRawType(observableType);
        Type responseType;
        if (rawObservableType == HttpResponse.class) {
            if (!(observableType instanceof ParameterizedType)) {
                throw new IllegalStateException("Response must be parameterized as Response<Foo> or Response<? extends Foo>");
            } else {
                responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
                return new RxJavaCallAdapterFactory.ResponseCallAdapter(responseType, scheduler);
            }
        } else if (rawObservableType == Result.class) {
            if (!(observableType instanceof ParameterizedType)) {
                throw new IllegalStateException("Result must be parameterized as Result<Foo> or Result<? extends Foo>");
            } else {
                responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
                return new RxJavaCallAdapterFactory.ResultCallAdapter(responseType, scheduler);
            }
        } else {
            return new RxJavaCallAdapterFactory.SimpleCallAdapter(observableType, scheduler);
        }
    }

    static final class ResultCallAdapter<R> implements HttpCallAdapter<Observable<?>,R> {
        private final Type responseType;
        private final Scheduler scheduler;

        ResultCallAdapter(Type responseType, Scheduler scheduler) {
            this.responseType = responseType;
            this.scheduler = scheduler;
        }

        public Type responseType() {
            return this.responseType;
        }

        public   Observable<Result<R>> adapt(HttpCall<R> call) {
            Observable observable =
                    Observable
                            .create(new RxJavaCallAdapterFactory.CallOnSubscribe(call))
                            .map(
                                    new Func1<HttpResponse<R, ?, ?>, Result<R>>() {
                                        public Result<R> call(HttpResponse<R, ?, ?> response) {
                                            return Result.response(response);
                                        }
                                    })
                            .onErrorReturn(new Func1<Throwable, Result<R>>() {
                                public Result<R> call(Throwable throwable) {
                                    return Result.error(throwable);
                                }
                            });
            return this.scheduler != null ? observable.subscribeOn(this.scheduler) : observable;
        }
    }

    static final class SimpleCallAdapter<R> implements HttpCallAdapter<Observable<?>,R> {
        private final Type responseType;
        private final Scheduler scheduler;

        SimpleCallAdapter(Type responseType, Scheduler scheduler) {
            this.responseType = responseType;
            this.scheduler = scheduler;
        }

        public Type responseType() {
            return this.responseType;
        }

        public  Observable<R> adapt(HttpCall<R> call) {
            Observable observable = Observable.create(new RxJavaCallAdapterFactory.CallOnSubscribe(call)).lift(OperatorMapResponseToBodyOrError.instance());
            return this.scheduler != null ? observable.subscribeOn(this.scheduler) : observable;
        }
    }

    static final class ResponseCallAdapter<R> implements HttpCallAdapter<Observable<?>,R> {
        private final Type responseType;
        private final Scheduler scheduler;

        ResponseCallAdapter(Type responseType, Scheduler scheduler) {
            this.responseType = responseType;
            this.scheduler = scheduler;
        }

        public Type responseType() {
            return this.responseType;
        }

        public  Observable<HttpResponse<R, ?, ?>> adapt(HttpCall<R> call) {
            Observable observable = Observable.create(new RxJavaCallAdapterFactory.CallOnSubscribe(call));
            return this.scheduler != null ? observable.subscribeOn(this.scheduler) : observable;
        }
    }

    static final class RequestArbiter<T> extends AtomicBoolean implements Subscription, Producer {
        private final HttpCall<T> call;
        private final Subscriber<? super HttpResponse<T, ?, ?>> subscriber;

        RequestArbiter(HttpCall<T> call, Subscriber<? super HttpResponse<T, ?, ?>> subscriber) {
            this.call = call;
            this.subscriber = subscriber;
        }

        public void request(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("n < 0: " + n);
            } else if (n != 0L) {
                if (this.compareAndSet(false, true)) {
                    try {
                        HttpResponse<T,?,?> t = this.call.execute();
                        if (!this.subscriber.isUnsubscribed()) {
                            this.subscriber.onNext(t);
                        }
                    } catch (Throwable var4) {
                        Exceptions.throwIfFatal(var4);
                        if (!this.subscriber.isUnsubscribed()) {
                            this.subscriber.onError(var4);
                        }

                        return;
                    }

                    if (!this.subscriber.isUnsubscribed()) {
                        this.subscriber.onCompleted();
                    }

                }
            }
        }

        public void unsubscribe() {
            this.call.cancel();
        }

        public boolean isUnsubscribed() {
            return this.call.isCanceled();
        }
    }

    static final class CallOnSubscribe<T> implements Observable.OnSubscribe<HttpResponse<T, ?, ?>> {
        private final HttpCall<T> originalCall;

        CallOnSubscribe(HttpCall<T> originalCall) {
            this.originalCall = originalCall;
        }

        public void call(Subscriber<? super HttpResponse<T, ?, ?>> subscriber) {
            HttpCall call = this.originalCall.clone();
            RxJavaCallAdapterFactory.RequestArbiter requestArbiter = new RxJavaCallAdapterFactory.RequestArbiter(call, subscriber);
            subscriber.add(requestArbiter);
            subscriber.setProducer(requestArbiter);
        }
    }
}
