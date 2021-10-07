package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaController {

    @Autowired
    private KafkaPublisher kafkaPublisher;

    @Autowired
    private KafkaConsumer kafkaConsumer;

    @PostMapping("kafka/publish")
    public String publishMessage(@RequestParam String message){
        try {
            kafkaPublisher.sendMessage("twitch",message);
            return "Yes";
        }catch (Exception e ){
            return "No";
        }
    }

    @GetMapping("kafka/receive")
    public String receiveMessage(@RequestParam String message){
        try {
            kafkaConsumer.listenGroupFoo(message);
            return "Yes";
        }catch (Exception e ){
            return "No";
        }
    }

}
