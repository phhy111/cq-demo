package edu.cqie.cqdemo.config;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SensitiveWordConfig {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveWordConfig.class);

    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        logger.info("开始初始化敏感词组件...");

        SensitiveWordBs swBs = SensitiveWordBs.newInstance();
        
        swBs.init();

        logger.info("敏感词组件初始化完成，使用默认敏感词库");
        return swBs;
    }
}
