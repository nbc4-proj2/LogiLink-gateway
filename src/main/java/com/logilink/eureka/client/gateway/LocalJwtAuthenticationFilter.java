package com.logilink.eureka.client.gateway;

import com.logilink.eureka.client.gateway.common.constants.DeliveryUserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class LocalJwtAuthenticationFilter implements GlobalFilter {

    @Value("${service.jwt.secret-key}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.equals("/api/v1/master/users/signup") ||
                path.equals("/api/v1/users/signup") ||
                path.equals("/api/v1/users/login")) {
            return chain.filter(exchange);  // 회원가입, 로그인 경로는 필터를 적용하지 않음
        }

        // 토큰 추출
        String token = extractToken(exchange);

        // 토큰이 없거나 유효하지 않으면 예외처리
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 유효한 토큰에서 payload(클레임) 파싱
        Claims claims;
        try {
            claims = parseAndValidateToken(token); // ✅ 예외 발생 시 catch로 넘어감
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);
        UUID hubId = claims.get("hubId", UUID.class);
        UUID companyId = claims.get("companyId", UUID.class);
        DeliveryUserType deliveryType = claims.get("deliveryType", DeliveryUserType.class);
        Boolean isDeliveryAvailable = claims.get("isDeliveryAvailable", Boolean.class);


        /*** 새로운 헤더 추가 : 문자열 형태만 가능
         *
         * 각 도메인에서는 아래와 같은 형식으로 캐스팅 후 사용하시면 됩니다.
         * String userIdHeader = request.getHeader("X-User-Id");
         * Long userId = Long.parseLong(userIdHeader);
         * UUID hubId = UUID.fromString(request.getHeader("X-Hub-Id"));
         * Boolean isDeliveryAvailable = Boolean.parseBoolean(request.getHeader("X-Is-Delivery-Available"));
         *
         */
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role)
                .header("X-Hub-Id", hubId.toString())
                .header("X-Company-Id", companyId.toString())
                .header("X-Delivery-Type", deliveryType.name())
                .header("X-Is-Delivery-Available", String.valueOf(isDeliveryAvailable))
                .build();

        // 수정된 요청으로 체인 계속 진행
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Claims parseAndValidateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
        Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        log.info("#####payload :: " + claimsJws.getPayload().toString());

        // 토큰 만료 검사
        Claims claims = claimsJws.getPayload();
        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.before(new Date())) {
            log.error("JWT expired");
        }

        return claims;
    }
}