package dev.thilanka.resolvr.service;

import dev.thilanka.resolvr.dto.response.UserResponse;
import dev.thilanka.resolvr.model.entity.User;
import dev.thilanka.resolvr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserResponse> getAllAssigners(int limit) {
        List<User> users = userRepository.findAllAssigners(limit);
        return users.stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> getAllUsersByDistrict(Long districtId) {
        List<User> users = userRepository.findAllAssignersByDistrict(districtId);
        return users.stream().map(UserResponse::from).toList();
    }
}
