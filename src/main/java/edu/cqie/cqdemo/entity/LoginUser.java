package edu.cqie.cqdemo.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 自定义UserDetails，承载UserID等自定义信息
 */
@Data
public class LoginUser implements UserDetails {
    // 用户ID（类型和你数据库Users的id一致：Long/Integer/String）
    private Long id;
    // 用户名
    private String username;
    // 加密后的密码
    private String password;

    // 从数据库Users对象构造
    public LoginUser(Users user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
    }

    // 实现UserDetails抽象方法（默认账户可用，后续可扩展）
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}