package demo;

import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit.RequestBody;
import retrofit.core.HttpCall;
import retrofit.http.*;
import retrofit.HttpResponse;

/**
 * @author Mr.Yuan
 * @since 2016/12/17.
 */
public interface Api {
    @HttpMethod.GET("/users/{user}")
    HttpCall<User> getUser(@Path("user")String user);
    @HttpMethod.GET("/users/{user}")
    @Headers("sada:asda")
    HttpResponse<User, Response, ResponseBody> getUser2(@Path("user")String user);
    @HttpMethod.GET("/users/{user}")
    HttpCall<User> getUser3(@Path("user") String user);
    @HttpMethod.POST("/post")
    @FormUrlEncoded
    HttpCall<retrofit.ResponseBody> post(
            @Field("first_name") String first,
            @Field("last_name") String last);


    @HttpMethod.POST("/post")
    @Multipart
    HttpCall<retrofit.ResponseBody> postPart(
            @Part("first_name") String first,
            @Part("last_name") String last);


    @HttpMethod.POST("/post")
    @Multipart
    HttpCall<retrofit.ResponseBody> postPart(@Part("first_name") String first, @Part("last_name") String last,
                                             @Part("file") RequestBody requestBody);
}
