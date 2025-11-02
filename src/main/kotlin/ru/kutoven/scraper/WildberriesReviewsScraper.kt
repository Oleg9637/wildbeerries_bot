package ru.kutoven.scraper

import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import ru.kutoven.model.Review

class WildberriesReviewsScraper(private val config: Properties = getDefaultConfig()) {

    private var driver: WebDriver? = null
    private val pageLoadTimeout: Duration = Duration.ofSeconds(config.getProperty("timeout.page.load", "30").toLong())
    private val scrollDelayMin: Long = config.getProperty("timeout.scroll.delay.min", "1500").toLong()
    private val scrollDelayMax: Long = config.getProperty("timeout.scroll.delay.max", "2500").toLong()
    private val initialDelayMin: Long = config.getProperty("timeout.initial.delay.min", "2000").toLong()
    private val initialDelayMax: Long = config.getProperty("timeout.initial.delay.max", "4000").toLong()
    private val maxNoChangeIterations: Int = config.getProperty("scroll.max.no.change.iterations", "5").toInt()

    companion object {
        fun getDefaultConfig(): Properties {
            val properties = Properties()
            properties.setProperty("timeout.page.load", "30")
            properties.setProperty("timeout.scroll.delay.min", "1500")
            properties.setProperty("timeout.scroll.delay.max", "2500")
            properties.setProperty("timeout.initial.delay.min", "2000")
            properties.setProperty("timeout.initial.delay.max", "4000")
            properties.setProperty("scroll.max.no.change.iterations", "5")
            return properties
        }
    }

    fun initializeDriver() {
        val chromeDriverPath = config.getProperty("webdriver.chrome.driver", "")
        if (chromeDriverPath.isNotBlank()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath)
        }

        val options = createChromeOptions()
        driver = ChromeDriver(options)
        configureDriver(driver as ChromeDriver)
    }

    private fun createChromeOptions(): ChromeOptions {
        val options = ChromeOptions()

        options.addArguments(
            "--headless=new",
            "--disable-dev-shm-usage",
            "--no-sandbox",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--start-maximized",
            "--disable-blink-features=AutomationControlled",
            "--disable-features=IsolateOrigins,site-per-process"
        )

        options.setExperimentalOption("excludeSwitches", listOf("enable-automation", "enable-logging"))
        options.setExperimentalOption("useAutomationExtension", false)

        options.addArguments(
            "user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        val prefs = mapOf(
            "profile.default_content_setting_values.notifications" to 2,
            "credentials_enable_service" to false,
            "profile.password_manager_enabled" to false
        )
        options.setExperimentalOption("prefs", prefs)

        val chromeBinary = config.getProperty("chrome.binary", "")
        if (chromeBinary.isNotBlank()) {
            options.setBinary(chromeBinary)
        }

        return options
    }

    private fun configureDriver(driver: ChromeDriver) {
        driver.executeCdpCommand(
            "Page.addScriptToEvaluateOnNewDocument",
            mapOf(
                "source" to """
                    Object.defineProperty(navigator, 'webdriver', {
                        get: () => undefined
                    });
                """.trimIndent()
            )
        )
    }

    fun scrapeReviews(url: String): List<Review> {
        val currentDriver = driver ?: throw IllegalStateException("Driver not initialized. Call initializeDriver() first.")

        currentDriver.get(url)

        Thread.sleep((initialDelayMin..initialDelayMax).random())

        waitForPageContent(currentDriver)
        scrollToLoadAllReviews(currentDriver)

        return parseReviews(currentDriver)
    }

    private fun waitForPageContent(driver: WebDriver) {
        val wait = WebDriverWait(driver, pageLoadTimeout)

        try {
            println("Waiting for page content to load...")
            wait.until { webDriver ->
                val pageSource = webDriver.pageSource ?: ""
                val hasReviews = webDriver.findElements(By.cssSelector(".comments__item.feedback")).isNotEmpty()
                val hasNoReviewsMessage = pageSource.contains("Отзывов пока нет") ||
                        pageSource.contains("отзывов не найдено")
                hasReviews || hasNoReviewsMessage
            }
            println("Page content loaded successfully")
        } catch (e: Exception) {
            println("Failed to load page content: ${e.message}")
            saveDebugInfo(driver)
            throw e
        }
    }

    private fun scrollToLoadAllReviews(driver: WebDriver) {
        var previousReviewCount: Int
        var currentReviewCount = 0
        var noChangeCounter = 0

        println("Loading all reviews by scrolling...")

        do {
            previousReviewCount = currentReviewCount

            (driver as JavascriptExecutor).executeScript("window.scrollTo(0, document.body.scrollHeight);")

            Thread.sleep((scrollDelayMin..scrollDelayMax).random())

            currentReviewCount = driver.findElements(By.cssSelector(".comments__item.feedback")).size
            println("Currently loaded: $currentReviewCount reviews")

            if (currentReviewCount == previousReviewCount) {
                noChangeCounter++
            } else {
                noChangeCounter = 0
            }

        } while (noChangeCounter < maxNoChangeIterations)

        println("Finished loading. Total reviews found: $currentReviewCount")
    }

    private fun parseReviews(driver: WebDriver): List<Review> {
        val reviewElements = driver.findElements(By.cssSelector(".comments__item.feedback"))
        val reviews = mutableListOf<Review>()

        for (element in reviewElements) {
            try {
                reviews.add(parseReviewElement(element))
            } catch (e: Exception) {
                println("Error parsing review: ${e.message}")
            }
        }

        return reviews
    }

    private fun parseReviewElement(element: WebElement): Review {
        val date = extractText(element, ".feedback__date")
        val author = extractText(element, ".feedback__info-header")
        val text = extractText(element, ".feedback__text")
        val rating = extractRating(element)
        val photosCount = element.findElements(By.cssSelector(".feedback__photo")).size
        val hasVideo = element.findElements(By.cssSelector(".feedback__video")).isNotEmpty()
        val tags = extractText(element, ".feedback__params")

        return Review(date, author, text, rating, photosCount, hasVideo, tags)
    }

    private fun extractText(parent: WebElement, selector: String): String {
        val elements = parent.findElements(By.cssSelector(selector))
        return if (elements.isNotEmpty()) elements[0].text else "N/A"
    }

    private fun extractRating(element: WebElement): String {
        val ratingElements = element.findElements(By.cssSelector(".feedback__rating"))
        if (ratingElements.isEmpty()) return "N/A"

        val ratingClass = ratingElements[0].getAttribute("class") ?: return "N/A"

        return when {
            ratingClass.contains("star5") -> "5"
            ratingClass.contains("star4") -> "4"
            ratingClass.contains("star3") -> "3"
            ratingClass.contains("star2") -> "2"
            ratingClass.contains("star1") -> "1"
            else -> "N/A"
        }
    }

    fun saveToCSV(reviews: List<Review>, outputFile: String) {
        FileWriter(outputFile, StandardCharsets.UTF_8).use { writer ->
            val csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("Date", "Author", "Text", "Rating", "Photos", "Video", "Tags")
                .build()

            CSVPrinter(writer, csvFormat).use { csvPrinter ->
                reviews.forEach { review ->
                    csvPrinter.printRecord(*review.toCsvArray())
                }
                csvPrinter.flush()
            }
        }
        println("Saved ${reviews.size} reviews to $outputFile")
    }

    private fun saveDebugInfo(driver: WebDriver, prefix: String = "error") {
        try {
            val screenshotFile = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
            screenshotFile.copyTo(File("${prefix}_screenshot.png"), overwrite = true)
            File("${prefix}_page_source.html").writeText(driver.pageSource ?: "")
            println("Debug information saved with prefix: $prefix")
        } catch (e: Exception) {
            println("Failed to save debug info: ${e.message}")
        }
    }

    fun close() {
        driver?.quit()
        driver = null
        println("Driver closed")
    }
}