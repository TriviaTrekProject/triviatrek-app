package com.main.triviatreckapp.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload utilisé lors du lancement d'une partie.
 * Contient l'identifiant de la room et le user qui démarre la partie.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartGameRequest {
    private String roomId;
    private Long participantId;
}
