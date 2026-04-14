package kz.arta.synergy.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class RestService {

    private final RestTemplate restTemplate;

    public RestService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public String getPostWithsSoapHeaders(String url, String xml) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.TEXT_XML));
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", "");

        HttpEntity request = new HttpEntity<>(xml,headers);

        ResponseEntity<String> response;
        try {
            response = this.restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return response.getBody();

        } catch (HttpServerErrorException e){
            return e.getResponseBodyAsString();
        } catch (Exception e){
            return e.getMessage();
        }
    }

}
