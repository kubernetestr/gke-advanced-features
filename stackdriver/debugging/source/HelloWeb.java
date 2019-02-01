package com.aozturk.hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Random;


@RestController
public class HelloWeb {



    @RequestMapping("/")
    public String home() {
        Random rand = new Random();
        int n = rand.nextInt(50);
        n = n * 2;
        return "Lucky number: " + n;
    }
}