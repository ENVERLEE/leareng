package com.leareng.repository;

import com.leareng.entity.SubscriptionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRequestRepository extends JpaRepository<SubscriptionRequest, Long> {
    List<SubscriptionRequest> findByStatus(String status);
    List<SubscriptionRequest> findByEmailAndStatus(String email, String status);
}

