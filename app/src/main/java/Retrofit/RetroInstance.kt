package Retrofit;

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory;

public class RetroInstance {
    val retrofit = Retrofit.Builder()
            .baseUrl("") // Replace with your API base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val apiService = retrofit.create(ApiService::class.java)
}
