package dev.thilanka.resolvr.controller;

import dev.thilanka.resolvr.dto.response.DistrictResponse;
import dev.thilanka.resolvr.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictRepository districtRepository;

    @GetMapping
    public ResponseEntity<List<DistrictResponse>> getAll() {
        return ResponseEntity.ok(
                districtRepository.findAll().stream()
                        .map(DistrictResponse::from)
                        .toList()
        );
    }
}
