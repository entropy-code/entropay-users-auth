package com.entropy.entropay.auth.controllers;

import com.entropy.entropay.auth.dto.JwtRequestDto;
import com.entropy.entropay.auth.dto.JwtResponseDto;
import com.entropy.entropay.auth.service.AuthenticationService;
import com.entropy.entropay.auth.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class JwtAuthController {

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private UserDetailsService userDetailsService;

	@PostMapping(value = "/authenticate")
	public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequestDto authRequestDto) throws Exception {

		authenticationService.authenticate(authRequestDto.getUsername(), authRequestDto.getPassword());

		final UserDetails userDetails = userDetailsService
				.loadUserByUsername(authRequestDto.getUsername());

		final String token = jwtTokenUtil.generateToken(userDetails);

		return ResponseEntity.ok(JwtResponseDto.builder()
				.token(token)
				.build());
	}


}