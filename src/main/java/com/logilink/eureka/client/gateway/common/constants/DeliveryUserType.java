package com.logilink.eureka.client.gateway.common.constants;

import lombok.Getter;

/**
 * 배송 담당 타입
 */
@Getter
public enum DeliveryUserType {

    HUB("허브"),
    COMPANY("업체");

    private final String type;

    DeliveryUserType(String type) {
        this.type = type;
    }
}
