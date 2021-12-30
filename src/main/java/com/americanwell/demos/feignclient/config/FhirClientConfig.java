package com.americanwell.demos.feignclient.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.americanwell.demos.feignclient.exception.TokenNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Configuration
public class FhirClientConfig {

    String GRANT_TYPE = "grant_type";
    String CLIENT_ID = "client_id";
    String CLIENT_SECRET = "client_secret";
    String CLIENT_CREDENTIALS = "client_credentials";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.INTERFACES)
    public IGenericClient client(Environment environment) {
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient(environment.getProperty("cdr.base-url"));

        client.registerInterceptor(new BearerTokenAuthInterceptor(tokenResolver(environment).getToken()));

        return client;
    }

    private FhirProxyTokenResolver tokenResolver(Environment environment) {
        return () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add(GRANT_TYPE, CLIENT_CREDENTIALS);
            formParams.add(CLIENT_ID, environment.getProperty("keycloak.client.id"));
            formParams.add(CLIENT_SECRET, environment.getProperty("keycloak.client.secret"));

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(formParams, headers);

            ResponseEntity<AccessTokenResponse> response = restTemplate().
                    postForEntity(environment.getProperty("token-url"), tokenRequest, AccessTokenResponse.class);

            String token = Optional.ofNullable(response.getBody()).map(AccessTokenResponse::getToken).
                    orElseThrow(() -> new TokenNotFoundException("Access token not found in the response."));

            log.info("access token: {}", token);

            return token;
        };
    }



    @Data
    private static class AccessTokenResponse {
        @JsonProperty("access_token")
        protected String token;
    }
}
