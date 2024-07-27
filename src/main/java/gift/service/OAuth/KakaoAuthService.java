package gift.service.OAuth;

import gift.common.enums.LoginType;
import gift.dto.OAuth.AuthTokenResponse;
import gift.model.token.KakaoToken;
import gift.model.user.User;
import gift.repository.token.KakaoTokenRepository;
import gift.repository.user.UserRepository;
import gift.util.AuthUtil;
import gift.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KakaoAuthService {

    private final AuthUtil authUtil;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final KakaoTokenRepository kakaoTokenRepository;

    @Autowired
    public KakaoAuthService(AuthUtil authUtil, JwtUtil jwtUtil, UserRepository userRepository, KakaoTokenRepository kakaoTokenRepository) {
        this.authUtil = authUtil;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.kakaoTokenRepository = kakaoTokenRepository;
    }

    public String createCodeUrl() {
        return authUtil.createGetCodeUrl();
    }

    public String register(String authCode) {

        AuthTokenResponse tokenResponse = authUtil.getAccessToken(authCode);
        String email = authUtil.extractUserEmail(tokenResponse.accessToken());

        User user = userRepository.findByEmail(email).orElseGet(
                () -> userRepository.save(new User(email, "1234", LoginType.KAKAO))
        );

        user.checkLoginType(LoginType.KAKAO);

        saveKakaoAccessToken(tokenResponse.accessToken(), tokenResponse.refreshToken(), user);

        return jwtUtil.generateJWT(user);
    }

    private void saveKakaoAccessToken(String accessToken, String refreshToken, User user) {
        kakaoTokenRepository.findByUser(user).ifPresentOrElse(
                existingToken -> {
                    existingToken.updateTokens(refreshToken, accessToken);
                    kakaoTokenRepository.save(existingToken);
                },
                () -> {
                    KakaoToken newToken = new KakaoToken(user, refreshToken, accessToken);
                    kakaoTokenRepository.save(newToken);
                }
        );
    }
}