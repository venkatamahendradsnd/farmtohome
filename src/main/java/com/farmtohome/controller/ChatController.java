package com.farmtohome.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.farmtohome.model.ChatMessage;
import com.farmtohome.model.ChatThread;
import com.farmtohome.model.User;
import com.farmtohome.repository.ChatMessageRepository;
import com.farmtohome.repository.ChatThreadRepository;
import com.farmtohome.repository.UserRepository;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatThreadRepository threadRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/threads")
    public ResponseEntity<?> listThreads(@RequestParam Long userId, @RequestParam String role) {
        // Ignore role for robustness; return all threads where this user participates
        return ResponseEntity.ok(threadRepository.findByFarmer_IdOrCustomer_IdOrderByUpdatedAtDesc(userId, userId));
    }

    @PostMapping("/thread")
    public ResponseEntity<?> getOrCreateThread(@RequestBody Map<String, Object> body) {
        Long farmerId = body.get("farmerId") == null ? null : Long.valueOf(body.get("farmerId").toString());
        Long customerId = body.get("customerId") == null ? null : Long.valueOf(body.get("customerId").toString());
        Long requesterId = body.get("requesterId") == null ? null : Long.valueOf(body.get("requesterId").toString());

        if (farmerId == null || customerId == null || requesterId == null) {
            return ResponseEntity.badRequest().body("farmerId, customerId and requesterId are required");
        }
        if (!requesterId.equals(farmerId) && !requesterId.equals(customerId)) {
            return ResponseEntity.badRequest().body("Access denied");
        }

        ChatThread t = threadRepository.findByFarmer_IdAndCustomer_Id(farmerId, customerId).orElse(null);
        if (t != null) return ResponseEntity.ok(t);

        User farmer = userRepository.findById(farmerId).orElseThrow(() -> new RuntimeException("Farmer not found"));
        User customer = userRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));

        ChatThread nt = new ChatThread();
        nt.setFarmer(farmer);
        nt.setCustomer(customer);
        nt.setCreatedAt(LocalDateTime.now());
        nt.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(threadRepository.save(nt));
    }

    @GetMapping("/messages/{threadId}")
    public ResponseEntity<?> listMessages(@PathVariable Long threadId, @RequestParam Long userId) {
        ChatThread t = threadRepository.findById(threadId).orElseThrow(() -> new RuntimeException("Thread not found"));
        if (!t.getFarmer().getId().equals(userId) && !t.getCustomer().getId().equals(userId)) {
            return ResponseEntity.badRequest().body("Access denied");
        }
        return ResponseEntity.ok(messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId));
    }

    @PostMapping("/messages/{threadId}")
    public ResponseEntity<?> sendMessage(@PathVariable Long threadId, @RequestBody Map<String, Object> body) {
        Long senderId = body.get("senderId") == null ? null : Long.valueOf(body.get("senderId").toString());
        String text = body.get("text") == null ? null : body.get("text").toString();
        String imageUrl = body.get("imageUrl") == null ? null : body.get("imageUrl").toString();

        if (senderId == null) return ResponseEntity.badRequest().body("senderId is required");
        if ((text == null || text.trim().isEmpty()) && (imageUrl == null || imageUrl.trim().isEmpty())) {
            return ResponseEntity.badRequest().body("Message text or imageUrl is required");
        }

        ChatThread t = threadRepository.findById(threadId).orElseThrow(() -> new RuntimeException("Thread not found"));
        if (!t.getFarmer().getId().equals(senderId) && !t.getCustomer().getId().equals(senderId)) {
            return ResponseEntity.badRequest().body("Access denied");
        }

        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));

        ChatMessage m = new ChatMessage();
        m.setThread(t);
        m.setSender(sender);
        m.setText(text == null ? null : text.trim());
        m.setImageUrl(imageUrl == null ? null : imageUrl.trim());
        m.setCreatedAt(LocalDateTime.now());

        t.setUpdatedAt(LocalDateTime.now());
        threadRepository.save(t);

        ChatMessage saved = messageRepository.save(m);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", saved);
        return ResponseEntity.ok(resp);
    }
}

