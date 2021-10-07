package com.example.demo.rest;

import com.example.demo.Constants;
import com.example.demo.KafkaPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaController {

    @Autowired
    private KafkaPublisher kafkaPublisher;

    @PostMapping("publish")
    public ResponseEntity<String> publishMessage(@RequestParam String message){
        try {
            kafkaPublisher.sendMessage(Constants.TOPIC,message);
            return ResponseEntity.ok(null);

        }catch (Exception e ){
            return ResponseEntity.internalServerError().body(e.getLocalizedMessage());
        }
    }

}
