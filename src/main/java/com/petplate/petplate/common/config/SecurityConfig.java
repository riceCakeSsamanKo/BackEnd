package com.petplate.petplate.common.config;

import com.petplate.petplate.auth.jwt.JwtFilter;
import com.petplate.petplate.auth.jwt.TokenProvider;
import com.petplate.petplate.auth.jwt.handler.JwtAccessDeniedHandler;
import com.petplate.petplate.auth.jwt.handler.JwtAuthenticationEntryPointHandler;
import com.petplate.petplate.auth.oauth.cookie.HttpCookieOAuth2AuthorizationRequestRepository;
import com.petplate.petplate.auth.oauth.handler.OAuth2LoginSuccessHandler;
import com.petplate.petplate.auth.oauth.service.CustomOAuth2LoginAuthenticationProvider;
import com.petplate.petplate.auth.oauth.service.CustomOAuth2UserService;
import com.petplate.petplate.auth.oauth.service.SocialLoginTokenUtil;
import com.petplate.petplate.user.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPointHandler jwtAuthenticationEntryPointHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final SocialLoginTokenUtil socialLoginTokenUtil;



    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exception)->exception.authenticationEntryPoint(jwtAuthenticationEntryPointHandler))
                .exceptionHandling((exception)->exception.accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests((requests) ->
                        requests
                                .requestMatchers("/test/login").hasAuthority(Role.GENERAL.toString())
                                .requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**").permitAll()// swagger 경로 접근 허용
                                .requestMatchers("/api/v1/users/my-profile").hasAuthority(Role.GENERAL.toString())
                                .anyRequest().permitAll()//지금은 다 permit all 로 합시다! (개발 과정 동안은)
                )
                .oauth2Login(configure ->
                        configure.authorizationEndpoint(
                                        config-> config.authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
                                .userInfoEndpoint(
                                        config -> config.userService(customOAuth2UserService))
                                .successHandler(oAuth2LoginSuccessHandler)
                );

        http.authenticationProvider(new CustomOAuth2LoginAuthenticationProvider(accessTokenResponseClient(),customOAuth2UserService,
                socialLoginTokenUtil));

        return http.addFilterBefore(new JwtFilter(tokenProvider,redisTemplate), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        return new DefaultAuthorizationCodeTokenResponseClient();
    }

    @Bean
    public OAuth2AuthorizationCodeAuthenticationProvider authorizationCodeAuthenticationProvider(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
        return new OAuth2AuthorizationCodeAuthenticationProvider(accessTokenResponseClient);
    }



}