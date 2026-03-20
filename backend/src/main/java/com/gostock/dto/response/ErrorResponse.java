package com.gostock.dto.response;

import com.gostock.dto.response.base.ApiResponse;
import java.time.LocalDateTime;

public record ErrorResponse(
    boolean success,
    String message,
    Object error,
    LocalDateTime timestamp
) implements ApiResponse<Void> {
    
    public ErrorResponse(String message, Object errorDetails) {
        this(false, message, errorDetails, LocalDateTime.now());
    }

    @Override public boolean getSuccess() { return success; }
    @Override public String getMessage() { return message; }
    @Override public Void getData() { return null; }
    @Override public Object getError() { return error; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
}
