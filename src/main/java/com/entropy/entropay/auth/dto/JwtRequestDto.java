package com.entropy.entropay.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JwtRequestDto implements Serializable {

	private static final long serialVersionUID = 5926468583005150707L;
	
	private String username;
	private String password;

}