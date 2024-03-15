package com.example.testapi.data.response

import com.google.gson.annotations.SerializedName

data class TestResponse(

	@field:SerializedName("input")
	val input: Input,

	@field:SerializedName("urls")
	val urls: Urls,

	@field:SerializedName("created_at")
	val createdAt: String,

	@field:SerializedName("model")
	val model: String,

	@field:SerializedName("id")
	val id: String,

	@field:SerializedName("error")
	val error: Any,

	@field:SerializedName("version")
	val version: String,

	@field:SerializedName("logs")
	val logs: String,

	@field:SerializedName("status")
	val status: String
)

data class Input(

	@field:SerializedName("image")
	val image: String,

	@field:SerializedName("max_new_tokens")
	val maxNewTokens: Int,

	@field:SerializedName("prompt")
	val prompt: String
)

data class Urls(

	@field:SerializedName("cancel")
	val cancel: String,

	@field:SerializedName("get")
	val get: String
)
