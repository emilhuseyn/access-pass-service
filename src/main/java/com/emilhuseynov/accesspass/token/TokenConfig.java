package com.emilhuseynov.accesspass.token;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TokenConfig {

    @Bean
    SigningKeyRing signingKeyRing(TokenProperties properties) {
        return SigningKeyRing.fromProperties(properties);
    }

    @Bean
    PassTokenCodec passTokenCodec(SigningKeyRing signingKeyRing) {
        return new PassTokenCodec(signingKeyRing);
    }
}
