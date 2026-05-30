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
import com.sdkanban.settings.repository.BoardColumnTemplateRepository;
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
    private static final String GROUP_BY_TEMPLATE = "template";

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardColumnTemplateRepository boardColumnTemplateRepository;
    private final TaskRepository taskRepository;
    private final TaskChecklistItemRepository taskChecklistItemRepository;
    private final UserRepository userRepository;

    public BoardServiceImpl(
        ProjectService projectService,
        ProjectRepository projectRepository,
        BoardColumnRepository boardColumnRepository,
        BoardColumnTemplateRepository boardColumnTemplateRepository,
        TaskRepository taskRepository,
        TaskChecklistItemRepository taskChecklistItemRepository,
        UserRepository userRepository
    ) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.boardColumnTemplateRepository = boardColumnTemplateRepository;
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
        List<BoardColumn> boardColumns = boardColumnRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId);
        Map<Long, List<Task>> tasksByColumn = tasks.stream()
            .collect(Collectors.groupingBy(
                Task::getColumnId,
                LinkedHashMap::new,
                Collectors.toList()
            ));
        CardContext cardContext = cardContext(tasks);

        List<BoardColumnTasks> columns = boardColumns.stream()
            .map(column -> new BoardColumnTasks(
                column.getId(),
                column.getTemplateKey(),
                column.getName(),
                column.getColor(),
                column.getSortOrder(),
                column.isDone(),
                cards(tasksByColumn.getOrDefault(column.getId(), List.of()), cardContext)
            ))
            .toList();
        return new BoardResponse(projectId, columns);
    }

    @Override
    @Transactional(readOnly = true)
    public MyTaskBoardResponse myTaskBoard(String groupBy, Long currentUserId) {
        List<Task> tasks = taskRepository.findByAssigneeIdAndDeletedFalseAndArchivedFalseOrderByProjectIdAscColumnIdAscSortOrderAscIdAsc(currentUserId);
        CardContext cardContext = cardContext(tasks);
        return new MyTaskBoardResponse(GROUP_BY_TEMPLATE, groupByTemplate(tasks, cardContext));
    }

    private List<MyTaskBoardGroup> groupByTemplate(List<Task> tasks, CardContext cardContext) {
        Map<String, List<Task>> tasksByTemplate = tasks.stream()
            .filter(task -> cardContext.columnsById().containsKey(task.getColumnId()))
            .collect(Collectors.groupingBy(
                task -> cardContext.columnsById().get(task.getColumnId()).getTemplateKey(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        return boardColumnTemplateRepository.findByOrderBySortOrderAscIdAsc()
            .stream()
            .map(template -> new MyTaskBoardGroup(
                template.getTemplateKey(),
                template.getDisplayName(),
                template.getColor(),
                template.getSortOrder(),
                template.isDone(),
                cards(tasksByTemplate.getOrDefault(template.getTemplateKey(), List.of()), cardContext)
            ))
            .toList();
    }

    private CardContext cardContext(List<Task> tasks) {
        Map<Long, UserSummary> usersById = userRepository.findAllById(tasks.stream()
                .map(Task::getAssigneeId)
                .filter(Objects::nonNull)
                .toList())
            .stream()
            .collect(Collectors.toMap(User::getId, UserSummary::from));
        Map<Long, Project> projectsById = projectRepository.findAllById(tasks.stream()
                .map(Task::getProjectId)
                .distinct()
                .toList())
            .stream()
            .collect(Collectors.toMap(Project::getId, Function.identity()));
        Map<Long, BoardColumn> columnsById = boardColumnRepository.findAllById(tasks.stream()
                .map(Task::getColumnId)
                .distinct()
                .toList())
            .stream()
            .collect(Collectors.toMap(BoardColumn::getId, Function.identity()));
        Map<Long, TaskChecklistItemRepository.ChecklistCountView> checklistCounts = tasks.isEmpty()
            ? Map.of()
            : taskChecklistItemRepository.countByTaskIds(tasks.stream().map(Task::getId).toList())
                .stream()
                .collect(Collectors.toMap(TaskChecklistItemRepository.ChecklistCountView::getTaskId, Function.identity()));
        return new CardContext(usersById, projectsById, columnsById, checklistCounts);
    }

    private List<TaskCardResponse> cards(List<Task> tasks, CardContext cardContext) {
        return tasks.stream()
            .map(task -> {
                TaskChecklistItemRepository.ChecklistCountView count = cardContext.checklistCounts().get(task.getId());
                long doneCount = count == null ? 0 : count.getDoneCount();
                long totalCount = count == null ? 0 : count.getTotalCount();
                return TaskCardResponse.from(
                    task,
                    cardContext.projectsById().get(task.getProjectId()),
                    cardContext.columnsById().get(task.getColumnId()),
                    cardContext.usersById().get(task.getAssigneeId()),
                    doneCount,
                    totalCount
                );
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

    private record CardContext(
        Map<Long, UserSummary> usersById,
        Map<Long, Project> projectsById,
        Map<Long, BoardColumn> columnsById,
        Map<Long, TaskChecklistItemRepository.ChecklistCountView> checklistCounts
    ) {
    }
}
