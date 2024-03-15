package com.example.testapi.data.response

import com.google.gson.annotations.SerializedName

data class ResultResponse(

	@field:SerializedName("created_at")
	val createdAt: String,

	@field:SerializedName("error")
	val error: Any,

	@field:SerializedName("version")
	val version: String,

	@field:SerializedName("output")
	val output: String,

	@field:SerializedName("completed_at")
	val completedAt: String,

	@field:SerializedName("started_at")
	val startedAt: String,

	@field:SerializedName("model")
	val model: String,

	@field:SerializedName("id")
	val id: String,

	@field:SerializedName("metrics")
	val metrics: Metrics,

	@field:SerializedName("logs")
	val logs: String,

	@field:SerializedName("status")
	val status: String
)

data class Metrics(

	@field:SerializedName("predict_time")
	val predictTime: Any
)
