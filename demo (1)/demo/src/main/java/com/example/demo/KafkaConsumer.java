package com.example.demo;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {


    @KafkaListener(topics = "twitch", groupId = "myGroup")
    public void listenGroupFoo(String message) {
        System.out.println("Received Message in group myGroup: " + message);
    }
}
