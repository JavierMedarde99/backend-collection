package com.wikicollection.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;

public record ProgressUpdateRequest(
        @Min(value = 0, message = "Las páginas leídas no pueden ser negativas") Integer pagesRead) {
}
