package com.main.triviatreckapp.dto;

import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    public String roomId;
    public List<String> participants;
    public List<MessageDTO> messages;

}
