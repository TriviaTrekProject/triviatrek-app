package com.main.triviatreckapp.service;

import com.main.triviatreckapp.dto.RoomDTO;
import com.main.triviatreckapp.entities.Message;
import com.main.triviatreckapp.entities.Participant;
import com.main.triviatreckapp.entities.QuizGame;
import com.main.triviatreckapp.entities.Room;
import com.main.triviatreckapp.repository.ParticipantRepository;
import com.main.triviatreckapp.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test") // Active application-test.properties
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private Participant testParticipant;
    private Message testMessage;
    private String roomId;
    private String username;

    @BeforeEach
    void setUp() {
        roomId = "test-room-id";
        username = "testUser";

        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomId(roomId);
        testRoom.setActiveGame(false);
        testRoom.setParticipants(new ArrayList<>());
        testRoom.setMessages(new ArrayList<>());

        // Create test participant
        testParticipant = new Participant();
        testParticipant.setId(1L);
        testParticipant.setUsername(username);
        testParticipant.setDelaiReponse(0);

        // Create test message
        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setContent("Test message");
        testMessage.setSender(username);
        testMessage.setCreatedAt(Instant.now());
        testMessage.setRoom(testRoom);
        testMessage.setTimestamp(LocalDateTime.now());
    }

    @Test
    void getRoom_shouldReturnRoom_whenRoomExists() {
        // Arrange
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        // Act
        Optional<Room> result = roomService.getRoom(roomId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(roomId, result.get().getRoomId());
        verify(roomRepository).findByRoomId(roomId);
    }

    @Test
    void getRoom_shouldReturnEmpty_whenRoomDoesNotExist() {
        // Arrange
        when(roomRepository.findByRoomId(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<Room> result = roomService.getRoom("non-existent-room");

        // Assert
        assertTrue(result.isEmpty());
        verify(roomRepository).findByRoomId("non-existent-room");
    }

    @Test
    void getOrCreateRoom_shouldReturnExistingRoom_whenRoomExists() {
        // Arrange
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        // Act
        Room result = roomService.getOrCreateRoom(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        verify(roomRepository).findByRoomId(roomId);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void getOrCreateRoom_shouldCreateAndReturnNewRoom_whenRoomDoesNotExist() {
        // Arrange
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.empty());
        when(roomRepository.existsByRoomId(roomId)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // Act
        Room result = roomService.getOrCreateRoom(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        verify(roomRepository).findByRoomId(roomId);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void deleteRoom_shouldCallRepositoryMethod() {
        // Act
        roomService.deleteRoom(roomId);

        // Assert
        verify(roomRepository).deleteByRoomId(roomId);
    }

    @Test
    void saveRoom_shouldCallRepositoryMethod() {
        // Arrange
        when(roomRepository.save(testRoom)).thenReturn(testRoom);

        // Act
        Room result = roomService.saveRoom(testRoom);

        // Assert
        assertNotNull(result);
        assertEquals(testRoom, result);
        verify(roomRepository).save(testRoom);
    }

    @Test
    void removeParticipant_shouldRemoveParticipantFromRoom() {
        // Arrange
        testRoom.addParticipant(testParticipant);
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // Act
        roomService.removeParticipant(roomId, username);

        // Assert
        assertTrue(testRoom.getParticipants().isEmpty());
        verify(roomRepository).findByRoomId(roomId);
        verify(roomRepository).save(testRoom);
    }

    @Test
    void removeParticipant_shouldThrowException_whenRoomDoesNotExist() {
        // Arrange
        when(roomRepository.findByRoomId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> roomService.removeParticipant("non-existent-room", username));
        verify(roomRepository).findByRoomId("non-existent-room");
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void createRoom_shouldCreateAndReturnNewRoom() {
        // Arrange
        when(roomRepository.existsByRoomId(roomId)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // Act
        Room result = roomService.createRoom(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        verify(roomRepository).existsByRoomId(roomId);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createRoom_shouldThrowException_whenRoomAlreadyExists() {
        // Arrange
        when(roomRepository.existsByRoomId(roomId)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> roomService.createRoom(roomId));
        verify(roomRepository).existsByRoomId(roomId);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void convertRoomToDTO_shouldConvertRoomToDTO() {
        // Arrange
        testRoom.addParticipant(testParticipant);
        testRoom.getMessages().add(testMessage);
        QuizGame quizGame = new QuizGame();
        String gameId = UUID.randomUUID().toString();
        quizGame.setGameId(gameId);
        testRoom.setQuizGame(quizGame);

        // Act
        RoomDTO result = roomService.convertRoomToDTO(testRoom);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals(1, result.getParticipants().size());
        assertEquals(username, result.getParticipants().getFirst().getUsername());
        assertEquals(1, result.getMessages().size());
        assertEquals("Test message", result.getMessages().getFirst().getContent());
        assertEquals(gameId, result.getGameId());
        assertFalse(result.isActiveGame());
    }

    @Test
    void getUniqueUserName_shouldReturnOriginalName_whenNoConflict() {
        // Arrange
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        // Act
        String result = roomService.getUniqueUserName(roomId, "newUser");

        // Assert
        assertEquals("newUser", result);
        verify(roomRepository).findByRoomId(roomId);
    }

    @Test
    void getUniqueUserName_shouldReturnModifiedName_whenConflictExists() {
        // Arrange
        testRoom.addParticipant(testParticipant);
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        // Act
        String result = roomService.getUniqueUserName(roomId, username);

        // Assert
        assertEquals(username + "(2)", result);
        verify(roomRepository).findByRoomId(roomId);
    }

    @Test
    void addUserToRoom_shouldAddUserToExistingRoom() {
        // Arrange
        String tempUuid = UUID.randomUUID().toString();
        when(roomRepository.findByRoomIdForUpdate(roomId)).thenReturn(Optional.of(testRoom));
        when(participantRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenReturn(testParticipant);
        when(chatService.saveMessage(eq(roomId), eq("SYSTEM"), anyString())).thenReturn(testMessage);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // Act
        RoomDTO result = roomService.addUserToRoom(roomId, username, tempUuid);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals(1, result.getParticipants().size());
        assertEquals(username, result.getParticipants().getFirst().getUsername());
        assertEquals(tempUuid, result.getParticipants().getFirst().getTempId());
        verify(roomRepository).findByRoomIdForUpdate(roomId);
        verify(participantRepository).findByUsername(username);
        verify(participantRepository).save(any(Participant.class));
        verify(chatService).saveMessage(eq(roomId), eq("SYSTEM"), anyString());
        verify(roomRepository).save(testRoom);
    }

    @Test
    void addUserToRoom_shouldCreateRoomAndAddUser_whenRoomDoesNotExist() {
        // Arrange
        String tempUuid = UUID.randomUUID().toString();
        when(roomRepository.findByRoomIdForUpdate(roomId)).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(participantRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenReturn(testParticipant);
        when(chatService.saveMessage(eq(roomId), eq("SYSTEM"), anyString())).thenReturn(testMessage);

        // Act
        RoomDTO result = roomService.addUserToRoom(roomId, username, tempUuid);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals(1, result.getParticipants().size());
        assertEquals(username, result.getParticipants().getFirst().getUsername());
        assertEquals(tempUuid, result.getParticipants().getFirst().getTempId());
        verify(roomRepository).findByRoomIdForUpdate(roomId);
        verify(roomRepository, times(2)).save(any(Room.class));
        verify(participantRepository).findByUsername(username);
        verify(participantRepository).save(any(Participant.class));
        verify(chatService).saveMessage(eq(roomId), eq("SYSTEM"), anyString());
    }

    @Test
    void submitMessageToRoom_shouldAddMessageToRoom() {
        // Arrange
        Message newMessage = new Message();
        newMessage.setSender(username);
        newMessage.setContent("New message");

        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));
        when(chatService.saveMessage(roomId, username, "New message")).thenReturn(newMessage);
        when(roomRepository.save(testRoom)).thenReturn(testRoom);

        // Act
        RoomDTO result = roomService.submitMessageToRoom(roomId, newMessage);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        verify(roomRepository).findByRoomId(roomId);
        verify(chatService).saveMessage(roomId, username, "New message");
        verify(roomRepository).save(testRoom);
    }

    @Test
    void submitMessageToRoom_shouldThrowException_whenRoomDoesNotExist() {
        // Arrange
        Message newMessage = new Message();
        newMessage.setSender(username);
        newMessage.setContent("New message");

        when(roomRepository.findByRoomId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> roomService.submitMessageToRoom("non-existent-room", newMessage));
        verify(roomRepository).findByRoomId("non-existent-room");
        verify(chatService, never()).saveMessage(anyString(), anyString(), anyString());
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void removeParticipantAndCheckRoomStatus_shouldRemoveParticipantAndReturnUpdatedRoom() {
        // Arrange
        testRoom.addParticipant(testParticipant);

        // Add a second participant so the room isn't empty after removal
        Participant secondParticipant = new Participant();
        secondParticipant.setId(2L);
        secondParticipant.setUsername("secondUser");
        secondParticipant.setDelaiReponse(0);
        testRoom.addParticipant(secondParticipant);

        when(roomRepository.findByRoomIdForUpdate(roomId)).thenReturn(Optional.of(testRoom));
        when(chatService.saveMessage(eq(roomId), eq("SYSTEM"), anyString())).thenReturn(testMessage);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // Act
        Optional<RoomDTO> result = roomService.removeParticipantAndCheckRoomStatus(roomId, username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(roomId, result.get().getRoomId());
        assertEquals(1, testRoom.getParticipants().size());
        assertEquals("secondUser", testRoom.getParticipants().getFirst().getUsername());
        verify(roomRepository).findByRoomIdForUpdate(roomId);
        verify(chatService).saveMessage(eq(roomId), eq("SYSTEM"), anyString());
        verify(roomRepository).save(testRoom);
        verify(roomRepository, never()).deleteByRoomId(anyString());
    }

    @Test
    void removeParticipantAndCheckRoomStatus_shouldDeleteRoomWhenEmpty() {
        // Arrange
        testRoom.addParticipant(testParticipant);
        when(roomRepository.findByRoomIdForUpdate(roomId)).thenReturn(Optional.of(testRoom));
        when(chatService.saveMessage(eq(roomId), eq("SYSTEM"), anyString())).thenReturn(testMessage);
        doNothing().when(roomRepository).deleteByRoomId(roomId);

        // Act
        Optional<RoomDTO> result = roomService.removeParticipantAndCheckRoomStatus(roomId, username);

        // Assert
        assertTrue(result.isEmpty());
        verify(roomRepository).findByRoomIdForUpdate(roomId);
        verify(chatService).saveMessage(eq(roomId), eq("SYSTEM"), anyString());
        verify(roomRepository).deleteByRoomId(roomId);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void removeParticipantAndCheckRoomStatus_shouldReturnEmpty_whenRoomDoesNotExist() {
        // Arrange
        when(roomRepository.findByRoomIdForUpdate(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<RoomDTO> result = roomService.removeParticipantAndCheckRoomStatus("non-existent-room", username);

        // Assert
        assertTrue(result.isEmpty());
        verify(roomRepository).findByRoomIdForUpdate("non-existent-room");
        verify(chatService, never()).saveMessage(anyString(), anyString(), anyString());
        verify(roomRepository, never()).save(any(Room.class));
        verify(roomRepository, never()).deleteByRoomId(anyString());
    }

    @Test
    void getRoomDTO_shouldReturnRoomDTO_whenRoomExists() {
        // Arrange
        when(roomRepository.findByRoomId(roomId)).thenReturn(Optional.of(testRoom));

        // Act
        RoomDTO result = roomService.getRoomDTO(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        verify(roomRepository).findByRoomId(roomId);
    }

    @Test
    void getRoomDTO_shouldThrowException_whenRoomDoesNotExist() {
        // Arrange
        when(roomRepository.findByRoomId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> roomService.getRoomDTO("non-existent-room"));
        verify(roomRepository).findByRoomId("non-existent-room");
    }
}
