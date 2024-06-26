package com.rustam.ccc.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "video not found")
public class NotFoundException extends RuntimeException{
		public NotFoundException(String message) {
			super(message);
	}
}
