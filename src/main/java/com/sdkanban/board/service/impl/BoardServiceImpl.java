package com.sdkanban.board.service.impl;

import com.sdkanban.board.dto.BoardColumnTasks;
import com.sdkanban.board.dto.BoardResponse;
import com.sdkanban.board.dto.MyTaskBoardGroup;
import com.sdkanban.board.dto.MyTaskBoardResponse;
import com.sdkanban.board.dto.TaskCardResponse;
import com.sdkanban.board.entity.BoardColumn;
import com.sdkanban.board.repository.BoardColumnRepository;
import com.sdkanban.board.service.BoardService;
import com.sdkanban.project.entity.Project;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.repository.ProjectRepository;
import com.sdkanban.project.service.ProjectService;
import com.sdkanban.task.entity.Task;
import com.sdkanban.task.repository.TaskChecklistItemRepository;
import com.sdkanban.task.repository.TaskRepository;
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class BoardServiceImpl implements BoardService {
    private static final String GROUP_BY_COLUMN = "column";
    private static final String GROUP_BY_PROJECT = "project";

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;
    private final TaskChecklistItemRepository taskChecklistItemRepository;
    private final UserRepository userRepository;

    public BoardServiceImpl(
        ProjectService projectService,
        ProjectRepository projectRepository,
        BoardColumnRepository boardColumnRepository,
        TaskRepository taskRepository,
        TaskChecklistItemRepository taskChecklistItemRepository,
        UserRepository userRepository
    ) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.taskRepository = taskRepository;
        this.taskChecklistItemRepository = taskChecklistItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BoardResponse projectBoard(
        Long projectId,
        Long sprintId,
        Long assigneeId,
        String type,
        String priority,
        String keyword,
        Long currentUserId
    ) {
        projectService.requireMember(projectId, currentUserId);
        List<Task> tasks = taskRepository.findProjectBoardTasks(
            projectId,
            sprintId,
            assigneeId,
            normalize(type),
            normalize(priority),
            keyword(keyword)
        );
        Map<Long, List<Task>> tasksByColumn = tasks.stream()
            .collect(Collectors.groupingBy(
                Task::getColumnId,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<BoardColumnTasks> columns = boardColumnRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
            .map(column -> new BoardColumnTasks(
                column.getId(),
                column.getName(),
                column.getColor(),
                column.getSortOrder(),
                column.isDone(),
                cards(tasksByColumn.getOrDefault(column.getId(), List.of()))
            ))
            .toList();
        return new BoardResponse(projectId, columns);
    }

    @Override
    @Transactional(readOnly = true)
    public MyTaskBoardResponse myTaskBoard(String groupBy, Long currentUserId) {
        String normalizedGroupBy = GROUP_BY_COLUMN.equalsIgnoreCase(String.valueOf(groupBy))
            ? GROUP_BY_COLUMN
            : GROUP_BY_PROJECT;
        List<Task> tasks = taskRepository.findByAssigneeIdAndDeletedFalseAndArchivedFalseOrderByProjectIdAscColumnIdAscSortOrderAscIdAsc(currentUserId);
        if (GROUP_BY_COLUMN.equals(normalizedGroupBy)) {
            return new MyTaskBoardResponse(normalizedGroupBy, groupByColumn(tasks));
        }
        return new MyTaskBoardResponse(normalizedGroupBy, groupByProject(tasks));
    }

    private List<MyTaskBoardGroup> groupByProject(List<Task> tasks) {
        Map<Long, Project> projectsById = projectRepository.findAllById(tasks.stream().map(Task::getProjectId).toList())
            .stream()
            .collect(Collectors.toMap(Project::getId, Function.identity()));

        return tasks.stream()
            .collect(Collectors.groupingBy(Task::getProjectId, LinkedHashMap::new, Collectors.toList()))
            .entrySet()
            .stream()
            .map(entry -> new MyTaskBoardGroup(
                entry.getKey(),
                projectsById.get(entry.getKey()).getName(),
                cards(entry.getValue())
            ))
            .toList();
    }

    private List<MyTaskBoardGroup> groupByColumn(List<Task> tasks) {
        Map<Long, BoardColumn> columnsById = boardColumnRepository.findAllById(tasks.stream().map(Task::getColumnId).toList())
            .stream()
            .collect(Collectors.toMap(BoardColumn::getId, Function.identity()));

        return tasks.stream()
            .collect(Collectors.groupingBy(Task::getColumnId, LinkedHashMap::new, Collectors.toList()))
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(entry -> columnsById.get(entry.getKey()).getSortOrder()))
            .map(entry -> new MyTaskBoardGroup(
                entry.getKey(),
                columnsById.get(entry.getKey()).getName(),
                cards(entry.getValue())
            ))
            .toList();
    }

    private List<TaskCardResponse> cards(List<Task> tasks) {
        Map<Long, UserSummary> usersById = userRepository.findAllById(tasks.stream()
                .map(Task::getAssigneeId)
                .filter(Objects::nonNull)
                .toList())
            .stream()
            .collect(Collectors.toMap(User::getId, UserSummary::from));
        Map<Long, TaskChecklistItemRepository.ChecklistCountView> checklistCounts = tasks.isEmpty()
            ? Map.of()
            : taskChecklistItemRepository.countByTaskIds(tasks.stream().map(Task::getId).toList())
                .stream()
                .collect(Collectors.toMap(TaskChecklistItemRepository.ChecklistCountView::getTaskId, Function.identity()));

        return tasks.stream()
            .map(task -> {
                TaskChecklistItemRepository.ChecklistCountView count = checklistCounts.get(task.getId());
                long doneCount = count == null ? 0 : count.getDoneCount();
                long totalCount = count == null ? 0 : count.getTotalCount();
                return TaskCardResponse.from(task, usersById.get(task.getAssigneeId()), doneCount, totalCount);
            })
            .toList();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }
}
