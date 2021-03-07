package com.jakewharton.plex

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpException(val code: Int, message: String) : RuntimeException("$code $message")

suspend fun Call.awaitString(): String {
	return suspendCancellableCoroutine { continuation ->
		enqueue(object : Callback {
			override fun onResponse(call: Call, response: Response) {
				response.use {
					if (response.isSuccessful) {
						val body = response.body!!.string()
						continuation.resume(body)
					} else {
						continuation.resumeWithException(
							HttpException(response.code, response.message)
						)
					}
				}
			}
			override fun onFailure(call: Call, e: IOException) {
				continuation.resumeWithException(e)
			}
		})
		continuation.invokeOnCancellation {
			cancel()
		}
	}
}
