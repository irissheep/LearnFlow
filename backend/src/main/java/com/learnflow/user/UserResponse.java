package com.learnflow.user;
import java.time.LocalDateTime;
// 响应 DTO 明确排除 passwordHash，避免把实体直接返回给浏览器。
public record UserResponse(Long id, String username, String nickname, LocalDateTime createdAt) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname(), user.getCreatedAt());
    }
}
