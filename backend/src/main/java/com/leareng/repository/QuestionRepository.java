package com.leareng.repository;

import com.leareng.entity.Passage;
import com.leareng.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByPassage(Passage passage);
    boolean existsByPassage(Passage passage);
}

