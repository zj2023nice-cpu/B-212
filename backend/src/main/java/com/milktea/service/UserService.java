package com.milktea.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.milktea.entity.User;
import com.milktea.dto.LoginDTO;
import com.milktea.dto.RegisterDTO;

public interface UserService extends IService<User> {
    String login(LoginDTO loginDTO);
    void register(RegisterDTO registerDTO);
    User getByUsername(String username);
    void updateAvatar(Long userId, String avatarUrl);
}
