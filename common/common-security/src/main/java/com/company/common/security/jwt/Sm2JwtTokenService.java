package com.company.common.security.jwt;

import com.company.common.security.config.SecurityJwtProperties;
import com.company.common.security.crypto.BouncyCastleProviderRegistrar;
import com.company.common.security.exception.SecurityAuthenticationException;
import com.company.common.security.exception.SecurityErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class Sm2JwtTokenService {

    private static final String DEV_KEY_PATH = ".sy-base-project/security/dev-sm2-jwt.properties";

    private final ObjectMapper objectMapper;
    private final SecurityJwtProperties properties;
    private volatile KeyPair generatedKeyPair;

    public Sm2JwtTokenService(ObjectMapper objectMapper, SecurityJwtProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        BouncyCastleProviderRegistrar.ensureRegistered();
    }

    public String generateToken(JwtAccessTokenClaims claims) {
        Map<String, Object> header = Map.of(
            "alg", "SM2SM3",
            "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.getIssuer());
        payload.put("sub", claims.userId());
        payload.put("loginName", claims.loginName());
        payload.put("jti", claims.tokenId());
        payload.put("iat", claims.issuedAt().getEpochSecond());
        payload.put("exp", claims.expiresAt().getEpochSecond());

        try {
            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String signatureSource = encodedHeader + "." + encodedPayload;
            byte[] signature = sign(signatureSource.getBytes(StandardCharsets.UTF_8));
            return signatureSource + "." + base64Url(signature);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize JWT payload", exception);
        }
    }

    public JwtAccessTokenClaims parseAndValidate(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new SecurityAuthenticationException(
                SecurityErrorCode.INVALID_ACCESS_TOKEN,
                401,
                "Access token format is invalid");
        }
        String source = parts[0] + "." + parts[1];
        byte[] signature = decodeBase64Url(parts[2]);
        if (!verify(source.getBytes(StandardCharsets.UTF_8), signature)) {
            throw new SecurityAuthenticationException(
                SecurityErrorCode.INVALID_ACCESS_TOKEN,
                401,
                "Access token signature is invalid");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                decodeBase64Url(parts[1]),
                new TypeReference<>() {
                });
            String issuer = stringValue(payload.get("iss"));
            if (!properties.getIssuer().equals(issuer)) {
                throw new SecurityAuthenticationException(
                    SecurityErrorCode.INVALID_ACCESS_TOKEN,
                    401,
                    "Access token issuer is invalid");
            }
            String userId = stringValue(payload.get("sub"));
            String loginName = stringValue(payload.get("loginName"));
            String tokenId = stringValue(payload.get("jti"));
            Instant issuedAt = Instant.ofEpochSecond(longValue(payload.get("iat")));
            Instant expiresAt = Instant.ofEpochSecond(longValue(payload.get("exp")));
            if (Instant.now().isAfter(expiresAt)) {
                throw new SecurityAuthenticationException(
                    SecurityErrorCode.ACCESS_TOKEN_EXPIRED,
                    401,
                    "Access token is expired");
            }
            return new JwtAccessTokenClaims(tokenId, userId, loginName, issuedAt, expiresAt);
        } catch (JsonProcessingException exception) {
            throw new SecurityAuthenticationException(
                SecurityErrorCode.INVALID_ACCESS_TOKEN,
                401,
                "Access token payload is invalid");
        }
    }

    public SecurityJwtProperties getProperties() {
        return properties;
    }

    private byte[] sign(byte[] source) {
        try {
            Signature signature = Signature.getInstance("SM3withSM2", BouncyCastleProviderRegistrar.PROVIDER_NAME);
            signature.initSign(privateKey());
            signature.update(source);
            return signature.sign();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    private boolean verify(byte[] source, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("SM3withSM2", BouncyCastleProviderRegistrar.PROVIDER_NAME);
            signature.initVerify(publicKey());
            signature.update(source);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException exception) {
            throw new SecurityAuthenticationException(
                SecurityErrorCode.INVALID_ACCESS_TOKEN,
                401,
                "Access token verification failed");
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decodeBase64Url(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new SecurityAuthenticationException(
                SecurityErrorCode.INVALID_ACCESS_TOKEN,
                401,
                "Access token encoding is invalid");
        }
    }

    private ECPrivateKey privateKey() throws GeneralSecurityException {
        String encoded = normalizeKey(properties.getSm2().getPrivateKey());
        if (!StringUtils.hasText(encoded)) {
            return (ECPrivateKey) keyPair().getPrivate();
        }
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderRegistrar.PROVIDER_NAME);
        return (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }

    private ECPublicKey publicKey() throws GeneralSecurityException {
        String encoded = normalizeKey(properties.getSm2().getPublicKey());
        if (!StringUtils.hasText(encoded)) {
            return (ECPublicKey) keyPair().getPublic();
        }
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderRegistrar.PROVIDER_NAME);
        return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }

    private KeyPair keyPair() throws GeneralSecurityException {
        KeyPair current = generatedKeyPair;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (generatedKeyPair == null) {
                generatedKeyPair = loadOrCreateDevelopmentKeyPair();
            }
            return generatedKeyPair;
        }
    }

    private KeyPair loadOrCreateDevelopmentKeyPair() throws GeneralSecurityException {
        try {
            Path keyFile = Path.of(System.getProperty("user.home"), DEV_KEY_PATH);
            Files.createDirectories(keyFile.getParent());
            if (Files.exists(keyFile)) {
                Properties properties = new Properties();
                try (var inputStream = Files.newInputStream(keyFile)) {
                    properties.load(inputStream);
                }
                String privateKey = properties.getProperty("privateKey");
                String publicKey = properties.getProperty("publicKey");
                if (StringUtils.hasText(privateKey) && StringUtils.hasText(publicKey)) {
                    return new KeyPair(
                        decodePublicKey(publicKey),
                        decodePrivateKey(privateKey));
                }
            }

            KeyPair keyPair = generateKeyPair();
            Properties devProperties = new Properties();
            devProperties.setProperty("privateKey", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            devProperties.setProperty("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            try (var outputStream = Files.newOutputStream(
                keyFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
                devProperties.store(outputStream, "Development SM2 JWT key pair");
            }
            return keyPair;
        } catch (Exception exception) {
            if (exception instanceof GeneralSecurityException generalSecurityException) {
                throw generalSecurityException;
            }
            throw new IllegalStateException("Failed to initialize development SM2 key pair", exception);
        }
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
            "EC",
            BouncyCastleProviderRegistrar.PROVIDER_NAME);
        keyPairGenerator.initialize(new ECGenParameterSpec("sm2p256v1"));
        return keyPairGenerator.generateKeyPair();
    }

    private ECPrivateKey decodePrivateKey(String encoded) throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderRegistrar.PROVIDER_NAME);
        return (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }

    private ECPublicKey decodePublicKey(String encoded) throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderRegistrar.PROVIDER_NAME);
        return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }

    private String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(stringValue(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
