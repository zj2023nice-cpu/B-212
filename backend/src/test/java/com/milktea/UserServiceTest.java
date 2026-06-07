package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.dto.LoginDTO;
import com.milktea.dto.RegisterDTO;
import com.milktea.entity.User;
import com.milktea.mapper.UserMapper;
import com.milktea.service.impl.UserServiceImpl;
import com.milktea.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setNickname("测试用户");
        testUser.setPhone("13800138000");
        testUser.setRole("USER");

        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setNickname("新用户");
        registerDTO.setPhone("13900139000");
    }

    @Test
    @DisplayName("测试 getByUsername - 用户存在")
    void testGetByUsername_UserExists() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        
        User result = userService.getByUsername("testuser");
        
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("测试用户", result.getNickname());
    }

    @Test
    @DisplayName("测试 getByUsername - 用户不存在")
    void testGetByUsername_UserNotExists() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        
        User result = userService.getByUsername("nonexistent");
        
        assertNull(result);
    }

    @Test
    @DisplayName("测试 loadUserByUsername - 用户存在")
    void testLoadUserByUsername_UserExists() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        
        UserDetails result = userService.loadUserByUsername("testuser");
        
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("测试 loadUserByUsername - 用户不存在")
    void testLoadUserByUsername_UserNotExists() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userService.loadUserByUsername("nonexistent")
        );
        
        assertEquals("User not found: nonexistent", exception.getMessage());
    }

    @Test
    @DisplayName("测试 login - 登录成功")
    void testLogin_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(jwtUtils.generateToken("testuser", "USER", 1L)).thenReturn("mockToken");
        
        String result = userService.login(loginDTO);
        
        assertEquals("mockToken", result);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils, times(1)).generateToken("testuser", "USER", 1L);
    }

    @Test
    @DisplayName("测试 login - 认证失败")
    void testLogin_AuthenticationFailed() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new RuntimeException("Bad credentials"));
        
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.login(loginDTO)
        );
        
        assertEquals("Bad credentials", exception.getMessage());
    }

    @Test
    @DisplayName("测试 register - 注册成功")
    void testRegister_Success() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        userService.register(registerDTO);
        
        verify(userMapper, times(1)).insert(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("测试 register - 用户名已存在")
    void testRegister_UsernameExists() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.register(registerDTO)
        );
        
        assertEquals("Username already exists", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("测试 register - 角色设置为USER")
    void testRegister_DefaultRole() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        userService.register(registerDTO);
        
        verify(userMapper).insert(argThat(user -> 
            "USER".equals(user.getRole())
        ));
    }

    @Test
    @DisplayName("测试 register - 密码加密")
    void testRegister_PasswordEncoded() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        userService.register(registerDTO);
        
        verify(userMapper).insert(argThat(user -> 
            "encodedPassword".equals(user.getPassword())
        ));
    }

    @Test
    @DisplayName("测试 loadUserByUsername - 管理员角色")
    void testLoadUserByUsername_AdminRole() {
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setPassword("adminPassword");
        adminUser.setRole("ADMIN");
        
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(adminUser);
        
        UserDetails result = userService.loadUserByUsername("admin");
        
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("测试 loadUserByUsername - 无角色")
    void testLoadUserByUsername_NoRole() {
        User userWithNoRole = new User();
        userWithNoRole.setId(3L);
        userWithNoRole.setUsername("noroleuser");
        userWithNoRole.setPassword("password");
        userWithNoRole.setRole(null);
        
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userWithNoRole);
        
        UserDetails result = userService.loadUserByUsername("noroleuser");
        
        assertTrue(result.getAuthorities().isEmpty());
    }
}
