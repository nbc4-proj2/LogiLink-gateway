package com.logilink.eureka.client.gateway;

import com.logilink.eureka.client.gateway.common.constants.DeliveryUserType;
import com.logilink.eureka.client.gateway.common.exception.AppException;
import com.logilink.eureka.client.gateway.common.exception.GatewayErrorCode;
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
            throw AppException.of(GatewayErrorCode.TOKEN_IS_NOT_EXISTING_OR_INVALID);
        }

        // 유효한 토큰에서 payload(클레임) 파싱
        Claims claims;
        try {
            claims = parseAndValidateToken(token);
        } catch (Exception e) {
            throw AppException.of(GatewayErrorCode.FAILED_TOKEN_VALIDATION);
        }

        Long userId = claims.get("user_id", Long.class);    //필수 데이터
        String role = claims.get("role", String.class);    //필수 데이터
        // 문자열로 받고 직접 파싱
        String hubIdStr = claims.get("hub_id", String.class);
        String companyIdStr = claims.get("company_id", String.class);
        String deliveryTypeStr = claims.get("delivery_type", String.class);   //선택 데이터

        if (userId == null || role == null) {
            throw AppException.of(GatewayErrorCode.REQUIRED_DATA_IS_NULL);
        }

        /*** 새로운 헤더 추가 : 문자열 형태만 가능
         *
         * 각 도메인에서는 아래와 같은 형식으로 캐스팅 후 사용하시면 됩니다.
         * String userIdHeader = request.getHeader("X-User-Id");
         * Long userId = Long.parseLong(userIdHeader);
         * UUID hubId = UUID.fromString(request.getHeader("X-Hub-Id"));
         * Boolean isDeliveryAvailable = Boolean.parseBoolean(request.getHeader("X-Is-Delivery-Available"));
         *
         */
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest()
                .mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role);

        //선택 값들은 있을 때만 헤더 추가
        if (hubIdStr != null) {
            requestBuilder.header("X-Hub-Id", hubIdStr);
        }
        if (companyIdStr != null) {
            requestBuilder.header("X-Company-Id", companyIdStr);
        }
        if (deliveryTypeStr != null) {
            requestBuilder.header("X-Delivery-Type", deliveryTypeStr);
        }

        ServerHttpRequest mutatedRequest = requestBuilder.build();
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
            throw AppException.of(GatewayErrorCode.EXPIRED_TOKEN);
        }

        return claims;
    }
}