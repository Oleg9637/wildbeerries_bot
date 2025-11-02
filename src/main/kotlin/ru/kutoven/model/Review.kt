package ru.kutoven.model

data class Review(
    val date: String,
    val author: String,
    val text: String,
    val rating: String,
    val photosCount: Int,
    val hasVideo: Boolean,
    val tags: String
) {
    fun toCsvArray(): Array<String> {
        return arrayOf(
            date,
            author,
            text,
            rating,
            photosCount.toString(),
            if (hasVideo) "Yes" else "No",
            tags
        )
    }
}