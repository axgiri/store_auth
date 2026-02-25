package com.github.storeauth.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record NotificationMessage(
    @NotNull(message = "recipient cannot be null")
    String recipient,

    @NotNull(message = "text cannot be null")
    String text,

    @JsonProperty("is_html")
    boolean isHtml,

    String subject
) implements Serializable {}
