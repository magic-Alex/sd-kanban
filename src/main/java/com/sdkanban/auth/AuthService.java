package com.sdkanban.auth;

import com.sdkanban.common.BusinessException;
import com.sdkanban.config.JwtService;
import com.sdkanban.user.User;
import com.sdkanban.user.UserRepository;
import com.sdkanban.user.UserSummary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        ObjectProvider<UserRepository> userRepositoryProvider,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepositoryProvider = userRepositoryProvider;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserRepository userRepository = userRepository();
        if (userRepository.existsByAccount(request.account())) {
            throw BusinessException.conflict("ACCOUNT_EXISTS", "Account already exists");
        }

        User user = userRepository.save(new User(
            request.account(),
            request.nickname(),
            request.email(),
            passwordEncoder.encode(request.password())
        ));

        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository().findByAccount(request.account())
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(() -> new BadCredentialsException("Invalid account or password"));

        return responseFor(user);
    }

    private UserRepository userRepository() {
        return userRepositoryProvider.getObject();
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.createToken(user), UserSummary.from(user));
    }
}
