package com.sdkanban.settings.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderBoardColumnTemplatesRequest(
    @NotEmpty
    List<String> templateKeys
) {
}
