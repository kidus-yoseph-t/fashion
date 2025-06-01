package com.project.Fashion.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserDto {
    public String id;
    public String firstName; // Changed from 'firstname' to 'firstName'
    public String lastName;  // Changed from 'lastname' to 'lastName'
    public String email;
    public String role;
}