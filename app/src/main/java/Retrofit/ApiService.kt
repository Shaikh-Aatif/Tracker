package Retrofit

import androidx.core.app.NotificationCompat.StreamType
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

//    @GET("u/0/uc?id=1S4JgDRz_9dh1GBvQO0QwTX_5bKyNc_pb&export=download")
    @Streaming
    @GET("https://aatif.blob.core.windows.net/android/app-debug.apk")
    suspend fun downloadApk():  Response<ResponseBody>
//    https://drive.usercontent.google.com/u/0/uc?id=1S4JgDRz_9dh1GBvQO0QwTX_5bKyNc_pb&export=download
//    https://drive.google.com/u/0/uc?id=1Z1MH13LYN0hnmFNxSyJDDNj74drlEL6c&export=download
}