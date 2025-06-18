package com.example.api_auth_server.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.salt.StringFixedSaltGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JasyptEncryptor {
    private final StandardPBEStringEncryptor encryptor;

    public JasyptEncryptor(@Value("${jasypt.encryptor.password:123456}") String password,
                          @Value("${jasypt.encryptor.algorithm:PBEWithMD5AndDES}") String algo,
                          @Value("${jasypt.encryptor.salt:oauth2-salt}") String salt) {
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        encryptor.setAlgorithm(algo);
        encryptor.setSaltGenerator(new StringFixedSaltGenerator(salt));
    }

    public String encrypt(String plain) {
        return encryptor.encrypt(plain);
    }

    public String decrypt(String cipher) {
        return encryptor.decrypt(cipher);
    }
}
