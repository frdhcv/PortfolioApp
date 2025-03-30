package com.example.portfolio.Service;

import com.example.portfolio.Repository.ProjectRepository;
import com.example.portfolio.Repository.UserRepository;
import com.example.portfolio.entity.PortfolioEntity;
import com.example.portfolio.entity.ProjectComment;
import com.example.portfolio.entity.ProjectEntity;
import com.example.portfolio.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final String uploadDir = "resources/media";

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        createUploadDirectory();
    }

    private void createUploadDirectory() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public UserEntity getCurrentUser() {
        return userRepository.findById(2L).orElseThrow();
    }

    public ProjectEntity addProjectToMyPortfolio(ProjectEntity project) {
        return addProjectToUserPortfolio(getCurrentUser().getId(), project);
    }

    public ProjectEntity addProjectToUserPortfolio(Long userId, ProjectEntity project) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        PortfolioEntity portfolio = user.getPortfolio();

        if (portfolio == null) {
            portfolio = new PortfolioEntity();
            portfolio.setUser(user);
            user.setPortfolio(portfolio);
        }

        project.setPortfolio(portfolio);
        return projectRepository.save(project);
    }

    public ProjectEntity getProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public List<ProjectEntity> getMyProjects() {
        UserEntity user = getCurrentUser();
        PortfolioEntity portfolio = user.getPortfolio();
        if (portfolio == null) return List.of();
        return projectRepository.findByPortfolio(portfolio);
    }

    public List<ProjectEntity> getProjectsByUser(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        PortfolioEntity portfolio = user.getPortfolio();
        if (portfolio == null) return List.of();
        return projectRepository.findByPortfolio(portfolio);
    }

    public String likeProject(Long projectId) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow();

        if (!user.getLikedProjects().contains(project)) {
            user.getLikedProjects().add(project);
            project.setLikes(project.getLikes() + 1);
            userRepository.save(user);
            projectRepository.save(project);
            return "✅ Project liked!";
        }
        return "⚠️ You already liked this project.";
    }

    public String saveProject(Long projectId) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow();

        if (!user.getSavedProjects().contains(project)) {
            user.getSavedProjects().add(project);
            userRepository.save(user);
            return "✅ Project saved!";
        }
        return "⚠️ You already saved this project.";
    }

    public List<ProjectEntity> getSavedProjects() {
        return getCurrentUser().getSavedProjects();
    }

    public String attachMediaToProject(Long projectId, String imageUrl, String videoUrl) {
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            project.setImageUrl(imageUrl);
        }
        if (videoUrl != null && !videoUrl.isEmpty()) {
            project.setVideoUrl(videoUrl);
        }

        projectRepository.save(project);
        return "✅ Media attached to project!";
    }

    public String addCommentToProject(Long projectId, String text) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow();

        ProjectComment comment = new ProjectComment();
        comment.setUsername(user.getUsername());
        comment.setText(text);
        comment.setPostedAt(LocalDateTime.now());

        project.getComments().add(comment);
        projectRepository.save(project);

        return "✅ Comment added to project!";
    }

    private String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate MD5", e);
        }
    }

    private String getExistingImagePath(String md5Hash, String extension) {
        try {
            Path dir = Paths.get(uploadDir);
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(md5Hash))
                    .findFirst()
                    .map(path -> "/media/" + path.getFileName().toString())
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    public String uploadProjectImage(Long projectId, MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was uploaded");
        }

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        try {
            ProjectEntity project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
            }

            // Get original filename and clean it
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

            // Validate extension
            if (!extension.matches("\\.(jpg|jpeg|png|gif)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG and GIF files are allowed");
            }

            // Calculate MD5 hash of file content
            byte[] fileContent = file.getBytes();
            String md5Hash = calculateMD5(fileContent);

            // Check if this image already exists
            String existingImagePath = getExistingImagePath(md5Hash, extension);
            if (existingImagePath != null) {
                // If image already exists, just update the project with existing image URL
                project.setImageUrl(existingImagePath);
                projectRepository.save(project);
                return "✅ Image reference updated successfully! You can view it at: " + existingImagePath;
            }

            // If image doesn't exist, save it with MD5 hash as filename
            String newFilename = md5Hash + extension;
            Path filePath = Paths.get(uploadDir, newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update project with new image URL
            String imageUrl = "/media/" + newFilename;
            project.setImageUrl(imageUrl);
            projectRepository.save(project);

            return "✅ Image uploaded successfully! You can view it at: " + imageUrl;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image: " + e.getMessage());
        }
    }
}
