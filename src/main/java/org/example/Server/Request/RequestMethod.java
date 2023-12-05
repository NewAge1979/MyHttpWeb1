package org.example.Server.Request;

import lombok.AccessLevel;
import lombok.Getter;

public enum RequestMethod {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE");

    @Getter(AccessLevel.PUBLIC)
    private final String methodName;

    RequestMethod(String methodName) {
        this.methodName = methodName;
    }
}
