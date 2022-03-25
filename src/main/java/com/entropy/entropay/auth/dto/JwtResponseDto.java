package com.entropy.entropay.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class JwtResponseDto implements Serializable {

	private static final long serialVersionUID = -8091879091924046844L;
	private final String token;

}