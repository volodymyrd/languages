package com.shopizer.tools.language.process;

import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class LanguageBuilder {

    static final JsonMapper MAPPER = new JsonMapper();

    private static final String SOURCE_LANGUAGE_CODE = "en";

    private final AmazonTranslate translate;
    private final String targetLanguage;
    private final Path pathToFile;

    public LanguageBuilder(AmazonTranslate translate, String targetLanguage, String fullPath) throws IOException {
        this.translate = translate;
        pathToFile = Paths.get(fullPath);
        if (!Files.exists(pathToFile)) {
            throw new IOException("Path [" + pathToFile + "] does not exists");
        }
        this.targetLanguage = targetLanguage;
        long count = Arrays.stream(Locale.getISOLanguages()).filter(targetLanguage::equals).count();

        if (count == 0) {
            throw new IOException("Language with iso-code [" + targetLanguage + "]");
        }
    }

    public void process() throws IOException {
        String fileName = pathToFile.getFileName().toString();
        if (fileName.endsWith(".properties")) {
            processPropsFile();
        } else if (fileName.endsWith(".json")) {
            processJsonFile();
        } else {
            throw new UnsupportedOperationException("Unsupported file " + pathToFile.getFileName());
        }
    }

    private void processPropsFile() throws IOException {
        try (InputStream inputStream = Files.newInputStream(Paths.get(pathToFile.toString()))) {
            Properties properties = new Properties();
            properties.load(inputStream);
            Map<String, String> keyValue = properties.entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(),
                            e -> e.getValue().toString()));
            Map<String, Object> results =
                    keyValue.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> mapTranslation(e.getValue())
                            ));
            generateTranslationPropsFile(results);
        }
    }

    private void processJsonFile() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = MAPPER.readValue(new File(pathToFile.toString()), Map.class);
        Map<String, Object> results = map.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        e -> mapTranslation(e.getValue()),
                        (u, v) -> u,
                        LinkedHashMap::new));
        generateTranslationJsonFile(results);
    }

    @SuppressWarnings("unchecked")
    private Object mapTranslation(Object object) {
        if (object instanceof String) {
            return translate.translateText(buildTranslateTextRequest((String) object, targetLanguage))
                    .getTranslatedText();
        }
        return ((Map<String, Object>) object).entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        e -> mapTranslation(e.getValue()),
                        (u, v) -> u,
                        LinkedHashMap::new));
    }

    static TranslateTextRequest buildTranslateTextRequest(String label, String targetLanguage) {
        return new TranslateTextRequest()
                .withText(label)
                .withSourceLanguageCode(SOURCE_LANGUAGE_CODE)
                .withTargetLanguageCode(targetLanguage);
    }

    private void generateTranslationPropsFile(Map<String, Object> results) throws IOException {
        Files.write
                (buildNewFilePath(".properties"), () -> results.entrySet().stream()
                        .<CharSequence>map(e -> e.getKey() + "=" + e.getValue())
                        .iterator());
    }

    private void generateTranslationJsonFile(Map<String, Object> results) throws IOException {
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(buildNewFilePath(".json").toFile(), results);
    }

    private Path buildNewFilePath(String ext) {
        return Paths.get(getDir() + File.separator + getFileNameWithoutExt() + "_" + targetLanguage + ext);
    }

    private Path getDir() {
        return pathToFile.getParent();
    }

    private String getFileNameWithoutExt() {
        return pathToFile.getFileName().toString().replaceFirst("[.][^.]+$", "");
    }
}
