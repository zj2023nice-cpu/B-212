package com.milktea.controller;

import com.milktea.common.Result;
import com.milktea.common.ErrorCode;
import com.milktea.exception.BusinessException;
import com.milktea.dto.LoginDTO;
import com.milktea.dto.RegisterDTO;
import com.milktea.entity.User;
import com.milktea.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarUploadDir;

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

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.D0002, "请选择要上传的头像文件");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ErrorCode.D0016, "头像文件大小不能超过2MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.D0015, "文件名不能为空");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.D0015, "只支持JPG和PNG格式的图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.D0015, "只支持JPG和PNG格式的图片");
        }

        if (!validateImageMagicBytes(file)) {
            throw new BusinessException(ErrorCode.D0015, "文件内容与声明的图片类型不匹配，疑似恶意文件");
        }

        String safeFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Path uploadPath = Paths.get(avatarUploadDir).normalize().toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(safeFilename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new BusinessException(ErrorCode.D0014, "非法文件路径");
            }

            file.transferTo(filePath.toFile());

            String avatarUrl = "/uploads/avatars/" + safeFilename;

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.getByUsername(username);
            if (user != null) {
                String oldAvatar = user.getAvatarUrl();
                userService.updateAvatar(user.getId(), avatarUrl);
                if (oldAvatar != null && oldAvatar.startsWith("/uploads/avatars/")) {
                    try {
                        Path oldFilePath = uploadPath.resolve(oldAvatar.substring("/uploads/avatars/".length())).normalize();
                        if (oldFilePath.startsWith(uploadPath) && Files.exists(oldFilePath)) {
                            Files.deleteIfExists(oldFilePath);
                        }
                    } catch (Exception e) {
                        logger.warn("删除旧头像文件失败: {}", e.getMessage());
                    }
                }
            }

            logger.info("头像上传成功: userId={}, avatarUrl={}", user != null ? user.getId() : "unknown", avatarUrl);

            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            return Result.success(result);
        } catch (IOException e) {
            logger.error("头像上传失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.D0014, "头像上传失败，请稍后重试");
        }
    }

    private boolean validateImageMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                return false;
            }

            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return true;
            }

            if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50
                    && header[2] == (byte) 0x4E && header[3] == (byte) 0x47
                    && header[4] == (byte) 0x0D && header[5] == (byte) 0x0A
                    && header[6] == (byte) 0x1A && header[7] == (byte) 0x0A) {
                return true;
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
