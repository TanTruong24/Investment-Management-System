package com.gostock.dto.response;

import com.gostock.dto.response.base.ApiResponse;
import java.time.LocalDateTime;

public record SuccessResponse<T>(
    boolean success,
    String message,
    T data,
    Object error,
    LocalDateTime timestamp
) implements ApiResponse<T> {
    
    // Constructor tiện ích: chỉ cần truyền data
    public SuccessResponse(T data) {
        this(true, "Success", data, null, LocalDateTime.now());
    }

    @Override public boolean getSuccess() { return success; }
    @Override public String getMessage() { return message; }
    @Override public T getData() { return data; }
    @Override public Object getError() { return error; }
    @Override public LocalDateTime getTimestamp() { return timestamp; }
}
