package com.example.portfolio.Service;

import com.example.portfolio.unitofwork.UnitOfWork;
import com.example.portfolio.unitofwork.UnitOfWorkFactory;
import com.example.portfolio.entity.PortfolioEntity;
import com.example.portfolio.entity.ProjectComment;
import com.example.portfolio.entity.ProjectEntity;
import com.example.portfolio.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
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

    private final UnitOfWorkFactory unitOfWorkFactory;
    private final String uploadDir = "resources/media";

    public ProjectService(UnitOfWorkFactory unitOfWorkFactory) {
        this.unitOfWorkFactory = unitOfWorkFactory;
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
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return uow.getUserRepository().findById(2L).orElseThrow();
        } finally {
            uow.commit();
        }
    }

    public ProjectEntity addProjectToMyPortfolio(ProjectEntity project) {
        return addProjectToUserPortfolio(getCurrentUser().getId(), project);
    }

    public ProjectEntity addProjectToUserPortfolio(Long userId, ProjectEntity project) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = uow.getUserRepository().findById(userId).orElseThrow();
            PortfolioEntity portfolio = user.getPortfolio();

            if (portfolio == null) {
                portfolio = new PortfolioEntity();
                portfolio.setUser(user);
                user.setPortfolio(portfolio);
            }

            project.setPortfolio(portfolio);
            ProjectEntity savedProject = uow.getProjectRepository().save(project);
            uow.commit();
            return savedProject;
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    public ProjectEntity getProjectById(Long id) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return uow.getProjectRepository().findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
        } finally {
            uow.commit();
        }
    }

    public List<ProjectEntity> getMyProjects() {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = getCurrentUser();
            PortfolioEntity portfolio = user.getPortfolio();
            if (portfolio == null) return List.of();
            return uow.getProjectRepository().findByPortfolio(portfolio);
        } finally {
            uow.commit();
        }
    }

    public List<ProjectEntity> getProjectsByUser(Long userId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = uow.getUserRepository().findById(userId).orElseThrow();
            PortfolioEntity portfolio = user.getPortfolio();
            if (portfolio == null) return List.of();
            return uow.getProjectRepository().findByPortfolio(portfolio);
        } finally {
            uow.commit();
        }
    }

    public String likeProject(Long projectId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = getCurrentUser();
            ProjectEntity project = uow.getProjectRepository().findById(projectId).orElseThrow();

            if (!user.getLikedProjects().contains(project)) {
                user.getLikedProjects().add(project);
                project.setLikes(project.getLikes() + 1);
                uow.getUserRepository().save(user);
                uow.getProjectRepository().save(project);
                uow.commit();
                return "✅ Project liked!";
            }
            return "⚠️ You already liked this project.";
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    public String saveProject(Long projectId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = getCurrentUser();
            ProjectEntity project = uow.getProjectRepository().findById(projectId).orElseThrow();

            if (!user.getSavedProjects().contains(project)) {
                user.getSavedProjects().add(project);
                uow.getUserRepository().save(user);
                uow.commit();
                return "✅ Project saved!";
            }
            return "⚠️ You already saved this project.";
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    public String unsaveProject(Long projectId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = getCurrentUser();
            ProjectEntity project = uow.getProjectRepository().findById(projectId).orElseThrow();
            if (user.getSavedProjects().contains(project)) {
                user.getSavedProjects().remove(project);
                uow.getUserRepository().save(user);
                uow.commit();
                return "✅ Project unsaved!";
            }
            return "⚠️ You haven't saved this project.";
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    public ProjectEntity updateProject(Long projectId, ProjectEntity updatedData) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            ProjectEntity project = uow.getProjectRepository().findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            project.setTitle(updatedData.getTitle());
            project.setDescription(updatedData.getDescription());
            project.setGithubLink(updatedData.getGithubLink());

            ProjectEntity savedProject = uow.getProjectRepository().save(project);
            uow.commit();
            return savedProject;
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    public List<ProjectEntity> getSavedProjects() {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return getCurrentUser().getSavedProjects();
        } finally {
            uow.commit();
        }
    }

    public String addCommentToProject(Long projectId, String text) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = getCurrentUser();
            ProjectEntity project = uow.getProjectRepository().findById(projectId).orElseThrow();

            ProjectComment comment = new ProjectComment();
            comment.setUsername(user.getUsername());
            comment.setText(text);
            comment.setPostedAt(LocalDateTime.now());

            project.getComments().add(comment);
            uow.getProjectRepository().save(project);
            uow.commit();
            return "✅ Comment added to project!";
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
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
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            if (file == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was uploaded");
            }

            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
            }

            ProjectEntity project = uow.getProjectRepository().findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
            }

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!extension.matches("\\.(jpg|jpeg|png|gif)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG and GIF files are allowed");
            }

            byte[] fileContent = file.getBytes();
            String md5Hash = calculateMD5(fileContent);
            String existingImagePath = getExistingImagePath(md5Hash, extension);

            if (existingImagePath != null) {
                project.setImageUrl(existingImagePath);
                uow.getProjectRepository().save(project);
                uow.commit();
                return "✅ Image reference updated successfully! You can view it at: " + existingImagePath;
            }

            String newFilename = md5Hash + extension;
            Path filePath = Paths.get(uploadDir, newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/media/" + newFilename;
            project.setImageUrl(imageUrl);
            uow.getProjectRepository().save(project);
            uow.commit();

            return "✅ Image uploaded successfully! You can view it at: " + imageUrl;
        } catch (Exception e) {
            uow.rollback();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image: " + e.getMessage());
        }
    }

    public String deleteProjectImage(Long projectId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            ProjectEntity project = uow.getProjectRepository().findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

            String imageUrl = project.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                return "⚠️ No image to delete.";
            }

            deletePhysicalImageFile(imageUrl);
            project.setImageUrl(null);
            uow.getProjectRepository().save(project);
            uow.commit();

            return "✅ Image deleted successfully.";
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    private void deletePhysicalImageFile(String imageUrl) {
        try {
            String filename = imageUrl.replace("/media/", "");
            Path filePath = Paths.get(uploadDir, filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image file", e);
        }
    }

    public String uploadProjectVideo(Long projectId, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Video file is empty");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                throw new IllegalArgumentException("Invalid file type. Only video files are allowed.");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String md5Hash = calculateMD5(file.getBytes());

            // Load project
            UnitOfWork uow = unitOfWorkFactory.create();
            ProjectEntity project = uow.getProjectRepository().findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // If a video already exists, delete it
            if (project.getVideoUrl() != null) {
                deletePhysicalVideoFile(project.getVideoUrl());
            }

            // Save new video
            String uploadDir = "resources/media";
            File directory = new File(uploadDir);
            if (!directory.exists()) directory.mkdirs();

            String filename = md5Hash + extension;
            Path filePath = Paths.get(uploadDir, filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update project
            String videoUrl = "/media/" + filename;
            project.setVideoUrl(videoUrl);
            uow.getProjectRepository().save(project);
            uow.commit();

            return videoUrl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public String deleteProjectVideo(Long projectId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            ProjectEntity project = uow.getProjectRepository().findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            String videoUrl = project.getVideoUrl();
            if (videoUrl == null || videoUrl.isEmpty()) {
                return "⚠️ No video to delete.";
            }

            deletePhysicalVideoFile(videoUrl);
            project.setVideoUrl(null);
            uow.getProjectRepository().save(project);
            uow.commit();

            return "✅ Video deleted successfully.";
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    private void deletePhysicalVideoFile(String videoUrl) {
        // Remove "/media/" prefix to get the filename
        String filename = videoUrl.replace("/media/", "");
        Path filePath = Paths.get("resources/media", filename);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete video file: " + filename, e);
        }
    }

    private String getExistingVideoPath(String md5Hash, String extension) {
        String uploadDir = "resources/media";
        File directory = new File(uploadDir);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.startsWith(md5Hash) && name.endsWith(extension));
            if (files != null && files.length > 0) {
                return "/media/" + files[0].getName();
            }
        }
        return null;
    }
}
