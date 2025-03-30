package com.example.portfolio.Controller;

import com.example.portfolio.Service.ProjectService;
import com.example.portfolio.entity.ProjectEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Controller", description = "APIs for managing projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // ✅ Add a project for current user
    @PostMapping
    public ResponseEntity<ProjectEntity> createProject(@RequestBody ProjectEntity project) {
        return ResponseEntity.ok(projectService.addProjectToMyPortfolio(project));
    }

    // ✅ Add a project for a specific user (admin feature)
    @PostMapping("/user/{userId}")
    public ResponseEntity<ProjectEntity> createProjectForUser(@PathVariable Long userId, @RequestBody ProjectEntity project) {
        return ResponseEntity.ok(projectService.addProjectToUserPortfolio(userId, project));
    }

    // ✅ Get a project by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProjectEntity> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    // ✅ Get projects of current user
    @GetMapping("/me")
    public ResponseEntity<List<ProjectEntity>> getMyProjects() {
        return ResponseEntity.ok(projectService.getMyProjects());
    }

    // ✅ Get projects of a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProjectEntity>> getProjectsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(projectService.getProjectsByUser(userId));
    }

    // ✅ Like a project
    @PostMapping("/{id}/like")
    public ResponseEntity<String> likeProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.likeProject(id));
    }

    // ✅ Save a project
    @PostMapping("/{id}/save")
    public ResponseEntity<String> saveProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.saveProject(id));
    }

    // ✅ Get saved projects
    @GetMapping("/saved")
    public ResponseEntity<List<ProjectEntity>> getSavedProjects() {
        return ResponseEntity.ok(projectService.getSavedProjects());
    }

    @Operation(
        summary = "Upload image for a project",
        description = "Upload an image file for a specific project. Supports JPG, PNG, and GIF formats."
    )
    @PostMapping(value = "/{id}/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProjectImage(
            @Parameter(description = "Project ID") 
            @PathVariable Long id,
            @Parameter(
                description = "Image file to upload (JPG, PNG, GIF)",
                required = true,
                schema = @Schema(type = "string", format = "base64")
            )
            @RequestPart(value = "file") MultipartFile file) {
        return ResponseEntity.ok(projectService.uploadProjectImage(id, file));
    }

    @Operation(summary = "Attach media to a project", description = "Attach image or video URLs to a project")
    @PostMapping("/{id}/media")
    public ResponseEntity<String> attachMedia(
            @Parameter(description = "Project ID") @PathVariable Long id,
            @Parameter(description = "Image URL") @RequestParam(required = false) String imageUrl,
            @Parameter(description = "Video URL") @RequestParam(required = false) String videoUrl) {
        return ResponseEntity.ok(projectService.attachMediaToProject(id, imageUrl, videoUrl));
    }

    // ✅ Add a comment to a project
    @PostMapping("/{id}/comment")
    public ResponseEntity<String> commentOnProject(@PathVariable Long id,
                                                   @RequestParam String text) {
        return ResponseEntity.ok(projectService.addCommentToProject(id, text));
    }
}
