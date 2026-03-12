package com.farmtohome.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmtohome.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    void deleteByThreadId(Long threadId);
}

