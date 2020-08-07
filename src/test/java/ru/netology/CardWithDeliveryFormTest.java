package ru.netology;


import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.*;
import static com.codeborne.selenide.Selenide.*;
import static org.junit.jupiter.api.Assertions.*;

public class CardWithDeliveryFormTest {
    static final long TIME_TO_SUCCESS_LOAD_MILLISECONDS = 15000L;
    static int COUNT_CORRECT_CITIES = 83;
    static final String CORRECT_CITIES_CSV_FILEPATH = "src/test/resources/CorrectCities.csv";
    static final int NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY = 3;
    static final String PATTERN_OF_DATE = "dd.MM.yyyy";

    static String getDateAfterCurrent(int daysToAdd) {
        return getDateAfterCurrent(daysToAdd, PATTERN_OF_DATE);
    }

    static String getDateAfterCurrent(int daysToAdd, String pattern) {
        LocalDate date = LocalDate.now();
        date = date.plusDays(daysToAdd);
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    static String getRandomCorrectCity() {
        String returnCity = null;
        try (BufferedReader bufReader =
                     new BufferedReader(new FileReader(CORRECT_CITIES_CSV_FILEPATH, StandardCharsets.UTF_8))) {
            int randomNumber = ThreadLocalRandom.current().nextInt(1, COUNT_CORRECT_CITIES);
            for (int i = 0; i < randomNumber; ++i) {
                returnCity = bufReader.readLine();
            }
            assert returnCity != null;
            returnCity = returnCity.replaceAll(",", "");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnCity;
    }

    static void clearInputField(SelenideElement elem) {
        elem.sendKeys("\b".repeat(elem.getValue().length()));
    }

    @BeforeAll
    static void setUpAll() {
        int countCities = 0;
        try (BufferedReader bufReader = new BufferedReader(new FileReader(CORRECT_CITIES_CSV_FILEPATH))) {

            while (bufReader.readLine() != null) {
                ++countCities;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        COUNT_CORRECT_CITIES = countCities;
    }

    @BeforeEach
    void setUp() {
        open("http://localhost:9999");
    }

    @Nested
    class HappyPathTests {
        @Test
        void shouldHappyPath() {
            String dateToTest = getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY);
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(dateToTest);
            $("[data-test-id=name] input").sendKeys("Иванов Иван");
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $("[data-test-id=agreement] input").parent().click();
            $$("button").find(exactText("Забронировать")).click();
            SelenideElement notification =
                    $("[data-test-id=notification]").waitUntil(visible, TIME_TO_SUCCESS_LOAD_MILLISECONDS);
            assertTrue(notification.find(withText("Успешно!")).isDisplayed());
            assertTrue(notification.find(withText(dateToTest)).isDisplayed());
        }

        @Test
        void shouldHappyPathWithDifficultName() {
            String dateToTest = getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY);
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(dateToTest);
            $("[data-test-id=name] input").sendKeys("Салтыков-Щедрин Игорь");
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $("[data-test-id=agreement] input").parent().click();
            $$("button").find(exactText("Забронировать")).click();
            SelenideElement notification =
                    $("[data-test-id=notification]").waitUntil(visible, TIME_TO_SUCCESS_LOAD_MILLISECONDS);
            assertTrue(notification.find(withText("Успешно!")).isDisplayed());
            assertTrue(notification.find(withText(dateToTest)).isDisplayed());
        }

        @Test
        void shouldHappyPathWithDashFormatDate() {
            String dateToCheck = getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY);
            String dateToTest = getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY, "dd-MM-yyyy");
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(dateToTest);
            $("[data-test-id=name] input").sendKeys("Иванов Иван");
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $("[data-test-id=agreement] input").parent().click();
            $$("button").find(exactText("Забронировать")).click();
            SelenideElement notification =
                    $("[data-test-id=notification]").waitUntil(visible, TIME_TO_SUCCESS_LOAD_MILLISECONDS);
            assertTrue(notification.find(withText("Успешно!")).isDisplayed());
            assertTrue(notification.find(withText(dateToCheck)).isDisplayed());
        }
    }

    @Nested
    class IncorrectDataInFieldsTests {
        @ParameterizedTest
        @EmptySource
        @CsvSource(value = {
                //"*/-*+.,^%$#@!~`",
                "Ялта",
                "Ivanov",
                "03404309"
        })
        void shouldSendWithIncorrectCity(String city) {
            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(
                    getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY));
            $("[data-test-id=name] input").sendKeys("Иванов Иван");
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $("[data-test-id=agreement] input").parent().click();

            $("[data-test-id=city] input").sendKeys(city);
            $$("button").find(exactText("Забронировать")).click();
            assertTrue($("[data-test-id=city].input_invalid").isDisplayed());
        }

        @ParameterizedTest
        @EmptySource
        @CsvSource(value = {
                "*/-*+.,^%$#@!~`",
                "Ivanov",
                "03404309",
        })
        void shouldSendWithIncorrectDateIncorrectInput(String date) {
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            $("[data-test-id=name] input").sendKeys("Иванов Иван");
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $("[data-test-id=agreement] input").parent().click();

            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(date);
            $$("button").find(exactText("Забронировать")).click();
            assertTrue($("[data-test-id=date] .input.input_invalid").isDisplayed());
        }

        @Test
        void shouldSendWithIncorrectDateIncorrectDate() {
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            $("[data-test-id=name] input").sendKeys("Иванов Иван");
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $("[data-test-id=agreement] input").parent().click();

            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(getDateAfterCurrent(0));
            $$("button").find(exactText("Забронировать")).click();
            assertTrue($("[data-test-id=date] .input.input_invalid").isDisplayed());
        }

        @ParameterizedTest
        @EmptySource
        @CsvSource(value = {
                "*/*+.,^%$#@!~`",
                "Ivanov",
                "03404309"
        })
        void shouldSendWithIncorrectName(String name) {
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(
                    getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY));
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $("[data-test-id=agreement] input").parent().click();

            $("[data-test-id=name] input").sendKeys(name);
            $$("button").find(exactText("Забронировать")).click();
            assertTrue($("[data-test-id=name].input_invalid").isDisplayed());
        }

        @ParameterizedTest
        @EmptySource
        @CsvSource(value = {
                "*/*+.,^%$#@!~`",
                "Ivanov",
                "03404309",
                "-12345678901",
                "+1234567890",
                "+123456789011"
        })
        void shouldSendWithIncorrectPhone(String phone) {
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(
                    getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY));
            $("[data-test-id=name] input").sendKeys("Иванов Иван");
            $("[data-test-id=agreement] input").parent().click();

            $("[data-test-id=phone] input").sendKeys(phone);
            $$("button").find(exactText("Забронировать")).click();
            assertTrue($("[data-test-id=phone].input_invalid").isDisplayed());
        }

        @Test
        void shouldSendWithIncorrectAgreement() {
            $("[data-test-id=city] input").sendKeys(getRandomCorrectCity());
            clearInputField($("[data-test-id=date] input"));
            $("[data-test-id=date] input").sendKeys(
                    getDateAfterCurrent(NUMBER_DAYS_TO_AVAILABLE_TO_ORDER_FROM_TODAY));
            $("[data-test-id=name] input").sendKeys("Иванов Иван");
            $("[data-test-id=phone] input").sendKeys("+12345678901");
            $$("button").find(exactText("Забронировать")).click();

            assertTrue($("[data-test-id=agreement].input_invalid").isDisplayed());
        }
    }
}
