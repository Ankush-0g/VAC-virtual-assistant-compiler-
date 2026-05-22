package com.example.codecompiler;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    @Headers({
            "Content-Type: application/json",
            "X-RapidAPI-Key: 868895b4eamsh54993546158e8bcp155e5ajsn4e7e425d2c47",
            "X-RapidAPI-Host: judge0-ce.p.rapidapi.com"
    })
    @POST("submissions?base64_encoded=false&wait=true")
    Call<ResponseBody> executeCode(@Body RequestBody body);

    @Headers({
            "Content-Type: application/json",
            "X-RapidAPI-Key: 868895b4eamsh54993546158e8bcp155e5ajsn4e7e425d2c47",
            "X-RapidAPI-Host: judge0-ce.p.rapidapi.com"
    })
    @POST("submissions?base64_encoded=true&wait=true")
    Call<ResponseBody> executeCodeBase64(@Body RequestBody body);
}
