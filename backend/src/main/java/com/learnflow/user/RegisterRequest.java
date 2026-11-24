package com.learnflow.user;
import jakarta.validation.constraints.*;
public record RegisterRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{4,32}", message = "用户名需为 4–32 位字母、数字或下划线") String username,
        @NotBlank @Size(min = 8, max = 72, message = "密码至少 8 位，最多 72 字符") String password,
        @NotBlank @Size(max = 50) String nickname) {}
