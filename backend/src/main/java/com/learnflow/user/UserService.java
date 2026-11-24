package com.learnflow.user;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import com.learnflow.common.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    public UserService(UserMapper mapper, PasswordEncoder encoder) {
        this.mapper = mapper;
        this.encoder = encoder;
    }
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码的 UTF-8 编码不能超过 72 字节");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.username().toLowerCase(Locale.ROOT));
        user.setNickname(request.nickname().strip());
        if (user.getNickname().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "昵称不能为空");
        // BCrypt 每次自动生成盐；数据库只保存哈希，从不保存明文密码。
        user.setPasswordHash(encoder.encode(request.password()));
        try { mapper.insert(user); }
        catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已被使用");
        }
        return UserResponse.from(mapper.findById(user.getId()));
    }
    public UserResponse profile(long id) { return UserResponse.from(mapper.findById(id)); }
}
