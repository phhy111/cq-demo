package edu.cqie.cqdemo.ai.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TravelTemplateService {

    public List<Map<String, String>> getAllTemplates() {
        return Arrays.stream(TravelTemplate.values())
                .filter(t -> t != TravelTemplate.CUSTOM)
                .map(template -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("code", template.getCode());
                    info.put("name", template.getName());
                    info.put("description", getTemplateDescription(template));
                    return info;
                })
                .collect(Collectors.toList());
    }

    public String getTemplatePrompt(String templateCode) {
        TravelTemplate template = TravelTemplate.fromCode(templateCode);
        if (template == TravelTemplate.CUSTOM) {
            log.warn("未找到模板: {}", templateCode);
            return null;
        }
        return template.getPromptTemplate();
    }

    public String buildMessageWithTemplate(String templateCode, String userMessage) {
        TravelTemplate template = TravelTemplate.fromCode(templateCode);
        
        if (template == TravelTemplate.CUSTOM) {
            return userMessage;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(template.getPromptTemplate());
        sb.append("\n\n用户补充要求：");
        sb.append(userMessage);
        
        log.info("使用模板 [{}] 构建消息", template.getName());
        return sb.toString();
    }

    private String getTemplateDescription(TravelTemplate template) {
        switch (template) {
            case BUDGET:
                return "适合学生党和预算有限的旅行者，推荐免费景点和低价美食";
            case LUXURY:
                return "适合追求品质的旅行者，推荐高端酒店和精致美食";
            case FAMILY:
                return "适合带孩子出行的家庭，推荐亲子互动项目和安全景点";
            case COUPLE:
                return "适合情侣出游，推荐浪漫景点和约会餐厅";
            case CULTURAL:
                return "适合文化爱好者，深入了解重庆的历史和文化";
            case FOOD:
                return "适合美食爱好者，探索地道的重庆美食";
            case PHOTO:
                return "适合摄影爱好者，打卡最佳拍摄地点";
            default:
                return "";
        }
    }
}
