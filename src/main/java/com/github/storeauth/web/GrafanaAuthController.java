package com.github.storeauth.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class GrafanaAuthController {

    private final JwtDecoder jwtDecoder;

    @GetMapping("/grafana-auth")
    public ResponseEntity<Void> grafanaAuth(
            @CookieValue(value = "grafana_session", required = false) String cookieToken) {

        if (cookieToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Jwt jwt = jwtDecoder.decode(cookieToken);

            List<String> roles = jwt.getClaimAsStringList("roles");
            String grafanaRole = roles != null && roles.contains("ADMIN") ? "Admin" : "Viewer";

            return ResponseEntity.ok()
                    .header("X-WEBAUTH-USER", jwt.getSubject())
                    .header("X-WEBAUTH-ROLE", grafanaRole)
                    .build();
        } catch (JwtException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> openGrafana(@AuthenticationPrincipal Jwt jwt, HttpServletResponse response) {

        List<String> roles = jwt.getClaimAsStringList("roles");
        String grafanaRole = roles != null && roles.contains("ADMIN") ? "Admin" : "Viewer";

        response.setHeader("X-WEBAUTH-USER", jwt.getSubject());
        response.setHeader("X-WEBAUTH-ROLE", grafanaRole);

        ResponseCookie cookie = ResponseCookie.from("grafana_session", jwt.getTokenValue())
                .httpOnly(true)
                .secure(true)
                .path("/grafana")
                .maxAge(600)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }
}
