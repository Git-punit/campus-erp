package com.campuserp.user;

import com.campuserp.budget.ExpenseService;
import com.campuserp.common.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/** Your own profile: name, contact details, designation and photo. Any logged-in user. */
@RestController
@RequestMapping("/api/auth")
public class ProfileController {

    public record ProfileChange(
            @NotBlank @Size(max = 100) String fullName,
            @Email @Size(max = 100) String email,
            @Size(max = 20) @Pattern(regexp = "[0-9+() -]*", message = "only digits, spaces, +, - and brackets")
            String phone,
            @Size(max = 100) String designation) {}

    static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;

    private final UserRepository users;
    private final UserPhotoRepository photos;

    public ProfileController(UserRepository users, UserPhotoRepository photos) {
        this.users = users;
        this.photos = photos;
    }

    @PutMapping("/me")
    @Transactional
    public UserResponse update(@Valid @RequestBody ProfileChange req, Authentication auth) {
        User user = current(auth);
        String email = blank(req.email());
        // emails also log people in, so two accounts can't share one
        if (email != null && users.findFirstByEmailIgnoreCase(email).filter(u -> !u.getId().equals(user.getId())).isPresent()) {
            throw new BusinessRuleException("Another account already uses this email");
        }
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPhone(blank(req.phone()));
        user.setDesignation(blank(req.designation()));
        return UserResponse.from(user);
    }

    // ---------- photo

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public UserResponse uploadPhoto(@RequestPart("file") MultipartFile file, Authentication auth) throws IOException {
        User user = current(auth);
        byte[] data = file.getBytes();
        if (data.length == 0) {
            throw new BusinessRuleException("The file is empty");
        }
        if (data.length > MAX_PHOTO_BYTES) {
            throw new BusinessRuleException("The photo is too big (at most 2 MB)");
        }
        // checked from the file's first bytes, not the name the browser sent
        String type = ExpenseService.fileType(data);
        if (type == null || type.equals("application/pdf")) {
            throw new BusinessRuleException("Only JPG and PNG photos can be used");
        }
        UserPhoto photo = photos.findByUserId(user.getId()).orElseGet(() -> {
            UserPhoto p = new UserPhoto();
            p.setUser(user);
            return p;
        });
        photo.setData(data);
        photos.save(photo);
        user.setPhotoType(type);
        return UserResponse.from(user);
    }

    @GetMapping("/photo")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> photo(Authentication auth) {
        User user = current(auth);
        UserPhoto photo = photos.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No photo"));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(user.getPhotoType())).body(photo.getData());
    }

    @DeleteMapping("/photo")
    @Transactional
    public UserResponse removePhoto(Authentication auth) {
        User user = current(auth);
        photos.findByUserId(user.getId()).ifPresent(photos::delete);
        user.setPhotoType(null);
        return UserResponse.from(user);
    }

    private User current(Authentication auth) {
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
