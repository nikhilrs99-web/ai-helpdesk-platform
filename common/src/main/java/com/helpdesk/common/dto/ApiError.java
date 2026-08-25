package com.helpdesk.common.dto;

import java.time.Instant;

/**
 * Consistent error shape across every service, so a client only needs to learn one
 * error format regardless of which service handled (or rejected) the request.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
