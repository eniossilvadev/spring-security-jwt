package com.buildrun.springsecurityjwt.controller.dto;

import java.util.UUID;

public record ResponseUserDto (UUID userId, String username){
}
