package com.example.transcription.model;
import jakarta.persistence.*;
@Entity
public class Transcription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String originalFileName;
    @Column(columnDefinition="TEXT")
    private String text;

    public Transcription(){}
    public Transcription(String file, String text){ this.originalFileName=file; this.text=text; }

    public Long getId(){ return id; }
    public String getOriginalFileName(){ return originalFileName; }
    public void setOriginalFileName(String name){ this.originalFileName=name; }
    public String getText(){ return text; }
    public void setText(String t){ this.text=t; }
}
