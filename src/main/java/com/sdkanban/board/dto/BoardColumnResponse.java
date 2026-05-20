package com.sdkanban.board.dto;

import com.sdkanban.board.entity.BoardColumn;

import java.time.LocalDateTime;

public record BoardColumnResponse(
    Long id,
    Long projectId,
    String name,
    String color,
    Integer sortOrder,
    Integer wipLimit,
    boolean isDone,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static BoardColumnResponse from(BoardColumn column) {
        return new BoardColumnResponse(
            column.getId(),
            column.getProjectId(),
            column.getName(),
            column.getColor(),
            column.getSortOrder(),
            column.getWipLimit(),
            column.isDone(),
            column.getCreatedAt(),
            column.getUpdatedAt()
        );
    }
}
