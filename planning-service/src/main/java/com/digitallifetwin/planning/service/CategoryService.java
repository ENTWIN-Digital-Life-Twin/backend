package com.digitallifetwin.planning.service;

import com.digitallifetwin.planning.dto.request.CreateCategoryRequest;
import com.digitallifetwin.planning.dto.request.UpdateCategoryRequest;
import com.digitallifetwin.planning.dto.response.CategoryResponse;
import com.digitallifetwin.planning.entity.TaskCategory;
import com.digitallifetwin.planning.exception.ConflictException;
import com.digitallifetwin.planning.exception.ForbiddenOperationException;
import com.digitallifetwin.planning.exception.TaskCategoryNotFoundException;
import com.digitallifetwin.planning.mapper.PlanningMapper;
import com.digitallifetwin.planning.repository.TaskCategoryRepository;
import com.digitallifetwin.planning.repository.TaskRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final TaskCategoryRepository taskCategoryRepository;
    private final TaskRepository taskRepository;
    private final PlanningMapper planningMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return taskCategoryRepository.findVisibleForUser(userId).stream()
                .map(planningMapper::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest request) {
        String name = request.name().trim();
        if (taskCategoryRepository.existsByUserIdAndNameIgnoreCaseAndSystemCategoryFalse(userId, name)) {
            throw new ConflictException("A category with this name already exists");
        }

        TaskCategory category = new TaskCategory();
        category.setUserId(userId);
        category.setName(name);
        category.setDescription(trimToNull(request.description()));
        category.setColorCode(trimToNull(request.colorCode()));
        category.setSystemCategory(false);
        category.setActive(true);
        return planningMapper.toCategoryResponse(taskCategoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        TaskCategory category = taskCategoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(TaskCategoryNotFoundException::new);
        if (category.isSystemCategory()) {
            throw new ForbiddenOperationException("System categories cannot be modified");
        }
        if (!userId.equals(category.getUserId())) {
            throw new TaskCategoryNotFoundException();
        }

        category.setName(request.name().trim());
        category.setDescription(trimToNull(request.description()));
        category.setColorCode(trimToNull(request.colorCode()));
        return planningMapper.toCategoryResponse(taskCategoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID userId, UUID categoryId) {
        TaskCategory category = taskCategoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(TaskCategoryNotFoundException::new);
        if (category.isSystemCategory()) {
            throw new ForbiddenOperationException("System categories cannot be deleted");
        }
        if (!userId.equals(category.getUserId())) {
            throw new TaskCategoryNotFoundException();
        }

        if (taskRepository.existsByCategoryIdAndDeletedFalse(categoryId)) {
            category.setActive(false);
            taskCategoryRepository.save(category);
            return;
        }
        taskCategoryRepository.delete(category);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
