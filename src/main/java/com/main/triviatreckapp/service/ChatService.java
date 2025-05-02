package com.main.triviatreckapp.service;


import com.main.triviatreckapp.dto.MessageDTO;
import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.MessageRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {
    private final MessageRepository msgRepo;
    private final RoomRepository roomRepo;

    public ChatService(MessageRepository msgRepo, RoomRepository roomRepo) { this.msgRepo = msgRepo;
        this.roomRepo = roomRepo;
    }

    public Message saveMessage(String roomId, String sender, String content) {
        System.out.println("save message");
        System.out.println(roomId);
        Room room = roomRepo.findByRoomId(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        Message msg = new Message();
        msg.setRoom(room);
        msg.setSender(sender);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        return msgRepo.save(msg);
    }

    public List<Message> getHistory(Long roomId) {
        return msgRepo.findByRoom_IdOrderByTimestampAsc(roomId);
    }
}
