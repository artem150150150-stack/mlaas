package com.lumenml.security;

import com.lumenml.config.LumenMlProperties;
import com.lumenml.domain.RefreshToken;
import com.lumenml.domain.User;
import com.lumenml.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final LumenMlProperties props;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, LumenMlProperties props) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.props = props;
    }

    @Transactional
    public String issue(User user) {
        byte[] raw = new byte[48];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(token))
                .expiresAt(Instant.now().plusMillis(props.getJwt().getRefreshExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return token;
    }

    @Transactional
    public User consumeAndValidate(String rawRefresh) {
        RefreshToken existing =
                refreshTokenRepository.findByTokenHashAndRevokedIsFalse(hash(rawRefresh)).orElseThrow();
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh expired");
        }
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
        return existing.getUser();
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    private static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
