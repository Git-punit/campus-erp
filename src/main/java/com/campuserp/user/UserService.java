package com.campuserp.user;

import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final UserPhotoRepository photos;

    public UserService(UserRepository users, PasswordEncoder encoder, UserPhotoRepository photos) {
        this.users = users;
        this.photos = photos;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return users.findAllByOrderByUsernameAsc();
    }

    public User create(UserRequests.Create req) {
        String username = req.username().trim();
        if (users.existsByUsername(username)) {
            throw new BusinessRuleException("Username '" + username + "' is already taken");
        }
        return users.save(new User(username, encoder.encode(req.password()), req.fullName().trim(), req.role()));
    }

    public User update(Long id, UserRequests.Update req) {
        User user = get(id);
        boolean staysActiveAdmin = req.role() == Role.ADMIN && req.enabled();
        if (!staysActiveAdmin) {
            guardLastAdmin(user, "change the role of or disable");
        }
        user.setFullName(req.fullName().trim());
        user.setRole(req.role());
        user.setEnabled(req.enabled());
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 6) {
                throw new BusinessRuleException("Password must be at least 6 characters");
            }
            user.setPasswordHash(encoder.encode(req.password()));
        }
        return user;
    }

    public void delete(Long id) {
        User user = get(id);
        guardLastAdmin(user, "delete");
        photos.findByUserId(id).ifPresent(photos::delete);
        users.delete(user);
    }

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }

    /** There must always be at least one active admin, or nobody can manage users. */
    private void guardLastAdmin(User user, String action) {
        boolean isActiveAdmin = user.getRole() == Role.ADMIN && user.isEnabled();
        if (isActiveAdmin && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("Can't " + action + " the last admin. Make another user admin first.");
        }
    }
}
