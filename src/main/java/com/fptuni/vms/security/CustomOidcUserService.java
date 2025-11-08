package com.fptuni.vms.security;

import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.UserAuthProvider;
import com.fptuni.vms.repository.RoleRepository;
import com.fptuni.vms.repository.UserAuthProviderRepository;
import com.fptuni.vms.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserAuthProviderRepository uapRepo;
    private final PasswordEncoder passwordEncoder;

    public CustomOidcUserService(UserRepository userRepo,
                                 RoleRepository roleRepo,
                                 UserAuthProviderRepository uapRepo,
                                 PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.uapRepo = uapRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidc = super.loadUser(userRequest); // lấy claims chuẩn từ Google

        Map<String, Object> attrs = oidc.getClaims();
        String sub      = String.valueOf(attrs.get("sub"));
        String email    = String.valueOf(attrs.get("email")).toLowerCase();
        String fullName = String.valueOf(attrs.getOrDefault("name", email));
        String picture  = (String) attrs.get("picture");

        userRepo.findByEmailWithRole(email).ifPresent(u -> {
            String r = u.getRole().getRoleName();
            if ("ADMIN".equals(r) || "ORG_OWNER".equals(r)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("oauth_role_blocked"),
                        "Tổ chức và quản trị viên không được phép đăng nhập bằng Google ");
            }
        });

        // 1) đã link chưa?
        User user;
        Optional<UserAuthProvider> linkOpt =
                uapRepo.findByProviderAndExternalUid("GOOGLE", sub);

        if (linkOpt.isPresent()) {
            user = linkOpt.get().getUser();

            // ---- Gate 1: link có nhưng role không phải VOLUNTEER -> chặn
            String r = user.getRole().getRoleName();
            if ("ADMIN".equals(r) || "ORG_OWNER".equals(r)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("oauth_role_blocked"),
                        "Tổ chức và quản trị viên không được phép đăng nhập bằng Google");
            }

            if ((user.getFullName() == null || user.getFullName().isBlank())) user.setFullName(fullName);
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && picture != null) user.setAvatarUrl(picture);
            userRepo.save(user);
        } else {
            // 2) theo email
            user = userRepo.findByEmail(email).orElse(null);
            if (user == null) {
                Role volunteer = roleRepo.findByRoleName("VOLUNTEER")
                        .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("server_error"),
                                "Không tìm thấy role VOLUNTEER"));
                user = new User();
                user.setFullName(fullName);
                user.setEmail(email);
                user.setAvatarUrl(picture);
                user.setRole(volunteer);
                user.setStatus(User.UserStatus.ACTIVE);
                user.setPasswordHash(passwordEncoder.encode("MatKhau@123"));
                user = userRepo.save(user);
            } else {
                if ((user.getFullName() == null || user.getFullName().isBlank())) user.setFullName(fullName);
                if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && picture != null) user.setAvatarUrl(picture);
                userRepo.save(user);
            }
            UserAuthProvider link = new UserAuthProvider();
            link.setUser(user);
            link.setProvider("GOOGLE");
            link.setExternalUid(sub);
            uapRepo.save(link);
        }

        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_locked"), "Tài khoản đã bị khóa.");
        }

        var authorities = List.of(new SimpleGrantedAuthority(user.getRole().getRoleName()));
        // trả về OIDC user để Security tiếp tục flow
        return new DefaultOidcUser(authorities, oidc.getIdToken(), oidc.getUserInfo(), "sub");
    }
}

