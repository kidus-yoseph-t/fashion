package com.project.Fashion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserDto {
    public String id;
    public String firstname;
    public String lastname;
    public String email;
    public String role;
}
