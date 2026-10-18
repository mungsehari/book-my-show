package com.bookmyshow.modules.user;

import com.bookmyshow.modules.user.UserTypes.PrivacySettings;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Me")
@SecurityRequirement(name = "bearer")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/v1/me/privacy")
    @Operation(summary = "Your privacy settings")
    public PrivacySettings getPrivacy(@AuthenticationPrincipal Jwt jwt) {
        return new PrivacySettings(
                userService.isSharingActivity(CurrentUser.from(jwt).id()));
    }

    @PostMapping("/api/v1/me/privacy")
    @Operation(summary = "Show or hide your movie activity from friends")
    public PrivacySettings setPrivacy(@Valid @RequestBody PrivacySettings request, @AuthenticationPrincipal Jwt jwt) {
        return new PrivacySettings(
                userService.setShareActivity(CurrentUser.from(jwt).id(), request.shareActivity()));
    }
}
