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
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@CrossOrigin(origins="**")
@Tag(name = "Project Controller", description = "APIs for managing projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // ✅ Add a project for current user
    @PostMapping("/addProject/currentUser")
    public ResponseEntity<ProjectEntity> createProject(@RequestBody ProjectEntity project) {
        return ResponseEntity.ok(projectService.addProjectToMyPortfolio(project));
    }

    // ✅ Add a project for a specific user (admin feature)
    @PostMapping("/addProject/{userId}")
    public ResponseEntity<ProjectEntity> createProjectForUser(@PathVariable Long userId, @RequestBody ProjectEntity project) {
        return ResponseEntity.ok(projectService.addProjectToUserPortfolio(userId, project));
    }

    // ✅ Get a project by ID
    @GetMapping("/project/{id}")
    public ResponseEntity<ProjectEntity> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    // ✅ Get projects of current user
    @GetMapping("/myProjects")
    public ResponseEntity<List<ProjectEntity>> getMyProjects() {
        return ResponseEntity.ok(projectService.getMyProjects());
    }

    // ✅ Get projects of a specific user
    @GetMapping("/getProjects/{userId}")
    public ResponseEntity<List<ProjectEntity>> getProjectsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(projectService.getProjectsByUser(userId));
    }

    // ✅ Like a project
    @PostMapping("/likeProject/{id}")
    public ResponseEntity<String> likeProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.likeProject(id));
    }

    // ✅ Save a project
    @PostMapping("/saveProject/{projectId}")
    public ResponseEntity<String> saveProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.saveProject(projectId));
    }

    @PostMapping("/unsaveProject/{projectId}")
    public ResponseEntity<String> unsaveProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.unsaveProject(projectId));
    }


    // ✅ Get saved projects
    @GetMapping("/savedProjects")
    public ResponseEntity<List<ProjectEntity>> getSavedProjects() {
        return ResponseEntity.ok(projectService.getSavedProjects());
    }

    @Operation(
            summary = "Upload image for a project",
            description = "Upload an image file for a specific project. Supports JPG, PNG, and GIF formats."
    )
    @PostMapping(value = "/{projectId}/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProjectImage(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId,
            @Parameter(
                    description = "Image file to upload (JPG, PNG, GIF)",
                    required = true,
                    schema = @Schema(type = "string", format = "base64")
            )
            @RequestPart(value = "file") MultipartFile file) {
        return ResponseEntity.ok(projectService.uploadProjectImage(projectId, file));
    }

    @Operation(summary = "Delete image from a project")
    @DeleteMapping("/delete-image/{projectId}")
    public ResponseEntity<String> deleteProjectImage(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.deleteProjectImage(projectId));
    }


    @Operation(
            summary = "Upload video for a project",
            description = "Upload a video file for a specific project. Supports MP4, MOV, and AVI formats."
    )
    @PostMapping(value = "/{projectId}/upload-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProjectVideo(
            @Parameter(description = "Project ID")
            @PathVariable Long projectId,
            @Parameter(
                    description = "Video file to upload (MP4, MOV, AVI)",
                    required = true,
                    schema = @Schema(type = "string", format = "base64")
            )
            @RequestPart(value = "file") MultipartFile file) {
        try {
            String videoUrl = projectService.uploadProjectVideo(projectId, file);
            return ResponseEntity.ok("Video uploaded successfully! You can view it at: " + videoUrl);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload video: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete video for a project")
    @DeleteMapping("/delete-video/{projectId}")
    public ResponseEntity<String> deleteProjectVideo(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.deleteProjectVideo(projectId));
    }

    @PutMapping("/editProject/{projectId}")
    public ResponseEntity<ProjectEntity> updateProject(
            @PathVariable Long projectId,
            @RequestBody ProjectEntity updatedProject) {
        return ResponseEntity.ok(projectService.updateProject(projectId, updatedProject));
    }

//
//    @Operation(summary = "Attach media to a project", description = "Attach image or video URLs to a project")
//    @PostMapping("/{id}/media")
//    public ResponseEntity<String> attachMedia(
//            @Parameter(description = "Project ID") @PathVariable Long id,
//            @Parameter(description = "Image URL") @RequestParam(required = false) String imageUrl,
//            @Parameter(description = "Video URL") @RequestParam(required = false) String videoUrl) {
//        return ResponseEntity.ok(projectService.attachMediaToProject(id, imageUrl, videoUrl));
//    }

    // ✅ Add a comment to a project
    @PostMapping("/commentOnProject/{projectId}")
    public ResponseEntity<String> commentOnProject(@PathVariable Long projectId,
                                                   @RequestParam String text) {
        return ResponseEntity.ok(projectService.addCommentToProject(projectId, text));
    }

}
