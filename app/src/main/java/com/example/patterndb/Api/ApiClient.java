package com.example.patterndb.Api;


import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiClient {
    // Cambia la URL por la de tu servidor
    String BASE_URL = "https://dm-server-puzzle.vercel.app/"; // Usa tu IP local

    @POST("solve")
    Call<SolutionResponse> solvePuzzle(@Body PuzzleRequest request);

    class PuzzleRequest {
        public int[][] matrix;

        public PuzzleRequest(int[][] matrix) {
            this.matrix = matrix;
        }
    }

    class SolutionResponse {
        public List<int[][]> solution;
        public String error;
    }

    static ApiClient getClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiClient.class);
    }
}