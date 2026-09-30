package com.weeklyreportgenerator.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.weeklyreportgenerator.backend.security.CustomAccessDeniedHandler;
import com.weeklyreportgenerator.backend.security.CustomAuthenticationEntryPoint;
import com.weeklyreportgenerator.backend.security.JwtAuthenticationFilter;
import com.weeklyreportgenerator.backend.security.RateLimitFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final UserDetailsService userDetailsService;

    // The same origin the frontend-url property points at (email links, CORS) -- one source of
    // truth for "where the SPA lives" so this and the mail links can never drift apart.
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Never "*" together with allowCredentials(true) -- browsers reject that combination
        // outright, and even if they didn't it would defeat the point of a cookie-scoped origin.
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // A double-submit cookie: CookieCsrfTokenRepository writes the token into a
                // readable (non-httpOnly) XSRF-TOKEN cookie; the SPA reads it and echoes it back
                // as X-XSRF-TOKEN on state-changing requests. withHttpOnlyFalse() is required for
                // the SPA's JS to read it at all -- it is not the access/refresh token, so this is
                // not the "browser never exposes tokens to JavaScript" rule being broken.
                //
                // .sessionAuthenticationStrategy(...) HERE (on the csrf() customizer, not
                // sessionManagement()) is required with a STATELESS session policy. Spring
                // Security's CsrfConfigurer always registers a CsrfAuthenticationStrategy into
                // SessionManagementFilter's authentication-strategy chain, and that strategy
                // deletes the XSRF-TOKEN cookie (an anti-fixation measure: rotate the CSRF token
                // whenever a "new" authentication is detected). With no session, EVERY authenticated
                // request looks like a new authentication, so it was deleting the cookie after the
                // very first authenticated call following login -- confirmed by reproducing it with
                // a plain unauthenticated fetch and tracing the call stack to
                // CsrfAuthenticationStrategy.onAuthentication(). Setting the SAME no-op strategy on
                // sessionManagement() does NOT fix this: that setter only feeds one branch of an
                // internally-composed strategy list that CsrfConfigurer independently appends to;
                // csrf()'s own sessionAuthenticationStrategy(...) is the one CsrfConfigurer actually
                // checks before constructing its default CsrfAuthenticationStrategy.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .ignoringRequestMatchers(
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/invitations/**"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .headers(headers -> headers
                        // Invite/reset links carry a single-use token in the query string -- this
                        // stops it leaking to a third party via the Referer header if the accept/
                        // reset page ever links out or loads a third-party resource.
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/csrf",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/invitations/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                // jwtAuthenticationFilter must be registered (relative to a standard Spring
                // Security filter) before a second custom filter can be positioned relative to IT
                // -- so this has to come first, even though rateLimitFilter is the one that should
                // actually run earliest in the chain.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
