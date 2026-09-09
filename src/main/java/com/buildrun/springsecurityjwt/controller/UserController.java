package com.buildrun.springsecurityjwt.controller;

import com.buildrun.springsecurityjwt.controller.dto.CreateUserDto;
import com.buildrun.springsecurityjwt.controller.dto.ResponseUserDto;
import com.buildrun.springsecurityjwt.entities.Role;
import com.buildrun.springsecurityjwt.entities.User;
import com.buildrun.springsecurityjwt.repository.RoleRepository;
import com.buildrun.springsecurityjwt.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    public UserController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<Void> createUser(@RequestBody CreateUserDto userDto) {
        Role role = roleRepository.findByName(Role.Values.BASIC.name());

        Optional<User> userFromDb = userRepository.findByUsername(userDto.username());
        if(userFromDb.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User user = new User(userDto.username(), passwordEncoder.encode(userDto.password()));
        user.setRoles(Set.of(role));

        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<List<ResponseUserDto>> listUsers() {
        List<User> allUsers = userRepository.findAll();
            List<ResponseUserDto> responseUserDtos = allUsers.stream().map(
                    user -> new ResponseUserDto(user.getUserId(), user.getUsername())
            ).toList();
            return ResponseEntity.ok(responseUserDtos);
    }
}
