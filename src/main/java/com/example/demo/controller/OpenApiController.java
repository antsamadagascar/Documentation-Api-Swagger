package com.example.demo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.OpenApiBuilderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@RestController
@RequestMapping("/api")
public class OpenApiController {
    
    private final OpenApiBuilderService builder;
    private final ObjectMapper yamlMapper;
    
    public OpenApiController(OpenApiBuilderService builder) {
        this.builder = builder;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * Documentation OpenAPI globale pour tous les modules
     * @throws Exception 
     */
    @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
    public String globalOpenApiYaml() throws Exception {
        Map<String, Object> doc = builder.buildOpenApi();
        return yamlMapper.writeValueAsString(doc);
    }
    
    /**
     * Documentation OpenAPI pour un module spécifique
     * @throws Exception 
     */
    @GetMapping(value = "/openapi/{module}.yaml", produces = "application/yaml")
    public String moduleOpenApiYaml(@PathVariable String module) throws Exception {
        Map<String, Object> doc = builder.buildOpenApiForModule(module);
        return yamlMapper.writeValueAsString(doc);
    }
    
    /**
     * Documentation OpenAPI en format JSON
     * @throws Exception 
     */
    @GetMapping(value = "/openapi.json", produces = "application/json")
    public Map<String, Object> globalOpenApiJson() throws Exception {
        return builder.buildOpenApi();
    }
    
    /**
     * Documentation OpenAPI JSON pour un module spécifique
     * @throws Exception 
     */
    @GetMapping(value = "/openapi/{module}.json", produces = "application/json")
    public Map<String, Object> moduleOpenApiJson(@PathVariable String module) throws Exception {
        return builder.buildOpenApiForModule(module);
    }
}