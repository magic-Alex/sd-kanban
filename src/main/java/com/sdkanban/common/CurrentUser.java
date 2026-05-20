package com.sdkanban.common;

import com.sdkanban.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static Optional<String> username() {
        return authentication()
            .map(CurrentUser::accountName)
            .filter(name -> !"anonymousUser".equals(name));
    }

    public static Optional<User> user() {
        return authentication()
            .map(Authentication::getPrincipal)
            .filter(User.class::isInstance)
            .map(User.class::cast);
    }

    public static Optional<Long> userId() {
        return user().map(User::getId);
    }

    public static Optional<Authentication> authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    private static String accountName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getAccount();
        }
        return authentication.getName();
    }
}
