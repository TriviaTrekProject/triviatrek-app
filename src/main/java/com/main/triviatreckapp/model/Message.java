package com.main.triviatreckapp.model;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Message {
    private String senderName;
    private String roomId;
    private String message;
    private Status status;

}
