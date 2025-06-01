package com.project.Fashion.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserSignUpDto {
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String role;
}
