package com.sdkanban.settings.dto;

import com.sdkanban.settings.entity.BoardColumnTemplate;

public record BoardColumnTemplateResponse(
    Long id,
    String templateKey,
    String nameZh,
    String nameEn,
    String displayName,
    String color,
    Integer sortOrder,
    Integer wipLimit,
    boolean isDone
) {
    public static BoardColumnTemplateResponse from(BoardColumnTemplate template) {
        return new BoardColumnTemplateResponse(
            template.getId(),
            template.getTemplateKey(),
            template.getNameZh(),
            template.getNameEn(),
            template.getDisplayName(),
            template.getColor(),
            template.getSortOrder(),
            template.getWipLimit(),
            template.isDone()
        );
    }
}
