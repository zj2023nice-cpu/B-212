package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.milktea.dto.LoginDTO;
import com.milktea.dto.RegisterDTO;
import com.milktea.entity.User;
import com.milktea.mapper.UserMapper;
import com.milktea.service.UserService;
import com.milktea.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService, UserDetailsService {

    @Autowired
    @org.springframework.context.annotation.Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        // 添加角色权限，Spring Security 约定角色以 ROLE_ 前缀开头
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        }
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }

    @Override
    public String login(LoginDTO loginDTO) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );
        // 获取用户的角色信息和用户ID，添加到 Token 中
        User user = getByUsername(loginDTO.getUsername());
        return jwtUtils.generateToken(loginDTO.getUsername(), user.getRole(), user.getId());
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        if (getByUsername(registerDTO.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        user.setPhone(registerDTO.getPhone());
        user.setRole("USER");
        this.save(user);
    }

    @Override
    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
