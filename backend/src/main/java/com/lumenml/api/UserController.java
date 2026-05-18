package com.lumenml.api;

import com.lumenml.api.dto.UserDto;
import com.lumenml.api.mapper.ApiMapper;
import com.lumenml.repository.UserRepository;
import com.lumenml.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ApiMapper apiMapper;

    @GetMapping("/me")
    public UserDto me() {
        var principal = SecurityUtils.requireCurrentUser();
        return userRepository.findById(principal.id()).map(apiMapper::toUserDto).orElseThrow();
    }
}
