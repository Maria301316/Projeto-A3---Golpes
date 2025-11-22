package com.example.transcription.service;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.transcription.repository.TranscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.babamaria.transcription.WhisperCliTranscriber;

@Service
public class TranscriptionService {
    private final TranscriptionRepository repo;
    public TranscriptionService(TranscriptionRepository repo){ this.repo = repo; }

    public String transcribeAudio(MultipartFile file) throws Exception{


        String transcricaotxt = WhisperCliTranscriber.transcribeWithArgs(file, "--model", "small");

        boolean secureAudio = true;

        if (transcricaotxt.contains("pix")
            || transcricaotxt.contains("senha")
            || transcricaotxt.contains("banco")
        ) {
            secureAudio = false;
        }
        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("transcricaotxt", transcricaotxt);
        jsonMap.put("secureAudio", secureAudio);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(jsonMap);        
        
    }
}
