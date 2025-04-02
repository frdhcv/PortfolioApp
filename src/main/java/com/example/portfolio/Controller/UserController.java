package com.example.portfolio.Controller;

import com.example.portfolio.Service.UserService;
import com.example.portfolio.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create new user")
    @PostMapping("/createUser")
    public ResponseEntity<UserEntity> createUser(@RequestBody UserEntity userEntity) {
        return ResponseEntity.ok(userService.createUser(userEntity));
    }


    @Operation(summary = "Upload user profile image")
    @PostMapping(value = "/upload-image/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadUserImage(@PathVariable Long userId, @RequestPart MultipartFile file) {
        return ResponseEntity.ok(userService.uploadUserImage(userId, file));
    }

    @Operation(summary = "Delete user profile image")
    @DeleteMapping("/delete-image/{userId}")
    public ResponseEntity<String> deleteUserImage(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.deleteUserImage(userId));
    }

    @Operation(summary = "Upload profile image for current user")
    @PostMapping(value = "/currentUser/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadImageForMe(@RequestPart MultipartFile file) {
        return ResponseEntity.ok(userService.uploadProfileImageForCurrentUser(file));
    }

    @Operation(summary = "Delete profile image for current user")
    @DeleteMapping("/currentUser/delete-image")
    public ResponseEntity<String> deleteImageForMe() {
        return ResponseEntity.ok(userService.deleteProfileImageForCurrentUser());
    }



    @PostMapping("/{id}/follow")
    public ResponseEntity<String> follow(@PathVariable Long id) {
        return ResponseEntity.ok(userService.followUser(id));
    }

    @PostMapping("/{id}/unfollow")
    public ResponseEntity<String> unfollow(@PathVariable Long id) {
        return ResponseEntity.ok(userService.unfollowUser(id));
    }

    @PostMapping("/{followerId}/follow/{followingId}")
    public ResponseEntity<String> followUserByIds(@PathVariable Long followerId, @PathVariable Long followingId) {
        return ResponseEntity.ok(userService.followUserByIds(followerId, followingId));
    }

    @PostMapping("/{unfollowerId}/unfollow/{unfollowingId}")
    public ResponseEntity<String> unfollowUserByIds(@PathVariable Long unfollowerId, @PathVariable Long unfollowingId) {
        return ResponseEntity.ok(userService.unfollowUserByIds(unfollowerId, unfollowingId));
    }



    @GetMapping("/myFollowing")
    public ResponseEntity<List<UserEntity>> getFollowing() {
        return ResponseEntity.ok(userService.getFollowing());
    }

    @GetMapping("/myFollowers")
    public ResponseEntity<List<UserEntity>> getFollowers() {
        return ResponseEntity.ok(userService.getFollowers());
    }

    @GetMapping("/check-following/{id}")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long id) {
        return ResponseEntity.ok(userService.isFollowing(id));
    }
    @GetMapping("/{id}/followersList")
    public ResponseEntity<List<String>> getFollowersByUserId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getFollowersByUserId(id));
    }
    @GetMapping("/{id}/followingList")
    public ResponseEntity<List<String>> getFollowingByUserId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getFollowingByUserId(id));
    }
}