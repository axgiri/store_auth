package com.github.storeauth.exception;

import java.time.Instant;

record ExceptionApi(Instant timestamp, String code, String message) {}
