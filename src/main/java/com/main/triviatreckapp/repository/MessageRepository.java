package com.main.triviatreckapp.repository;

import com.main.triviatreckapp.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByRoom_IdOrderByTimestampAsc(Long roomId);

}