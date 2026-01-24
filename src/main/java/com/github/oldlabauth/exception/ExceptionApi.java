package com.github.oldlabauth.exception;

import java.time.Instant;

record ExceptionApi(Instant timestamp, String code, String message) {}
