package dev.thilanka.resolvr.controller;

import dev.thilanka.resolvr.dto.response.UserResponse;
import dev.thilanka.resolvr.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("assigners")
    public ResponseEntity<List<UserResponse>> getAllAssigners(@RequestParam("districtId") Long districtId) {
        if (districtId == 0) {
            return ResponseEntity.ok(userService.getAllAssigners(25));
        } else return ResponseEntity.ok(userService.getAllUsersByDistrict(districtId));
    }
}
