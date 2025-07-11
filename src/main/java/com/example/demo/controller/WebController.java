package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.service.AuthService;
import com.example.demo.service.ErpNextClientService;
import com.example.demo.service.OpenApiBuilderService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/api-docs")
public class WebController {

    private final ErpNextClientService erpNextClient;
    private final OpenApiBuilderService openApiBuilder;
    private final AuthService authService;

    @Value("${frappe.url}")
    private String baseUrl;

    public WebController(ErpNextClientService erpNextClient, OpenApiBuilderService openApiBuilder, AuthService authService) {
        this.erpNextClient = erpNextClient;
        this.openApiBuilder = openApiBuilder;
        this.authService = authService;
    }

    @GetMapping
    public String listModules(Model model, HttpSession session) {
        if (!authService.isAuthenticated(session)) {
            return "redirect:/login";
        }

        try {
            List<String> modules = erpNextClient.getModules();
            model.addAttribute("modules", modules);
            model.addAttribute("title", "Documentation API ERPNext");
            model.addAttribute("baseUrl", baseUrl);
            return "pages/documentation/modules";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement des modules: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/module/{moduleName}")
    public String moduleDocumentation(@PathVariable String moduleName, Model model, HttpSession session) {
        if (!authService.isAuthenticated(session)) {
            return "redirect:/login";
        }

        try {
            List<String> doctypes = erpNextClient.getDoctypesByModule(moduleName);
            model.addAttribute("moduleName", moduleName);
            model.addAttribute("doctypes", doctypes);
            model.addAttribute("swaggerUrl", "/api/openapi/" + moduleName + ".yaml");
            model.addAttribute("baseUrl", baseUrl);
            return "pages/documentation/swagger-ui";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement du module " + moduleName + ": " + e.getMessage());
            return "error";
        }
    }
}
