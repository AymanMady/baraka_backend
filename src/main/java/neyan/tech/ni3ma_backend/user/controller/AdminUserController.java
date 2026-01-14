package neyan.tech.ni3ma_backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.common.exception.NotFoundException;
import neyan.tech.ni3ma_backend.user.dto.UserDto;
import neyan.tech.ni3ma_backend.user.entity.User;
import neyan.tech.ni3ma_backend.user.entity.UserRole;
import neyan.tech.ni3ma_backend.user.mapper.UserMapper;
import neyan.tech.ni3ma_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin user management endpoints")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Operation(summary = "Get all users", description = "Returns paginated list of all users")
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<User> users;
        if (role != null && isActive != null) {
            users = userRepository.findByRoleAndIsActive(role, isActive, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else if (isActive != null) {
            users = userRepository.findByIsActive(isActive, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return ResponseEntity.ok(users.map(userMapper::toDto));
    }

    @Operation(summary = "Get user by ID", description = "Returns user details")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @Operation(summary = "Update user status", description = "Activates or deactivates a user")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam Boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        user.setIsActive(isActive);
        user = userRepository.save(user);
        return ResponseEntity.ok(userMapper.toDto(user));
    }
}
