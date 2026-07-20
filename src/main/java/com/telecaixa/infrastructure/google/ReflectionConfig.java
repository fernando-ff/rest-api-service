package com.telecaixa.infrastructure.google;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonError.ErrorInfo;

@RegisterForReflection(targets = {
    GoogleJsonError.class,
    ErrorInfo.class
})
public class ReflectionConfig {
}