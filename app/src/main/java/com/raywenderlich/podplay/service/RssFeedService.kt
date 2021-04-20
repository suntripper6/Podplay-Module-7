package com.raywenderlich.podplay.service

import okhttp3.*
import java.io.IOException

class RssFeedService: FeedService {
    override fun getFeed(xmlFileURL: String, callback: (RssFeedResponse?) -> Unit) {
        // 1
        val client = OkHttpClient()
        //  2
        val request = Request.Builder()
            .url(xmlFileURL)
            .build()
        // 3
        client.newCall(request).enqueue(object: Callback {
            // 4
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                // 6
                if (response.isSuccessful) {
                    // 7
                    response.body()?.let { responseBody ->
                        // 8
                        println(responseBody.toString())
                        // Parse response and send to callback
                        return
                    }
                }
                // 9
                callback(null)
            }
        })
    }
}

interface FeedService {
    // 1
    fun getFeed(xmlFileURL: String, callback: (RssFeedResponse?) -> Unit)
    // 2
    companion object {
        val instance: FeedService by lazy {
            RssFeedService()
        }
    }
}