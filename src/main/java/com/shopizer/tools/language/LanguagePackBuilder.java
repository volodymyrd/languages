package com.shopizer.tools.language;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.shopizer.tools.language.process.LanguageBuilder;

import java.io.IOException;


/**
 * Lang pack generator
 *
 * @author carlsamson
 */
public class LanguagePackBuilder {
    private static final String TARGET_ISO_LANGUAGE = "uk";
    private static final String ACCESS_KEY = "";
    private static final String SECRET_KEY = "";
    private static final String FILE = "";

    public static void main(String[] args) throws IOException {
        AmazonTranslate translate = AmazonTranslateClient.builder()
                .withCredentials(new
                        AWSStaticCredentialsProvider(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)))
                .withRegion("us-east-1")
                .build();
        new LanguageBuilder(translate, TARGET_ISO_LANGUAGE, FILE).process();
        printInstructions();
    }

    private static void printInstructions() {
        System.out.println("**************************************" + "\r\n" +
                "Next steps:" +
                "- Run this query in Shopizer database" + "\r\n" +
                "insert into SALESMANAGER.LANGUAGE('LANGUAGE_ID','DATE_CREATED','DATE_MODIFIED','CODE') values (select SEQ_COUNT from SM_SEQUENCER where SEQ_NAME='LANG_SEQ_NEXT_VAL', CURDATE(), CURDATE(), '" +
                TARGET_ISO_LANGUAGE +
                "')" +
                "\r\n" +
                "\r\n" + "\r\n");
    }
}
