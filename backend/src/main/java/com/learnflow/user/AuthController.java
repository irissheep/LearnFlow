package com.learnflow.user;
import java.util.Map;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.*;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService users;
    private final AuthenticationManager authentication;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions;
    public AuthController(UserService users, AuthenticationManager authentication,
            SecurityContextRepository contexts, SessionAuthenticationStrategy sessions) {
        this.users = users; this.authentication = authentication;
        this.contexts = contexts; this.sessions = sessions;
    }
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(users.register(request));
    }
    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        Authentication result = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        sessions.onAuthentication(result, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        // Spring Security 6+ 需要显式保存；仅放在线程上下文里，下一次请求会丢失登录态。
        contexts.saveContext(context, request, response);
        return users.profile(((LoginUser) result.getPrincipal()).id());
    }
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal LoginUser user) { return users.profile(user.id()); }
}
