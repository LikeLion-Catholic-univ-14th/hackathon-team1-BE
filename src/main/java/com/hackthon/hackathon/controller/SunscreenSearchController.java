package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.repository.SunscreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sunscreens")
public class SunscreenSearchController {
    private  final SunscreenRepository sunscreenRepository;

    @GetMapping("/search")
    public List<Sunscreen> findBySunnameContaining(@RequestParam String keyword) {
        return sunscreenRepository.findByNameContaining(keyword);
    }

}
