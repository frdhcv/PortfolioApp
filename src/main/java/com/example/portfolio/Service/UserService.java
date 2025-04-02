package com.example.portfolio.Service;

import com.example.portfolio.Repository.UserRepository;
import com.example.portfolio.entity.PortfolioEntity;
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

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity getCurrentUser() {
        return userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No users in database"));
    }


    public UserEntity createUser(UserEntity newUser) {
        if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        if (userRepository.findByUsername(newUser.getUsername()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        // Auto-create portfolio
        PortfolioEntity portfolio = new PortfolioEntity();
        portfolio.setUser(newUser); // one-to-one əlaqə qururuq
        newUser.setPortfolio(portfolio); // iki tərəfli əlaqə

        return userRepository.save(newUser);
    }


    public String uploadUserImage(Long userId, MultipartFile file) {
        UserEntity user = userRepository.findById(userId)
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

        try {
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
            userRepository.save(user);

            return imageUrl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    public String deleteUserImage(Long userId) {
        UserEntity user = userRepository.findById(userId)
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
        userRepository.save(user);
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
        UserEntity target = userRepository.findById(targetId).orElseThrow();

        if (me.getId().equals(targetId)) return "❌ You can't follow yourself.";

        if (!userRepository.isFollowing(me.getId(), targetId)) {
            me.getFollowing().add(target);
            userRepository.save(me);
            return "✅ Followed " + target.getUsername();
        }
        return "⚠️ Already following " + target.getUsername();
    }

    public String unfollowUser(Long targetId) {
        UserEntity me = getCurrentUser();
        UserEntity target = userRepository.findById(targetId).orElseThrow();

        if (userRepository.isFollowing(me.getId(), targetId)) {
            me.getFollowing().remove(target);
            userRepository.save(me);
            return "✅ Unfollowed " + target.getUsername();
        }
        return "⚠️ You're not following " + target.getUsername();
    }

    public String followUserByIds(Long sourceUserId, Long targetUserId) {
        if (sourceUserId.equals(targetUserId)) return "❌ You can't follow yourself.";

        UserEntity source = userRepository.findById(sourceUserId).orElseThrow();
        UserEntity target = userRepository.findById(targetUserId).orElseThrow();

        if (!userRepository.isFollowing(sourceUserId, targetUserId)) {
            source.getFollowing().add(target);
            userRepository.save(source);
            return "✅ " + source.getUsername() + " followed " + target.getUsername();
        }
        return "⚠️ Already following " + target.getUsername();
    }

    public String unfollowUserByIds(Long sourceUserId, Long targetUserId) {
        UserEntity source = userRepository.findById(sourceUserId).orElseThrow();
        UserEntity target = userRepository.findById(targetUserId).orElseThrow();

        if (userRepository.isFollowing(sourceUserId, targetUserId)) {
            source.getFollowing().remove(target);
            userRepository.save(source);
            return "✅ " + source.getUsername() + " unfollowed " + target.getUsername();
        }
        return "⚠️ " + source.getUsername() + " is not following " + target.getUsername();
    }


    public List<UserEntity> getFollowing() {
        return userRepository.findFollowingOfUser(getCurrentUser().getId());
    }

    public List<UserEntity> getFollowers() {
        return userRepository.findFollowersOfUser(getCurrentUser().getId());
    }

    public boolean isFollowing(Long userId) {
        return userRepository.isFollowing(getCurrentUser().getId(), userId);
    }

    public List<String> getFollowersByUserId(Long userId) {
        List<UserEntity> followers = userRepository.findFollowersOfUser(userId);
        return followers.stream()
                .map(UserEntity::getUsername)
                .toList();
    }

    public List<String> getFollowingByUserId(Long userId) {
        List<UserEntity> following = userRepository.findFollowingOfUser(userId);
        return following.stream()
                .map(UserEntity::getUsername)
                .toList();
    }

}