package com.digitallifetwin.planning.controller;

import com.digitallifetwin.planning.dto.request.CreateCategoryRequest;
import com.digitallifetwin.planning.dto.request.UpdateCategoryRequest;
import com.digitallifetwin.planning.dto.response.CategoryResponse;
import com.digitallifetwin.planning.security.SecurityUtils;
import com.digitallifetwin.planning.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/task-categories")
@RequiredArgsConstructor
@Tag(name = "Task Categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List system and user task categories")
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    @Operation(summary = "Create a custom task category")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a custom task category")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or deactivate a custom task category")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
