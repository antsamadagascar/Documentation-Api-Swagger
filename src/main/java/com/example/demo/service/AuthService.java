package com.example.demo.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta                                                                                                                                                                                                                                                                                                                                                                                                                          .servlet.http.HttpSession;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Value("${frappe.url:http://erpnext.localhost:8000/}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public AuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> login(String usr, String pwd, HttpSession session) throws Exception {
        String url = baseUrl + "/api/method/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    
        Map<String, String> body = new HashMap<>();
        body.put("usr", usr);
        body.put("pwd", pwd);
    
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            logger.info("Réponse de connexion: {}, En-têtes: {}", response.getBody(), response.getHeaders());
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = new HashMap<>();
                Optional.ofNullable(response.getHeaders().get("Set-Cookie"))
                    .flatMap(cookies -> cookies.stream()
                        .filter(cookie -> cookie.startsWith("sid="))
                        .map(cookie -> cookie.split(";")[0].split("=")[1])
                        .findFirst())
                    .ifPresent(sid -> {
                        session.setAttribute("frappe_sid", sid);
                        result.put("sid", sid);
                        logger.info("sid extrait et stocké: {}", sid);
                    });
    
                Optional.ofNullable(response.getBody())
                    .map(bodyMap -> bodyMap.get("full_name"))
                    .ifPresent(fullName -> result.put("full_name", fullName));
    
                if (!result.containsKey("sid")) {
                    logger.error("Aucun sid trouvé dans la réponse de connexion");
                    throw new Exception("Échec de l'extraction du sid");
                }
                return result;
            } else {
                logger.warn("Échec de la connexion pour l'utilisateur {} : statut {}", usr, response.getStatusCode());
                throw new Exception("Identifiants invalides");
            }
        } catch (HttpClientErrorException e) {
            logger.error("Erreur HTTP lors de la connexion pour l'utilisateur {} : {} - {}", usr, e.getStatusCode(), e.getResponseBodyAsString());
            throw new Exception("Erreur de connexion à ERPNext : " + e.getStatusCode() + " - " + e.getStatusText());
        }
    }

    public void logout(String sid) throws Exception {
        String url = baseUrl + "/api/method/logout";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + sid);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.GET, request, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Échec de la déconnexion pour sid {} : statut {}", sid, response.getStatusCode());
                throw new Exception("Échec de la déconnexion, statut : " + response.getStatusCode());
            }
            logger.info("Déconnexion réussie pour sid : {}", sid);
        } catch (HttpClientErrorException e) {
            logger.error("Erreur HTTP lors de la déconnexion pour sid {} : {}", sid, e.getMessage(), e);
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new Exception("Session ERPNext invalide ou expirée");
            }
            throw new Exception("Erreur lors de la déconnexion : " + e.getStatusCode() + " - " + e.getStatusText());
        }
    }

    public Map<String, Object> getLoggedUser(String sid) throws Exception {
        String url = baseUrl + "/api/method/frappe.auth.get_logged_user";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sid=" + sid);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.warn("Échec de la récupération de l'utilisateur pour sid {} : statut {}", sid, response.getStatusCode());
                throw new Exception("Session expirée");
            }
        } catch (HttpClientErrorException e) {
            logger.error("Erreur HTTP lors de la récupération de l'utilisateur pour sid {} : {}", sid, e.getMessage(), e);
            throw new Exception("Erreur lors de la récupération de l'utilisateur : " + e.getStatusCode() + " - " + e.getStatusText());
        }
    }

    public boolean isAuthenticated(HttpSession session) {
        return session.getAttribute("frappe_sid") != null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("content", "pages/dashboard/dashboard");
        return "layout/layout"; 
    }
}