package com.example.transcription.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.transcription.model.Transcription;
public interface TranscriptionRepository extends JpaRepository<Transcription, Long>{}
