package com.pgmanagement.security;

import com.pgmanagement.entity.Owner;
import com.pgmanagement.entity.User;
import com.pgmanagement.repository.OwnerRepository;
import com.pgmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService for loading users (both Owner and User) by email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        // Try to find as Owner first
        Owner owner = ownerRepository.findByEmail(email).orElse(null);
        if (owner != null) {
            log.debug("Found owner: {}", email);
            return new UserDetailsImpl(
                    owner.getId(),
                    owner.getEmail(),
                    owner.getPasswordHash(),
                    owner.getRole()
            );
        }

        // Try to find as User
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            log.debug("Found user: {}", email);
            return new UserDetailsImpl(
                    user.getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getRole()
            );
        }

        // Not found
        log.warn("User not found with email: {}", email);
        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
