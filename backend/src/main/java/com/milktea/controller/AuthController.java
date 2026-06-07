package com.milktea.controller;

import com.milktea.common.Result;
import com.milktea.dto.LoginDTO;
import com.milktea.dto.RegisterDTO;
import com.milktea.entity.User;
import com.milktea.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("Register success");
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        User user = userService.getByUsername(loginDTO.getUsername());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.success(data);
    }

    @GetMapping("/me")
    public Result<User> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getByUsername(username);
        return Result.success(user);
    }
}
