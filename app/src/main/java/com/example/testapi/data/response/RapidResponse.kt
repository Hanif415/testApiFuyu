package com.example.testapi.data.response

import com.google.gson.annotations.SerializedName

data class RapidResponse(

	@field:SerializedName("data")
	val data: Data? = null
)

data class Translations(

	@field:SerializedName("translatedText")
	val translatedText: String? = null
)

data class Data(

	@field:SerializedName("translations")
	val translations: Translations? = null
)
