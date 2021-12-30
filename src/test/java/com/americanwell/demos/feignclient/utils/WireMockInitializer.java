package com.americanwell.demos.feignclient.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

public class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var wireMockServer = new WireMockServer(
                new WireMockConfiguration().dynamicPort()
                        .stubRequestLoggingDisabled(false)
        );
        wireMockServer.start();
        applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

        TestPropertyValues.of(
                "cdr.base-url=" + wireMockServer.baseUrl()
        ).applyTo(applicationContext.getEnvironment());

        TestPropertyValues.of(
                "token-url=" + wireMockServer.baseUrl() + "/auth/realms/services/protocol/openid-connect/token"
        ).applyTo(applicationContext.getEnvironment());

        applicationContext.addApplicationListener(applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent) {
                wireMockServer.stop();
            }
        });
    }
}
