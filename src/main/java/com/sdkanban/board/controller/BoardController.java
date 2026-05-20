package com.sdkanban.board.controller;

import com.sdkanban.board.dto.BoardResponse;
import com.sdkanban.board.dto.MyTaskBoardResponse;
import com.sdkanban.board.service.BoardService;
import com.sdkanban.common.ApiResponse;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.user.entity.User;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class BoardController {
    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/projects/{projectId}/board")
    ApiResponse<BoardResponse> projectBoard(
        @PathVariable Long projectId,
        @RequestParam(required = false) Long sprintId,
        @RequestParam(required = false) Long assigneeId,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String keyword,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardService.projectBoard(
            projectId,
            sprintId,
            assigneeId,
            type,
            priority,
            keyword,
            currentUserId(user)
        ));
    }

    @GetMapping("/tasks/mine/board")
    ApiResponse<MyTaskBoardResponse> myTaskBoard(
        @RequestParam(required = false) String groupBy,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(boardService.myTaskBoard(groupBy, currentUserId(user)));
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
