package com.sdkanban.board.controller;

import com.sdkanban.board.dto.BoardColumnResponse;
import com.sdkanban.board.dto.CreateBoardColumnRequest;
import com.sdkanban.board.dto.ReorderBoardColumnsRequest;
import com.sdkanban.board.dto.UpdateBoardColumnRequest;
import com.sdkanban.board.service.BoardColumnService;
import com.sdkanban.common.ApiResponse;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/columns")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class BoardColumnController {
    private final BoardColumnService boardColumnService;

    public BoardColumnController(BoardColumnService boardColumnService) {
        this.boardColumnService = boardColumnService;
    }

    @GetMapping
    ApiResponse<List<BoardColumnResponse>> list(
        @PathVariable Long projectId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardColumnService.list(projectId, currentUserId(user)));
    }

    @PostMapping
    ApiResponse<BoardColumnResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateBoardColumnRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardColumnService.create(projectId, request, currentUserId(user)));
    }

    @PatchMapping("/{columnId}")
    ApiResponse<BoardColumnResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long columnId,
        @Valid @RequestBody UpdateBoardColumnRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardColumnService.update(projectId, columnId, request, currentUserId(user)));
    }

    @PatchMapping("/reorder")
    ApiResponse<List<BoardColumnResponse>> reorder(
        @PathVariable Long projectId,
        @Valid @RequestBody ReorderBoardColumnsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardColumnService.reorder(projectId, request, currentUserId(user)));
    }

    @DeleteMapping("/{columnId}")
    ApiResponse<Void> delete(
        @PathVariable Long projectId,
        @PathVariable Long columnId,
        @AuthenticationPrincipal User user
    ) {
        boardColumnService.delete(projectId, columnId, currentUserId(user));
        return ApiResponse.ok(null);
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
