package com.main.triviatreckapp.config;

import com.main.triviatreckapp.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Appelé dès qu'une session est fermée (y compris pour heart-beat manquant).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DisconnectListener {

    private final RoomService roomService;   // votre logique métier

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String username = sha.getUser() != null ? sha.getUser().getName() : null;
        System.out.println("Dans onDisconnect");

        if (username != null) {
            log.info("Déconnexion (heartbeat manquant ?) pour {}", username);
//            roomService.removePlayerFromRoom(username); // fonction exécutée
        }
    }
}

