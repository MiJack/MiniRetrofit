package demo;

import retrofit.Retrofit;
import retrofit.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit.conveter.gson.GsonConverterFactory;
import retrofit.engine.okhttp.OkHttpEngine;
import rx.Subscriber;

import java.io.IOException;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public class Client {
    public static void main(String[] args) throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .engine(new OkHttpEngine())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Api api = retrofit.create(Api.class);
        api.getUser3("mijack");
//        api.getUser2("mijack").subscribe(new Subscriber<User>() {
//            @Override
//            public void onCompleted() {
//                System.out.println("onCompleted");
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//                System.out.println(throwable);
//            }
//
//            @Override
//            public void onNext(User user) {
//                System.out.println("user:" + user.login);
//            }
//        });
    }
}
