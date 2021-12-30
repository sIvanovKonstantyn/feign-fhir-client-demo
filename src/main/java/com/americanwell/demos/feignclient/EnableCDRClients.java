package com.americanwell.demos.feignclient;

import com.americanwell.demos.feignclient.config.FhirClientConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({CDRClientRegistrar.class, FhirClientConfig.class})
@Documented
public @interface EnableCDRClients {
}
