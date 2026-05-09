package edu.cqie.cqdemo.service;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensitiveWordService {

    @Autowired
    private SensitiveWordBs sensitiveWordBs;

    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return sensitiveWordBs.contains(text);
    }

    public String replaceSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return sensitiveWordBs.replace(text);
    }

    public List<String> findSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return sensitiveWordBs.findAll(text);
    }
}
