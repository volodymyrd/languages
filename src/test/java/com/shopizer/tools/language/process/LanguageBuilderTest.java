package com.shopizer.tools.language.process;

import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.model.TranslateTextResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

import static com.shopizer.tools.language.process.LanguageBuilder.MAPPER;
import static com.shopizer.tools.language.process.LanguageBuilder.buildTranslateTextRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link LanguageBuilder}.
 */
@ExtendWith(MockitoExtension.class)
class LanguageBuilderTest {

    private static final String TARGET_LANG = "uk";

    @Mock
    private AmazonTranslate translate;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testTranslatePropsFile() throws IOException {
        String message1 = "Запит виконано успішно";
        String message2 = "У цьому запиті сталася помилка";
        TranslateTextResult result1 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("Request completed with success", TARGET_LANG)))
                .thenReturn(result1);
        when(result1.getTranslatedText()).thenReturn(message1);

        TranslateTextResult result2 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("An error occurred in this request", TARGET_LANG)))
                .thenReturn(result2);
        when(result2.getTranslatedText()).thenReturn(message2);

        new LanguageBuilder(translate, TARGET_LANG, "languages/src/test/resources/messages.properties")
                .process();

        File file = new File("languages/src/test/resources/messages_uk.properties");
        assertThat(file).exists();
        try (InputStreamReader inputStreamReader =
                     new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(inputStreamReader);
            assertThat(properties).hasSize(2);
            assertThat(properties.getProperty("message.success")).isEqualTo(message1);
            assertThat(properties.getProperty("message.error")).isEqualTo(message2);
        }
        assertThat(file.delete()).isTrue();
    }

    @Test
    void testTranslateFlatJsonFile() throws IOException {
        String message1 = "Умови та політика";
        String message2 = "Цей веб-сайт використовує файли cookie лише для покращення взаємодії з користувачем.";
        TranslateTextResult result1 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("Terms and Policy", TARGET_LANG)))
                .thenReturn(result1);
        when(result1.getTranslatedText()).thenReturn(message1);

        TranslateTextResult result2 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest(
                "This website uses cookies only to enhance the user experience.", TARGET_LANG)))
                .thenReturn(result2);
        when(result2.getTranslatedText()).thenReturn(message2);

        new LanguageBuilder(translate, TARGET_LANG, "languages/src/test/resources/english.json")
                .process();

        File file = new File("languages/src/test/resources/english_uk.json");
        assertThat(file).exists();
        @SuppressWarnings("unchecked")
        Map<String, String> map = MAPPER.readValue(new File(file.toString()), Map.class);
        assertThat(map)
                .hasSize(2)
                .containsEntry("Terms and Policy", message1)
                .containsEntry("Cookie Consent", message2);
        assertThat(file.delete()).isTrue();
    }

    @Test
    void testTranslateNestedJsonFile() throws IOException {
        String message1 = "Грошовий переказ";
        String message2 = "PayPal експрес-оплата";
        String message3 = "Профіль";
        String message4 = "Вийти";
        String message5 = "Інформація про магазин";
        String message6 = "Останній доступ";
        TranslateTextResult result1 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("Money Order", TARGET_LANG)))
                .thenReturn(result1);
        when(result1.getTranslatedText()).thenReturn(message1);

        TranslateTextResult result2 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("PayPal Express Checkout", TARGET_LANG)))
                .thenReturn(result2);
        when(result2.getTranslatedText()).thenReturn(message2);

        TranslateTextResult result3 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("Profile", TARGET_LANG)))
                .thenReturn(result3);
        when(result3.getTranslatedText()).thenReturn(message3);

        TranslateTextResult result4 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("Log out", TARGET_LANG)))
                .thenReturn(result4);
        when(result4.getTranslatedText()).thenReturn(message4);

        TranslateTextResult result5 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("Store information", TARGET_LANG)))
                .thenReturn(result5);
        when(result5.getTranslatedText()).thenReturn(message5);

        TranslateTextResult result6 = mock(TranslateTextResult.class);
        when(translate.translateText(buildTranslateTextRequest("Last access", TARGET_LANG)))
                .thenReturn(result6);
        when(result6.getTranslatedText()).thenReturn(message6);

        new LanguageBuilder(translate, TARGET_LANG, "languages/src/test/resources/en.json")
                .process();

        File file = new File("languages/src/test/resources/en_uk.json");
        assertThat(file).exists();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = MAPPER.readValue(new File(file.toString()), Map.class);
        assertThat(map)
                .hasSize(4)
                .containsEntry("moneyorder", message1)
                .containsEntry("paypal-express-checkout", message2)
                .containsEntry("HEADER", Map.of("PROFILE", message3, "LOGOUT", message4))
                .containsEntry("HOME", Map.of("STORE_INFORMATION", message5, "LAST_ACCESS", message6));
        assertThat(file.delete()).isTrue();
    }
}
