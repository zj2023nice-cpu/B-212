package com.milktea;

import com.milktea.controller.AuthController;
import com.milktea.dto.LoginDTO;
import com.milktea.dto.RegisterDTO;
import com.milktea.entity.User;
import com.milktea.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 测试")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

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

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    @DisplayName("测试 register - 注册成功")
    void testRegister_Success() {
        doNothing().when(userService).register(any(RegisterDTO.class));
        
        var result = authController.register(registerDTO);
        
        assertTrue(result.isSuccess());
        assertEquals("Register success", result.getMessage());
        verify(userService, times(1)).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("测试 register - 用户名已存在")
    void testRegister_UsernameExists() {
        doThrow(new RuntimeException("Username already exists"))
            .when(userService).register(any(RegisterDTO.class));
        
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> authController.register(registerDTO)
        );
        
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    @DisplayName("测试 login - 登录成功")
    void testLogin_Success() {
        String mockToken = "mockJwtToken123";
        
        when(userService.login(any(LoginDTO.class))).thenReturn(mockToken);
        when(userService.getByUsername("testuser")).thenReturn(testUser);
        
        var result = authController.login(loginDTO);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        
        Map<String, Object> data = result.getData();
        assertEquals(mockToken, data.get("token"));
        assertEquals(testUser, data.get("user"));
        
        verify(userService, times(1)).login(any(LoginDTO.class));
        verify(userService, times(1)).getByUsername("testuser");
    }

    @Test
    @DisplayName("测试 login - 登录失败")
    void testLogin_Failure() {
        when(userService.login(any(LoginDTO.class)))
            .thenThrow(new RuntimeException("Bad credentials"));
        
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> authController.login(loginDTO)
        );
        
        assertEquals("Bad credentials", exception.getMessage());
    }

    @Test
    @DisplayName("测试 me - 获取当前用户信息成功")
    void testMe_Success() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getByUsername("testuser")).thenReturn(testUser);
        
        var result = authController.me();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("testuser", result.getData().getUsername());
        assertEquals("测试用户", result.getData().getNickname());
        
        verify(userService, times(1)).getByUsername("testuser");
    }

    @Test
    @DisplayName("测试 me - 用户不存在")
    void testMe_UserNotFound() {
        when(authentication.getName()).thenReturn("nonexistent");
        when(userService.getByUsername("nonexistent")).thenReturn(null);
        
        var result = authController.me();
        
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("测试 login - 验证返回的token和用户信息")
    void testLogin_VerifyReturnData() {
        String mockToken = "mockJwtToken123";
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole("ADMIN");
        
        when(userService.login(any(LoginDTO.class))).thenReturn(mockToken);
        when(userService.getByUsername("admin")).thenReturn(adminUser);
        
        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");
        
        var result = authController.login(adminLogin);
        
        assertTrue(result.isSuccess());
        Map<String, Object> data = result.getData();
        
        assertNotNull(data.get("token"));
        assertNotNull(data.get("user"));
        
        User returnedUser = (User) data.get("user");
        assertEquals("admin", returnedUser.getUsername());
        assertEquals("ADMIN", returnedUser.getRole());
    }

    @Test
    @DisplayName("测试 register - 验证参数传递")
    void testRegister_VerifyParameters() {
        authController.register(registerDTO);
        
        verify(userService).register(argThat(dto -> 
            "newuser".equals(dto.getUsername()) &&
            "password123".equals(dto.getPassword()) &&
            "新用户".equals(dto.getNickname()) &&
            "13900139000".equals(dto.getPhone())
        ));
    }

    @Test
    @DisplayName("测试 login - 验证参数传递")
    void testLogin_VerifyParameters() {
        when(userService.login(any(LoginDTO.class))).thenReturn("token");
        when(userService.getByUsername(anyString())).thenReturn(testUser);
        
        authController.login(loginDTO);
        
        verify(userService).login(argThat(dto -> 
            "testuser".equals(dto.getUsername()) &&
            "password123".equals(dto.getPassword())
        ));
    }
}
