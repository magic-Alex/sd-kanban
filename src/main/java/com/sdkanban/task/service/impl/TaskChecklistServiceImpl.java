package com.sdkanban.task.service.impl;

import com.sdkanban.common.BusinessException;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.service.ProjectService;
import com.sdkanban.task.dto.CreateTaskChecklistItemRequest;
import com.sdkanban.task.dto.ReorderTaskChecklistItemsRequest;
import com.sdkanban.task.dto.TaskChecklistItemResponse;
import com.sdkanban.task.dto.UpdateTaskChecklistItemRequest;
import com.sdkanban.task.entity.Task;
import com.sdkanban.task.entity.TaskActivity;
import com.sdkanban.task.entity.TaskChecklistItem;
import com.sdkanban.task.repository.TaskActivityRepository;
import com.sdkanban.task.repository.TaskChecklistItemRepository;
import com.sdkanban.task.repository.TaskRepository;
import com.sdkanban.task.service.TaskChecklistService;
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class TaskChecklistServiceImpl implements TaskChecklistService {
    private static final String REORDER_INVALID_MESSAGE = "Checklist reorder payload must contain each task checklist item once";

    private final TaskRepository taskRepository;
    private final TaskChecklistItemRepository taskChecklistItemRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    public TaskChecklistServiceImpl(
        TaskRepository taskRepository,
        TaskChecklistItemRepository taskChecklistItemRepository,
        TaskActivityRepository taskActivityRepository,
        ProjectService projectService,
        UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskChecklistItemRepository = taskChecklistItemRepository;
        this.taskActivityRepository = taskActivityRepository;
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskChecklistItemResponse> list(Long taskId, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        return checklistItems(task).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public TaskChecklistItemResponse create(Long taskId, CreateTaskChecklistItemRequest request, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);

        TaskChecklistItem item = taskChecklistItemRepository.save(new TaskChecklistItem(
            task.getId(),
            task.getProjectId(),
            requiredTitle(request.title()),
            taskChecklistItemRepository.maxSortOrder(task.getId(), task.getProjectId()) + 1,
            currentUserId
        ));
        recordActivity(task, currentUserId, "CHECKLIST_ITEM_CREATED", "checklist", null, item.getTitle());
        return toResponse(item);
    }

    @Override
    @Transactional
    public TaskChecklistItemResponse update(
        Long taskId,
        Long itemId,
        UpdateTaskChecklistItemRequest request,
        Long currentUserId
    ) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        TaskChecklistItem item = requireItem(task, itemId);
        String oldTitle = item.getTitle();
        String newTitle = requiredTitle(request.title());

        if (!Objects.equals(oldTitle, newTitle)) {
            item.rename(newTitle);
            recordActivity(task, currentUserId, "CHECKLIST_ITEM_UPDATED", "title", oldTitle, newTitle);
        }
        return toResponse(item);
    }

    @Override
    @Transactional
    public TaskChecklistItemResponse toggle(Long taskId, Long itemId, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        TaskChecklistItem item = requireItem(task, itemId);

        if (item.isDone()) {
            item.markOpen();
            recordActivity(task, currentUserId, "CHECKLIST_ITEM_REOPENED", "done", "true", "false");
        } else {
            item.markDone(currentUserId);
            recordActivity(task, currentUserId, "CHECKLIST_ITEM_COMPLETED", "done", "false", "true");
        }
        return toResponse(item);
    }

    @Override
    @Transactional
    public List<TaskChecklistItemResponse> reorder(
        Long taskId,
        ReorderTaskChecklistItemsRequest request,
        Long currentUserId
    ) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        List<TaskChecklistItem> items = checklistItems(task);
        validateReorder(items, request.itemIds());

        Map<Long, TaskChecklistItem> itemsById = items.stream()
            .collect(Collectors.toMap(TaskChecklistItem::getId, Function.identity()));
        for (int sortOrder = 0; sortOrder < request.itemIds().size(); sortOrder++) {
            itemsById.get(request.itemIds().get(sortOrder)).changeSortOrder(sortOrder);
        }
        recordActivity(task, currentUserId, "CHECKLIST_ITEMS_REORDERED", "sortOrder", null, request.itemIds().toString());
        return checklistItems(task).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long taskId, Long itemId, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        TaskChecklistItem item = requireItem(task, itemId);
        String oldTitle = item.getTitle();

        taskChecklistItemRepository.delete(item);
        recordActivity(task, currentUserId, "CHECKLIST_ITEM_DELETED", "checklist", oldTitle, null);
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId)
            .filter(task -> !task.isDeleted())
            .orElseThrow(() -> BusinessException.notFound("TASK_NOT_FOUND", "Task not found"));
    }

    private TaskChecklistItem requireItem(Task task, Long itemId) {
        return taskChecklistItemRepository.findByIdAndTaskIdAndProjectId(itemId, task.getId(), task.getProjectId())
            .orElseThrow(() -> BusinessException.notFound("CHECKLIST_ITEM_NOT_FOUND", "Checklist item not found"));
    }

    private List<TaskChecklistItem> checklistItems(Task task) {
        return taskChecklistItemRepository.findByTaskIdAndProjectIdOrderBySortOrderAscIdAsc(task.getId(), task.getProjectId());
    }

    private void validateReorder(List<TaskChecklistItem> items, List<Long> itemIds) {
        if (itemIds == null || itemIds.size() != items.size()) {
            throw invalidReorder();
        }
        Set<Long> expectedIds = items.stream()
            .map(TaskChecklistItem::getId)
            .collect(Collectors.toSet());
        Set<Long> seenIds = new HashSet<>();
        for (Long itemId : itemIds) {
            if (itemId == null || !expectedIds.contains(itemId) || !seenIds.add(itemId)) {
                throw invalidReorder();
            }
        }
    }

    private BusinessException invalidReorder() {
        return BusinessException.badRequest("CHECKLIST_REORDER_INVALID", REORDER_INVALID_MESSAGE);
    }

    private TaskChecklistItemResponse toResponse(TaskChecklistItem item) {
        return TaskChecklistItemResponse.from(
            item,
            userSummary(item.getCreatedBy()),
            item.getCompletedBy() == null ? null : userSummary(item.getCompletedBy())
        );
    }

    private UserSummary userSummary(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
        return UserSummary.from(user);
    }

    private void recordActivity(Task task, Long actorId, String actionType, String fieldName, String oldValue, String newValue) {
        taskActivityRepository.save(new TaskActivity(task.getId(), task.getProjectId(), actorId, actionType, fieldName, oldValue, newValue));
    }

    private String requiredTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw BusinessException.badRequest("CHECKLIST_TITLE_REQUIRED", "Checklist title is required");
        }
        return title.trim();
    }
}
