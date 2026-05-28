package com.sdkanban.task.service.impl;

import com.sdkanban.board.repository.BoardColumnRepository;
import com.sdkanban.common.BusinessException;
import com.sdkanban.project.entity.ProjectMemberId;
import com.sdkanban.project.repository.ProjectMemberRepository;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.service.ProjectService;
import com.sdkanban.sprint.repository.SprintRepository;
import com.sdkanban.task.dto.AddTaskCommentRequest;
import com.sdkanban.task.dto.CreateTaskRequest;
import com.sdkanban.task.dto.CreateTaskTagRequest;
import com.sdkanban.task.dto.TaskCommentResponse;
import com.sdkanban.task.dto.TaskResponse;
import com.sdkanban.task.dto.TaskTagResponse;
import com.sdkanban.task.dto.UpdateTaskPositionRequest;
import com.sdkanban.task.dto.UpdateTaskRequest;
import com.sdkanban.task.dto.UpdateTaskTagsRequest;
import com.sdkanban.task.entity.Task;
import com.sdkanban.task.entity.TaskActivity;
import com.sdkanban.task.entity.TaskComment;
import com.sdkanban.task.entity.TaskTag;
import com.sdkanban.task.entity.TaskTagLink;
import com.sdkanban.task.repository.TaskActivityRepository;
import com.sdkanban.task.repository.TaskCommentRepository;
import com.sdkanban.task.repository.TaskRepository;
import com.sdkanban.task.repository.TaskTagLinkRepository;
import com.sdkanban.task.repository.TaskTagRepository;
import com.sdkanban.task.service.TaskService;
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class TaskServiceImpl implements TaskService {
    private static final String DEFAULT_TAG_COLOR = "#64748b";
    private static final Set<String> CLEARABLE_FIELDS = Set.of(
        "description",
        "storyPoints",
        "estimatedHours",
        "dueDate",
        "acceptanceCriteria",
        "assigneeId",
        "sprintId"
    );

    private final TaskRepository taskRepository;
    private final TaskTagRepository taskTagRepository;
    private final TaskTagLinkRepository taskTagLinkRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;

    public TaskServiceImpl(
        TaskRepository taskRepository,
        TaskTagRepository taskTagRepository,
        TaskTagLinkRepository taskTagLinkRepository,
        TaskCommentRepository taskCommentRepository,
        TaskActivityRepository taskActivityRepository,
        ProjectService projectService,
        ProjectMemberRepository projectMemberRepository,
        BoardColumnRepository boardColumnRepository,
        SprintRepository sprintRepository,
        UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskTagRepository = taskTagRepository;
        this.taskTagLinkRepository = taskTagLinkRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.taskActivityRepository = taskActivityRepository;
        this.projectService = projectService;
        this.projectMemberRepository = projectMemberRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        validateAssignee(projectId, request.assigneeId());
        validateColumn(projectId, request.columnId());
        validateSprint(projectId, request.sprintId());
        List<TaskTag> tags = validateTags(projectId, request.tagIds());

        Task task = taskRepository.saveAndFlush(new Task(
            projectId,
            request.sprintId(),
            request.columnId(),
            request.assigneeId(),
            currentUserId,
            requiredTitle(request.title()),
            normalizeText(request.description()),
            normalizeValue(request.taskType(), Task.DEFAULT_TYPE),
            normalizeValue(request.priority(), Task.DEFAULT_PRIORITY),
            request.storyPoints(),
            request.estimatedHours(),
            request.dueDate(),
            normalizeText(request.acceptanceCriteria()),
            taskRepository.maxSortOrderInColumn(projectId, request.columnId()) + 1
        ));
        replaceTags(task, tags);
        recordActivity(task, currentUserId, "TASK_CREATED", null, null, null);
        return toTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse detail(Long taskId, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        return toTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse update(Long taskId, UpdateTaskRequest request, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);

        applyClearFields(task, request, currentUserId);

        if (request.title() != null) {
            change(task, currentUserId, "title", task.getTitle(), requiredTitle(request.title()), task::changeTitle);
        }
        if (request.description() != null) {
            change(task, currentUserId, "description", task.getDescription(), normalizeText(request.description()), task::changeDescription);
        }
        if (request.taskType() != null) {
            change(task, currentUserId, "taskType", task.getTaskType(), normalizeValue(request.taskType(), Task.DEFAULT_TYPE), task::changeTaskType);
        }
        if (request.priority() != null) {
            change(task, currentUserId, "priority", task.getPriority(), normalizeValue(request.priority(), Task.DEFAULT_PRIORITY), task::changePriority);
        }
        if (request.storyPoints() != null) {
            change(task, currentUserId, "storyPoints", task.getStoryPoints(), request.storyPoints(), task::changeStoryPoints);
        }
        if (request.estimatedHours() != null) {
            change(task, currentUserId, "estimatedHours", task.getEstimatedHours(), request.estimatedHours(), task::changeEstimatedHours);
        }
        if (request.dueDate() != null) {
            change(task, currentUserId, "dueDate", task.getDueDate(), request.dueDate(), task::changeDueDate);
        }
        if (request.acceptanceCriteria() != null) {
            change(task, currentUserId, "acceptanceCriteria", task.getAcceptanceCriteria(), normalizeText(request.acceptanceCriteria()), task::changeAcceptanceCriteria);
        }
        if (request.assigneeId() != null) {
            validateAssignee(task.getProjectId(), request.assigneeId());
            change(task, currentUserId, "assigneeId", task.getAssigneeId(), request.assigneeId(), task::changeAssigneeId);
        }
        if (request.sprintId() != null) {
            validateSprint(task.getProjectId(), request.sprintId());
            change(task, currentUserId, "sprintId", task.getSprintId(), request.sprintId(), task::changeSprintId);
        }
        if (request.columnId() != null) {
            validateColumn(task.getProjectId(), request.columnId());
            change(task, currentUserId, "columnId", task.getColumnId(), request.columnId(), task::changeColumnId);
        }

        return toTaskResponse(task);
    }

    private void applyClearFields(Task task, UpdateTaskRequest request, Long currentUserId) {
        if (request.clearFields() == null || request.clearFields().isEmpty()) {
            return;
        }
        for (String field : request.clearFields()) {
            if (field == null || !CLEARABLE_FIELDS.contains(field)) {
                throw BusinessException.badRequest("TASK_CLEAR_FIELD_NOT_ALLOWED", "Task field cannot be cleared");
            }
            switch (field) {
                case "description" -> change(task, currentUserId, "description", task.getDescription(), null, task::changeDescription);
                case "storyPoints" -> change(task, currentUserId, "storyPoints", task.getStoryPoints(), null, task::changeStoryPoints);
                case "estimatedHours" -> change(task, currentUserId, "estimatedHours", task.getEstimatedHours(), null, task::changeEstimatedHours);
                case "dueDate" -> change(task, currentUserId, "dueDate", task.getDueDate(), null, task::changeDueDate);
                case "acceptanceCriteria" -> change(task, currentUserId, "acceptanceCriteria", task.getAcceptanceCriteria(), null, task::changeAcceptanceCriteria);
                case "assigneeId" -> change(task, currentUserId, "assigneeId", task.getAssigneeId(), null, task::changeAssigneeId);
                case "sprintId" -> change(task, currentUserId, "sprintId", task.getSprintId(), null, task::changeSprintId);
                default -> throw BusinessException.badRequest("TASK_CLEAR_FIELD_NOT_ALLOWED", "Task field cannot be cleared");
            }
        }
    }

    @Override
    @Transactional
    public TaskResponse updatePosition(Long taskId, UpdateTaskPositionRequest request, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        validateColumn(task.getProjectId(), request.columnId());
        change(task, currentUserId, "columnId", task.getColumnId(), request.columnId(), task::changeColumnId);
        change(task, currentUserId, "sortOrder", task.getSortOrder(), request.sortOrder(), task::changeSortOrder);
        return toTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskTagResponse createTag(Long projectId, CreateTaskTagRequest request, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        TaskTag tag = taskTagRepository.save(new TaskTag(
            projectId,
            requiredTagName(request.name()),
            normalizeColor(request.color())
        ));
        return TaskTagResponse.from(tag);
    }

    @Override
    @Transactional
    public TaskResponse updateTags(Long taskId, UpdateTaskTagsRequest request, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        List<TaskTag> tags = validateTags(task.getProjectId(), request.tagIds());
        replaceTags(task, tags);
        recordActivity(task, currentUserId, "TASK_TAGS_UPDATED", "tags", null, tagIds(tags).toString());
        return toTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskCommentResponse addComment(Long taskId, AddTaskCommentRequest request, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        TaskComment comment = taskCommentRepository.save(new TaskComment(
            taskId,
            currentUserId,
            requiredComment(request.content())
        ));
        recordActivity(task, currentUserId, "COMMENT_ADDED", null, null, null);
        return TaskCommentResponse.from(comment, userSummary(currentUserId));
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId)
            .filter(task -> !task.isDeleted())
            .orElseThrow(() -> BusinessException.notFound("TASK_NOT_FOUND", "Task not found"));
    }

    private void validateAssignee(Long projectId, Long assigneeId) {
        if (assigneeId != null && !projectMemberRepository.existsById(new ProjectMemberId(projectId, assigneeId))) {
            throw BusinessException.badRequest("TASK_ASSIGNEE_NOT_MEMBER", "Task assignee must be a project member");
        }
    }

    private void validateColumn(Long projectId, Long columnId) {
        if (columnId == null || boardColumnRepository.findByIdAndProjectId(columnId, projectId).isEmpty()) {
            throw BusinessException.notFound("BOARD_COLUMN_NOT_FOUND", "Board column not found");
        }
    }

    private void validateSprint(Long projectId, Long sprintId) {
        if (sprintId != null && sprintRepository.findByIdAndProjectId(sprintId, projectId).isEmpty()) {
            throw BusinessException.notFound("SPRINT_NOT_FOUND", "Sprint not found");
        }
    }

    private List<TaskTag> validateTags(Long projectId, Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueIds = new HashSet<>(tagIds);
        List<TaskTag> tags = taskTagRepository.findByProjectIdAndIdIn(projectId, uniqueIds);
        if (tags.size() != uniqueIds.size()) {
            throw BusinessException.badRequest("TASK_TAG_NOT_IN_PROJECT", "Task tags must belong to the project");
        }
        return tags;
    }

    private void replaceTags(Task task, List<TaskTag> tags) {
        taskTagLinkRepository.deleteByIdTaskIdAndIdProjectId(task.getId(), task.getProjectId());
        taskTagLinkRepository.saveAll(tags.stream()
            .map(tag -> new TaskTagLink(task.getId(), tag.getId(), task.getProjectId()))
            .toList());
    }

    private List<TaskTagResponse> tagsForTask(Task task) {
        List<Long> tagIds = taskTagLinkRepository.findByIdTaskIdAndIdProjectId(task.getId(), task.getProjectId()).stream()
            .map(link -> link.getId().getTagId())
            .toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }
        return taskTagRepository.findByProjectIdAndIdIn(task.getProjectId(), tagIds).stream()
            .map(TaskTagResponse::from)
            .toList();
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.from(
            task,
            task.getAssigneeId() == null ? null : userSummary(task.getAssigneeId()),
            userSummary(task.getCreatorId()),
            tagsForTask(task)
        );
    }

    private UserSummary userSummary(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
        return UserSummary.from(user);
    }

    private void recordActivity(Task task, Long actorId, String actionType, String fieldName, String oldValue, String newValue) {
        taskActivityRepository.save(new TaskActivity(
            task.getId(),
            task.getProjectId(),
            actorId,
            actionType,
            fieldName,
            oldValue,
            newValue
        ));
    }

    private <T> void change(Task task, Long actorId, String fieldName, T oldValue, T newValue, ValueWriter<T> writer) {
        if (!Objects.equals(oldValue, newValue)) {
            writer.write(newValue);
            recordActivity(task, actorId, "TASK_UPDATED", fieldName, valueOf(oldValue), valueOf(newValue));
        }
    }

    private String requiredTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw BusinessException.badRequest("TASK_TITLE_REQUIRED", "Task title is required");
        }
        return title.trim();
    }

    private String requiredTagName(String name) {
        if (!StringUtils.hasText(name)) {
            throw BusinessException.badRequest("TASK_TAG_NAME_REQUIRED", "Task tag name is required");
        }
        return name.trim();
    }

    private String requiredComment(String content) {
        if (!StringUtils.hasText(content)) {
            throw BusinessException.badRequest("TASK_COMMENT_REQUIRED", "Task comment is required");
        }
        return content.trim();
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private String normalizeValue(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeColor(String color) {
        if (!StringUtils.hasText(color)) {
            return DEFAULT_TAG_COLOR;
        }
        return color.trim();
    }

    private String valueOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        return String.valueOf(value);
    }

    private List<Long> tagIds(List<TaskTag> tags) {
        return tags.stream().map(TaskTag::getId).toList();
    }

    private interface ValueWriter<T> {
        void write(T value);
    }
}
