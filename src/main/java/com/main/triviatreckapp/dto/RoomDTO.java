package com.main.triviatreckapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    public String roomId;
    public List<ParticipantDTO> participants;
    public List<MessageDTO> messages;
    public String gameId;
    public boolean activeGame;

}
