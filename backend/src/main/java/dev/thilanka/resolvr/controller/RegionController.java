package dev.thilanka.resolvr.controller;

import dev.thilanka.resolvr.dto.response.RegionResponse;
import dev.thilanka.resolvr.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionRepository regionRepository;

    @GetMapping
    public ResponseEntity<List<RegionResponse>> getAll() {
        return ResponseEntity.ok(
                regionRepository.findAll().stream()
                        .map(RegionResponse::from)
                        .toList()
        );
    }
}
