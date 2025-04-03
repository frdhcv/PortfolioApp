package com.example.portfolio.Service;

import com.example.portfolio.unitofwork.UnitOfWork;
import com.example.portfolio.unitofwork.UnitOfWorkFactory;
import com.example.portfolio.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
import java.util.List;

@Service
public class UserService {

    private final UnitOfWorkFactory unitOfWorkFactory;

    public UserService(UnitOfWorkFactory unitOfWorkFactory) {
        this.unitOfWorkFactory = unitOfWorkFactory;
    }

    public UserEntity getCurrentUser() {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return uow.getUserRepository().findById(2L).orElseThrow();
        } finally {
            uow.commit();
        }
    }

    public String uploadUserImage(Long userId, MultipartFile file) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity user = uow.getUserRepository().findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is empty");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

            if (!extension.matches("\\.(jpg|jpeg|png|gif)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and GIF are allowed");
            }

            byte[] fileContent = file.getBytes();
            String md5Hash = calculateMD5(fileContent);
            String filename = md5Hash + extension;

            String uploadDir = "resources/media";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            Path filePath = Paths.get(uploadDir, filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/media/" + filename;
            user.setProfileImageUrl(imageUrl);
            uow.getUserRepository().save(user);
            uow.commit();

            return imageUrl;

        } catch (Exception e) {
            uow.rollback();
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    public String deleteUserImage(Long userId) {
        UserEntity user = unitOfWorkFactory.create().getUserRepository().findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String imageUrl = user.getProfileImageUrl();
        if (imageUrl == null) return "⚠️ No profile image to delete.";

        try {
            String filename = imageUrl.replace("/media/", "");
            Files.deleteIfExists(Paths.get("resources/media", filename));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image", e);
        }

        user.setProfileImageUrl(null);
        unitOfWorkFactory.create().getUserRepository().save(user);
        return "✅ Profile image deleted.";
    }

    public String uploadProfileImageForCurrentUser(MultipartFile file) {
        UserEntity currentUser = getCurrentUser();
        return uploadUserImage(currentUser.getId(), file);
    }

    public String deleteProfileImageForCurrentUser() {
        UserEntity currentUser = getCurrentUser();
        return deleteUserImage(currentUser.getId());
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
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public String followUser(Long targetId) {
        UserEntity me = getCurrentUser();
        UserEntity target = unitOfWorkFactory.create().getUserRepository().findById(targetId).orElseThrow();

        if (me.getId().equals(targetId)) return "❌ You can't follow yourself.";

        if (!unitOfWorkFactory.create().getUserRepository().isFollowing(me.getId(), targetId)) {
            me.getFollowing().add(target);
            unitOfWorkFactory.create().getUserRepository().save(me);
            return "✅ Followed " + target.getUsername();
        }
        return "⚠️ Already following " + target.getUsername();
    }

    public String unfollowUser(Long targetId) {
        UserEntity me = getCurrentUser();
        UserEntity target = unitOfWorkFactory.create().getUserRepository().findById(targetId).orElseThrow();

        if (unitOfWorkFactory.create().getUserRepository().isFollowing(me.getId(), targetId)) {
            me.getFollowing().remove(target);
            unitOfWorkFactory.create().getUserRepository().save(me);
            return "✅ Unfollowed " + target.getUsername();
        }
        return "⚠️ You're not following " + target.getUsername();
    }

    public String followUserByIds(Long sourceUserId, Long targetUserId) {
        if (sourceUserId.equals(targetUserId)) return "❌ You can't follow yourself.";

        UserEntity source = unitOfWorkFactory.create().getUserRepository().findById(sourceUserId).orElseThrow();
        UserEntity target = unitOfWorkFactory.create().getUserRepository().findById(targetUserId).orElseThrow();

        if (!unitOfWorkFactory.create().getUserRepository().isFollowing(sourceUserId, targetUserId)) {
            source.getFollowing().add(target);
            unitOfWorkFactory.create().getUserRepository().save(source);
            return "✅ " + source.getUsername() + " followed " + target.getUsername();
        }
        return "⚠️ Already following " + target.getUsername();
    }

    public String unfollowUserByIds(Long sourceUserId, Long targetUserId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            UserEntity source = uow.getUserRepository().findById(sourceUserId).orElseThrow();
            UserEntity target = uow.getUserRepository().findById(targetUserId).orElseThrow();

            if (uow.getUserRepository().isFollowing(sourceUserId, targetUserId)) {
                source.getFollowing().remove(target);
                uow.getUserRepository().save(source);
                uow.commit();
                return "✅ " + source.getUsername() + " unfollowed " + target.getUsername();
            }
            return "⚠️ " + source.getUsername() + " is not following " + target.getUsername();
        } catch (Exception e) {
            uow.rollback();
            throw e;
        }
    }

    public List<UserEntity> getFollowing() {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return uow.getUserRepository().findFollowingOfUser(getCurrentUser().getId());
        } finally {
            uow.commit();
        }
    }

    public List<UserEntity> getFollowers() {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return uow.getUserRepository().findFollowersOfUser(getCurrentUser().getId());
        } finally {
            uow.commit();
        }
    }

    public boolean isFollowing(Long userId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            return uow.getUserRepository().isFollowing(getCurrentUser().getId(), userId);
        } finally {
            uow.commit();
        }
    }

    public List<String> getFollowersByUserId(Long userId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            List<UserEntity> followers = uow.getUserRepository().findFollowersOfUser(userId);
            return followers.stream()
                    .map(UserEntity::getUsername)
                    .toList();
        } finally {
            uow.commit();
        }
    }

    public List<String> getFollowingByUserId(Long userId) {
        UnitOfWork uow = unitOfWorkFactory.create();
        try {
            List<UserEntity> following = uow.getUserRepository().findFollowingOfUser(userId);
            return following.stream()
                    .map(UserEntity::getUsername)
                    .toList();
        } finally {
            uow.commit();
        }
    }
}