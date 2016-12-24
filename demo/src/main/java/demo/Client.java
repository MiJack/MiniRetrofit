package demo;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit.Retrofit;
import retrofit.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit.conveter.gson.GsonConverterFactory;
import retrofit.engine.okhttp.OkHttpEngine;
import retrofit.HttpResponse;

import java.io.IOException;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public class Client {
    public static void main(String[] args) throws IOException {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String s) {
                System.out.println(s);
            }
        });
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        OkHttpEngine engine = new OkHttpEngine(client);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .engine(engine)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Api api = retrofit.create(Api.class);
//        HttpCall<User> mijack = api.getUser("mijack");
//        User user = mijack.execute().body();
//        System.out.println("mijack's login is " + user.login);

        HttpResponse execute = api
                .post("Yuan", "Yujie")
                .execute();
        Object body = execute.body();


//        api.getUser2("chih").subscribe(new Subscriber<User>() {
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
