package com.example.testapi.data.response


data class MyData(
	var input: Inputs,
	var version: String
)

data class Inputs(
	var image: String,
	var prompt: String
)
