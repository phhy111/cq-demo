package edu.cqie.cqdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义UserDetails实现类
 * 封装数据库用户信息（含核心的id），存入SecurityContext
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements UserDetails {
    // 数据库用户表的真实ID（核心，用于AI会话记忆隔离）
    private Long id;
    // 数据库用户名（和JWT中解析的username一致）
    private String username;
    // 密码（Spring Security自动校验，AI接口无需使用）
    private String password;
    // 权限列表（你的项目若未做权限控制，空列表即可）
    private List<GrantedAuthority> authorities;

    // ========== 实现UserDetails的默认方法（按你的业务需求返回） ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // 以下方法默认返回true（若你的项目有账号状态控制，按需修改）
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