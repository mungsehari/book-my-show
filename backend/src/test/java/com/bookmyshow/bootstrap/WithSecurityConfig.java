package com.bookmyshow.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * For {@code @WebMvcTest} classes: loads the real security rules. The token decoder is a mock, so tests log in with
 * {@code SecurityMockMvcRequestPostProcessors.jwt()} instead of real tokens.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SecurityConfig.class)
@MockitoBean(types = JwtDecoder.class)
public @interface WithSecurityConfig {}
