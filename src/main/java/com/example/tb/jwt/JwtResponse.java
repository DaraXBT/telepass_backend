package com.example.tb.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class JwtResponse implements Serializable {
    private final LocalDateTime dateTime;
    private final String username;
    private final String token;




}
