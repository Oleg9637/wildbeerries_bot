package ru.kutoven

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.kutoven.scraper.WildberriesReviewsScraper
import java.io.File
import kotlinx.coroutines.launch

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Wildberries Reviews Scraper API\n\nEndpoints:\n- GET /scrape?url=<product_url> - Scrape reviews from product page\n- GET /status - Check scraping status and list generated files")
        }

        get("/scrape") {
            val url = call.request.queryParameters["url"]

            if (url.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Missing 'url' parameter",
                        "example" to "/scrape?url=https://www.wildberries.ru/catalog/521896959/feedbacks?imtId=234818091"
                    )
                )
                return@get
            }

            try {
                val timestamp = System.currentTimeMillis()
                val outputFile = "/app/output/reviews_${timestamp}.csv"

                call.respond(
                    HttpStatusCode.Accepted,
                    mapOf(
                        "status" to "Scraping started",
                        "url" to url,
                        "output_file" to outputFile,
                        "message" to "Check /status endpoint for progress"
                    )
                )

                application.launch {
                    val scraper = WildberriesReviewsScraper()
                    try {
                        println("Initializing scraper...")
                        scraper.initializeDriver()

                        println("Scraping reviews from: $url")
                        val reviews = scraper.scrapeReviews(url)

                        println("Saving ${reviews.size} reviews to $outputFile")
                        scraper.saveToCSV(reviews, outputFile)

                        println("Scraping completed successfully! File: $outputFile")
                    } catch (e: Exception) {
                        println("Error during scraping: ${e.message}")
                        e.printStackTrace()
                    } finally {
                        scraper.close()
                    }
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to start scraping: ${e.message}")
                )
            }
        }

        get("/status") {
            try {
                val outputDir = File("/app/output")
                if (!outputDir.exists()) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Output directory does not exist")
                    )
                    return@get
                }

                val files = outputDir.listFiles()?.filter { it.isFile && it.extension == "csv" }?.sortedByDescending { it.lastModified() }

                if (files.isNullOrEmpty()) {
                    call.respond(
                        mapOf(
                            "status" to "No CSV files found",
                            "output_directory" to "/app/output",
                            "message" to "Use /scrape?url=<product_url> to start scraping"
                        )
                    )
                } else {
                    call.respond(
                        mapOf(
                            "status" to "OK",
                            "total_files" to files.size,
                            "latest_file" to files.first().name,
                            "latest_size_bytes" to files.first().length(),
                            "all_files" to files.map {
                                mapOf(
                                    "name" to it.name,
                                    "size_bytes" to it.length(),
                                    "created_at" to it.lastModified()
                                )
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to read status: ${e.message}")
                )
            }
        }
    }
}