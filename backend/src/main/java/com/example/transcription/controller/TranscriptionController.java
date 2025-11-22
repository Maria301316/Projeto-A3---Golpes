package com.example.transcription.controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.transcription.service.TranscriptionService;

@RestController
@RequestMapping("/api/transcriptions")
@CrossOrigin(origins = "*")
public class TranscriptionController {
    private final TranscriptionService service;
    public TranscriptionController(TranscriptionService service){ this.service = service; }

    @PostMapping("/upload")
    public String uploadAudio(@RequestParam("file") MultipartFile file) throws Exception{
                
        return service.transcribeAudio(file);
    }
}
