package com.sdkanban.user;

import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class DefaultAdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String account;
    private final String password;
    private final String nickname;
    private final String email;

    public DefaultAdminInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.auth.default-admin.enabled:true}") boolean enabled,
        @Value("${app.auth.default-admin.account:sd-robot}") String account,
        @Value("${app.auth.default-admin.password:1}") String password,
        @Value("${app.auth.default-admin.nickname:系统管理员}") String nickname,
        @Value("${app.auth.default-admin.email:}") String email
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.account = account;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        userRepository.findByAccount(account)
            .ifPresentOrElse(this::ensureAdmin, this::createAdmin);
    }

    private void ensureAdmin(User user) {
        user.activate();
        user.promoteToAdmin();
    }

    private void createAdmin() {
        userRepository.save(new User(
            account,
            nickname,
            StringUtils.hasText(email) ? email.trim() : null,
            passwordEncoder.encode(password),
            "ADMIN"
        ));
    }
}
