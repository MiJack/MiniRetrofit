package demo;

import retrofit.HttpCall;
import retrofit.Retrofit;
import retrofit.conveter.gson.GsonConverterFactory;
import retrofit.engine.OkHttpEngine;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public class Client {
    public static void main(String[] args) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .engine(new OkHttpEngine())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Api api = retrofit.create(Api.class);
        HttpCall<User> httpCall = api.getUser("mijack");
        System.out.println(httpCall);
    }
}
