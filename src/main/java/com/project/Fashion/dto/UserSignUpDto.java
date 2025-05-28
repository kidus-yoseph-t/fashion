package com.project.Fashion.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserSignUpDto {
    public String firstname;
    public String lastname;
    public String email;
    public String password;
    public String role;
}
