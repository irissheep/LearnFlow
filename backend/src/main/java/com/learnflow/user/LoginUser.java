package com.learnflow.user;
import java.util.Collection;
import java.util.List;
import java.io.Serializable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// 这是经过认证的服务端身份。业务 userId 从这里取，绝不相信请求中的 userId。
public record LoginUser(long id, String username, String passwordHash)
        implements UserDetails, Serializable {
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
}
