package com.emilhuseynov.accesspass.staff;

import com.emilhuseynov.accesspass.security.JwtTokenIssuer;
import com.emilhuseynov.accesspass.security.JwtTokenIssuer.IssuedJwt;
import com.emilhuseynov.accesspass.staff.StaffDtos.TokenRequest;
import com.emilhuseynov.accesspass.staff.StaffDtos.TokenResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
class AuthController {

    private final StaffService staffService;
    private final JwtTokenIssuer tokenIssuer;

    AuthController(StaffService staffService, JwtTokenIssuer tokenIssuer) {
        this.staffService = staffService;
        this.tokenIssuer = tokenIssuer;
    }

    @PostMapping("/token")
    ResponseEntity<TokenResponse> token(@Valid @RequestBody TokenRequest request) {
        StaffAccount account = staffService.authenticate(request.username(), request.password());
        IssuedJwt jwt = tokenIssuer.issue(account.getUsername(), List.of(account.getRole().name()));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenResponse(jwt.value(), "Bearer", jwt.expiresIn().toSeconds()));
    }
}
