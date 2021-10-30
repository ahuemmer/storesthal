package com.github.ahuemmer.storesthal;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockSettings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Abstract test template, encapsulating the functionality to set up and utilize wiremock.
 */
@WireMockSettings()
abstract class AbstractJsonTemplateBasedTest {

    /**
     * The folder (within the test resources) where the json files are stored.
     */
    protected static final String JSON_RESOURCE_FOLDER="json";

    @InjectServer
    WireMockServer serverMock;

    @ConfigureWireMock
    Options options = wireMockConfig()
            .dynamicPort();
    //.notifier(new ConsoleNotifier(true));

    protected void configureServerMock(String url, String responseFileName, Map<String, String> additionalSubstitutions) throws IOException {

        Map<String, String> substitutions =  new HashMap<>();
        substitutions.put("port", String.valueOf(serverMock.port()));
        substitutions.put("self", "http://localhost:"+serverMock.port()+url);

        if (additionalSubstitutions!=null) {
            substitutions.putAll(additionalSubstitutions);
        }

        serverMock.addStubMapping(stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody(getJsonFileContent(responseFileName, substitutions)))));
    }

    protected void configureServerMock(String url, String responseFileName) throws IOException {
        configureServerMock(url,responseFileName,null);
    }

    protected static String createJsonHrefArray(String[] entries) {
        StringBuilder result = new StringBuilder("[");
        for(int i=0;i<entries.length;i++) {
            result.append("    {\"href\":\"").append(entries[i]).append("\"}");
            if (i!=entries.length-1) {
                result.append(",");
            }
        }
        result.append("]");
        return result.toString();
    }

    protected static String getJsonFileContent(String fileName, Map<String,String> substitutes) throws IOException {
        File file = new File(Objects.requireNonNull(GeneralStoresthalTest.class.getClassLoader().getResource(JSON_RESOURCE_FOLDER + "/" + fileName)).getFile());
        String fileContent = Files.readString(file.toPath());
        return StringSubstitutor.replace(fileContent,substitutes);
    }

}
