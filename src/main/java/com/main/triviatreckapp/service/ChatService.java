package com.main.triviatreckapp.service;

import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.MessageRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChatService {
    private final MessageRepository msgRepo;
    private final RoomRepository roomRepo;

    public ChatService(MessageRepository msgRepo, RoomRepository roomRepo) { this.msgRepo = msgRepo;
        this.roomRepo = roomRepo;
    }

    @Transactional
    public Message saveMessage(String roomId, String sender, String content) {
        Room room = roomRepo.findByRoomId(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        Message msg = new Message();
        msg.setRoom(room);
        msg.setSender(sender);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        return msgRepo.save(msg);
    }

}
