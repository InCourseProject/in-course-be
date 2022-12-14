package com.example.week08.service;

import com.example.week08.domain.Member;
import com.example.week08.domain.UserDetailsImpl;
import com.example.week08.dto.TokenDto;
import com.example.week08.dto.request.KakaoMemberInfoDto;
import com.example.week08.dto.request.NaverMemberInfoDto;
import com.example.week08.errorhandler.BusinessException;
import com.example.week08.jwt.TokenProvider;
import com.example.week08.repository.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static com.example.week08.errorhandler.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverMemberService {
    // https://nid.naver.com/oauth2.0/authorize?client_id=X7Ek1tyoUOUuuk_xRNjx&response_type=code&redirect_uri=http://localhost:8080/api/member/naver&state=123

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public ResponseEntity<NaverMemberInfoDto> naverLogin(String code, HttpServletResponse response) throws JsonProcessingException {
// 1. "?????? ??????"??? ?????? response ??????
        String accessToken = getAccessToken(code);
        System.out.println(accessToken.getBytes().toString()+"      45\n\n");
        // 2. response??? access token?????? ????????? api ??????
        NaverMemberInfoDto naverMemberInfo = getnaverMemberInfo(accessToken);
        System.out.println(naverMemberInfo.getEmail()+"       ????????? 48.\n\n");
        // 3. ???????????? ????????????
        Member naverUser = registerNaverUserIfNeeded(naverMemberInfo);

        // 4. ?????? ????????? ??????
        forceLogin(naverUser);
        System.out.println(naverUser.getNaverId()+"      ????????? ?????? ?????????.\n\n");
        // 5. response Header??? JWT ?????? ??????
        TokenDto token = naverUsersAuthorizationInput(naverUser, response);
        System.out.println("????????? ???????????? ??????????????????.\n\n");
        return ResponseEntity.ok(new NaverMemberInfoDto(token, naverUser));
    }

    private String getAccessToken(String code)throws JsonProcessingException {
        // HTTP Header ??????
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body ??????
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", "7Cl6W7UwRoO8Ag75ZlpV"); //????????? ??????????????? ID, ????????? ????????????
        body.add("client_secret", "FM8I5KRIWK");
        body.add("redirect_uri", "https://incourse.me/login"); //????????? ????????? ???????????? ?????????
        body.add("code", code);
        body.add("state", "911");

        // HTTP ?????? ?????????
        HttpEntity<MultiValueMap<String, String>> naverTokenRequest =
                new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://nid.naver.com/oauth2.0/token",
                HttpMethod.POST,
                naverTokenRequest,
                String.class
        );

        // HTTP ?????? (JSON) -> ????????? ?????? ??????
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    private NaverMemberInfoDto getnaverMemberInfo(String accessToken)throws JsonProcessingException {
        // HTTP Header ??????
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP ?????? ?????????
        HttpEntity<MultiValueMap<String, String>> naverMemberInfoRequest = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.POST,
                naverMemberInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String naverId = jsonNode.get("response").get("id").asText();
        String gender = jsonNode.get("response").get("gender").asText();
        String nickname = jsonNode.get("response").get("name").asText();


        String email = jsonNode.get("response").get("email").asText();
        String image = jsonNode.get("response").get("profile_image").asText();
        log.info("????????? ????????? ??????: id -> " + naverId + ", nickname -> " + nickname+ ", email -> " +email+", profile -> " +image );
        return new NaverMemberInfoDto(email, gender, nickname, naverId, image);
    }

    private Member registerNaverUserIfNeeded(NaverMemberInfoDto naverMemberInfo) {
        // DB ??? ????????? naver Id ??? ????????? ??????
        String naverId = naverMemberInfo.getNaverId();
        Member naverUser = memberRepository.findByNaverId(naverId)
                .orElse(null);
        if (naverUser == null) {
            // ????????????
            String nickname = naverMemberInfo.getNickname();
            Optional<Member> optionalNicname = memberRepository.findByNickname(nickname);
            if(optionalNicname.isPresent()){
                nickname = nickname+ Random();
            }
            String email = naverMemberInfo.getEmail();
            Optional<Member> optionalEmail = memberRepository.findByEmail(email);
            if (optionalEmail.isPresent()){
                throw new BusinessException("?????? ????????? ????????? ?????????. ????????? ????????? ?????? ?????? ???????????? ????????????.",DUPLICATED_USER_EMAIL);
            }
            String gender = naverMemberInfo.getGender();
            if(gender.contains("F")){
                gender="??????";
            }else if(gender.contains("M")){
                gender="??????";
            }else {
                gender="??????";
            }
            System.out.println(gender+"  ??????\n\n");
            String image = naverMemberInfo.getImage(); // ???????????? s3??? ????????? ??? ??? ?????????..
            // password: random UUID
            String password = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

            naverUser = Member.builder()
                    .email(email)
                    .nickname(nickname)
                    .gender(gender)
                    .password(encodedPassword)
                    .profileImage(image)
                    .naverId(naverId)
                    .emailAuth(1)
                    .build();
            memberRepository.save(naverUser);
            log.info(nickname + "?????? ??????????????? ?????????????????????.");
        }
        return naverUser;
    }

    private void forceLogin(Member naverUser) {
        UserDetails userDetails = new UserDetailsImpl(naverUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private TokenDto naverUsersAuthorizationInput(Member naverUser, HttpServletResponse response) {
        // response header??? token ??????
        TokenDto token = tokenProvider.generateTokenDto(naverUser);
        response.addHeader("Authorization", "BEARER" + " " + token.getAccessToken());
        response.addHeader("RefreshToken", token.getRefreshToken());
        response.addHeader("Access-Token-Expire-Time", token.getAccessTokenExpiresIn().toString());
        response.addHeader("User-email", naverUser.getEmail());

        return token;
    }

    public static String Random() {
        Random random = new Random();
        int length = random.nextInt(5)+5;

        StringBuffer newWord = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int choice = random.nextInt(3);
            switch(choice) {
                case 0:
                    newWord.append((char)((int)random.nextInt(25)+97));
                    break;
                case 1:
                    newWord.append((char)((int)random.nextInt(25)+65));
                    break;
                case 2:
                    newWord.append((char)((int)random.nextInt(10)+48));
                    break;
                default:
                    break;
            }
        }
        return newWord.toString();
    } // ???????????? ?????? ?????? ??????
}
