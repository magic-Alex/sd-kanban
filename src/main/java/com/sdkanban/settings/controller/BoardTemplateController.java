package com.sdkanban.settings.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.settings.dto.BoardColumnTemplateResponse;
import com.sdkanban.settings.dto.CreateBoardColumnTemplateRequest;
import com.sdkanban.settings.dto.ReorderBoardColumnTemplatesRequest;
import com.sdkanban.settings.dto.UpdateBoardColumnTemplateRequest;
import com.sdkanban.settings.service.BoardTemplateService;
import com.sdkanban.user.entity.User;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/board-templates")
public class BoardTemplateController {
    private final BoardTemplateService boardTemplateService;

    public BoardTemplateController(BoardTemplateService boardTemplateService) {
        this.boardTemplateService = boardTemplateService;
    }

    @GetMapping
    ApiResponse<List<BoardColumnTemplateResponse>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(boardTemplateService.list(currentUserId(user)));
    }

    @PostMapping
    ApiResponse<BoardColumnTemplateResponse> create(
        @Valid @RequestBody CreateBoardColumnTemplateRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardTemplateService.create(request, currentUserId(user)));
    }

    @PatchMapping("/{templateKey}")
    ApiResponse<BoardColumnTemplateResponse> update(
        @PathVariable String templateKey,
        @Valid @RequestBody UpdateBoardColumnTemplateRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardTemplateService.update(templateKey, request, currentUserId(user)));
    }

    @PatchMapping("/reorder")
    ApiResponse<List<BoardColumnTemplateResponse>> reorder(
        @Valid @RequestBody ReorderBoardColumnTemplatesRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardTemplateService.reorder(request, currentUserId(user)));
    }

    @DeleteMapping("/{templateKey}")
    ApiResponse<Void> delete(
        @PathVariable String templateKey,
        @AuthenticationPrincipal User user
    ) {
        boardTemplateService.delete(templateKey, currentUserId(user));
        return ApiResponse.ok(null);
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
