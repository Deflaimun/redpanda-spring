package com.example.demo;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class KafkaConsumer {


    @KafkaListener(topics = Constants.TOPIC, groupId = Constants.GROUP_ID)
    public void listenGroupFoo(String message) {
        System.out.println("Received Message in group myGroup: " + message);
    }
}
