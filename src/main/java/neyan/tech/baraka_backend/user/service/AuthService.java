package neyan.tech.baraka_backend.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.baraka_backend.common.exception.AuthenticationException;
import neyan.tech.baraka_backend.common.exception.DuplicateResourceException;
import neyan.tech.baraka_backend.common.security.JwtService;
import neyan.tech.baraka_backend.common.security.UserPrincipal;
import neyan.tech.baraka_backend.user.dto.AuthResponse;
import neyan.tech.baraka_backend.user.dto.LoginRequest;
import neyan.tech.baraka_backend.user.dto.RegisterRequest;
import neyan.tech.baraka_backend.user.dto.UserDto;
import neyan.tech.baraka_backend.user.entity.User;
import neyan.tech.baraka_backend.user.entity.UserRole;
import neyan.tech.baraka_backend.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        // Create user
        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with id: {}", user.getId());

        // Generate token
        UserPrincipal userPrincipal = UserPrincipal.from(user);
        Map<String, Object> claims = buildClaims(user);
        String token = jwtService.generateToken(claims, userPrincipal);

        return AuthResponse.of(token, jwtService.getJwtExpiration(), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for phone: {}", request.getPhone());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findByPhone(request.getPhone())
                    .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

            if (!user.getIsActive()) {
                throw new AuthenticationException("Account is disabled");
            }

            Map<String, Object> claims = buildClaims(user);
            String token = jwtService.generateToken(claims, userPrincipal);

            log.info("User logged in successfully: {}", user.getId());
            return AuthResponse.of(token, jwtService.getJwtExpiration(), UserDto.from(user));

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for phone: {}", request.getPhone());
            throw new AuthenticationException("Invalid phone or password");
        }
    }

    private Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().name());
        claims.put("fullName", user.getFullName());
        return claims;
    }
}

