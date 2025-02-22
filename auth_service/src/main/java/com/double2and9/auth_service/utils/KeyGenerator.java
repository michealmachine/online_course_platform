package com.double2and9.auth_service.utils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Encoders;

public class KeyGenerator {
    public static void main(String[] args) {
        // 生成一个安全的密钥
        String secretString = Encoders.BASE64URL.encode(Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded());
        System.out.println("Generated secret key: " + secretString);
    }
} 