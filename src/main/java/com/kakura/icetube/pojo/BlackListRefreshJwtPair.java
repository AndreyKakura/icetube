package com.kakura.icetube.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class BlackListRefreshJwtPair implements Serializable {
    private String oldRefreshToken;
    private String refreshToken;
}
