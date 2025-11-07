package com.leareng.service;

import com.leareng.entity.Passage;
import com.leareng.repository.PassageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PassageService {
    
    @Autowired
    private PassageRepository passageRepository;
    
    public Passage savePassage(String title, String text) {
        Passage passage = new Passage();
        passage.setTitle(title);
        passage.setText(text);
        return passageRepository.save(passage);
    }
    
    public List<Passage> getAllPassages() {
        return passageRepository.findAll();
    }
    
    public Optional<Passage> getPassageById(Long id) {
        return passageRepository.findById(id);
    }
}

