package com.leareng.repository;

import com.leareng.entity.Passage;
import com.leareng.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByPassage(Passage passage);
    
    @Query("SELECT COUNT(q) > 0 FROM Question q WHERE q.passage = :passage")
    boolean existsByPassage(@Param("passage") Passage passage);
}

