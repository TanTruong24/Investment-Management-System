package com.gostock.dto.response.base;

import java.time.LocalDateTime;

public interface ApiResponse<T> {

    boolean getSuccess();
    String getMessage();
    T getData();
    Object getError();
    LocalDateTime getTimestamp();

}
