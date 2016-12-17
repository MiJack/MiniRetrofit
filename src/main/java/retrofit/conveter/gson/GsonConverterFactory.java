package retrofit.conveter.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import retrofit.RequestBody;
import retrofit.ResponseBody;
import retrofit.Retrofit;
import retrofit.core.HttpConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public final class GsonConverterFactory extends HttpConverter.Factory {
    private final Gson gson;

    public static GsonConverterFactory create() {
        return create(new Gson());
    }

    public static GsonConverterFactory create(Gson gson) {
        return new GsonConverterFactory(gson);
    }

    private GsonConverterFactory(Gson gson) {
        if(gson == null) {
            throw new NullPointerException("gson == null");
        } else {
            this.gson = gson;
        }
    }

    public HttpConverter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        TypeAdapter adapter = this.gson.getAdapter(TypeToken.get(type));
        return new GsonResponseBodyConverter(this.gson, adapter);
    }

    public HttpConverter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        TypeAdapter adapter = this.gson.getAdapter(TypeToken.get(type));
        return new GsonRequestBodyConverter(this.gson, adapter);
    }
}
