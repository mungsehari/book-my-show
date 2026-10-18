package com.bookmyshow.common.response;

import java.util.List;

/** Common shape for list endpoints. */
public record ListResponse<T>(List<T> items, int count) {

    public static <T> ListResponse<T> of(List<T> items) {
        return new ListResponse<>(items, items.size());
    }
}
