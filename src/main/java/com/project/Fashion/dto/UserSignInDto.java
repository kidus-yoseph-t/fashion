package com.project.Fashion.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserSignInDto {
    public String email;
    public String password;
}
