package com.aozturk.trace;

import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
public class TraceWeb {


    @RequestMapping("/")
    @NewSpan
    public String call() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject("http://mocktarget.apigee.net/", String.class);
    }
}