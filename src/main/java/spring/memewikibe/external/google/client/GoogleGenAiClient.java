package spring.memewikibe.external.google.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import spring.memewikibe.external.google.client.request.GenerateContentRequest;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;

@HttpExchange
public interface GoogleGenAiClient {

    @PostExchange("/{version}/models/{model}:generateContent")
    GenerateContentResponse generateContent(
        @PathVariable("version") String version,
        @PathVariable("model") String model,
        @RequestBody GenerateContentRequest request
    );
}
