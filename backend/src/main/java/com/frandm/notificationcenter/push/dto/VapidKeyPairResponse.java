package com.frandm.notificationcenter.push.dto;

public record VapidKeyPairResponse(
        String publicKey,
        String privateKey
) {}
