package com.gostock.dto.response;

import com.gostock.dto.response.base.ApiResponse;
import java.time.LocalDateTime;

public record PagingResponse<T>(
        boolean success,
        String message,
        T data,
        Object error,
        LocalDateTime timestamp,
        PaginationMetadata pagination) implements ApiResponse<T> {

    public record PaginationMetadata(int page, int size, long total) {
    }

    public PagingResponse(T data, int page, int size, long total) {
        this(true, "Success", data, null,
                LocalDateTime.now(), new PaginationMetadata(page, size, total));
    }

    @Override
    public boolean getSuccess() {
        return success;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public Object getError() {
        return error;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
