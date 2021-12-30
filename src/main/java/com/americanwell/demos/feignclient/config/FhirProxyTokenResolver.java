/*
 * Copyright 2021 Amwell
 * All rights reserved.
 *
 * It is illegal to use, reproduce or distribute
 * any part of this Intellectual Property without
 * prior written authorization from Amwell.
 */

package com.americanwell.demos.feignclient.config;

public interface FhirProxyTokenResolver {
    String getToken();
}
