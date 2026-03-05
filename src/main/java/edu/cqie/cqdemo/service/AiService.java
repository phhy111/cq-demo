// edu.cqie.cqdemo.service.AiService.java
package edu.cqie.cqdemo.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import edu.cqie.cqdemo.entity.AiReport;
import reactor.core.publisher.Flux;

public interface AiService {

    AiReport generateTravelPlan(@MemoryId Long memoryId, @UserMessage String userMessage);


}