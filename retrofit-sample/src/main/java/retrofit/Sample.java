package retrofit;

/**
 * @author Mr.Yuan
 * @since 2016/11/28.
 */
public class Sample {
    public static void main(String[] args) {
        Retrofit retrofit =new Retrofit();
        retrofit.get(GithubService.class);
    }
}
