package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.service.PersonerlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/personel")
public class PersonelController {
    @Autowired
    private PersonerlService personerlService;
    @PostMapping("/getusers")
    public String getUser(){
        System.out.println(personerlService.getUser().toString());
        return personerlService.getUser().toString();
    }
}
