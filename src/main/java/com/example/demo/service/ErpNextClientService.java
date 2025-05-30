/** 
 *  Author : Ny ANTSA
 */
package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class ErpNextClientService {

    @Value("${frappe.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public ErpNextClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders buildSessionHeaders() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        System.out.println("=== Building Headers ===");
        
        String authHeader = getAuthorizationHeader();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            headers.add("Authorization", "Bearer " + token);
            System.out.println(" Utilisation Bearer token: " + token.substring(0, Math.min(8, token.length())) + "...");
            return headers;
        }
        
        String cookieHeader = getCookieHeaderFromRequest();
        if (cookieHeader != null) {
            headers.add("Cookie", cookieHeader);
            System.out.println(" Utilisation Cookie header: " + cookieHeader);
            return headers;
        }

        String sessionCookie = getSessionCookie();
        if (sessionCookie != null) {
            headers.add("Cookie", sessionCookie);
            System.out.println(" Utilisation Cookie session: " + sessionCookie);
            return headers;
        }
        
        System.out.println("Aucune authentification trouvée");
        throw new Exception("Session ERPNext non authentifiée. Veuillez vous connecter d'abord.");
    }
    
    private String getAuthorizationHeader() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader("Authorization");
                System.out.println("Auth header trouvé: " + (authHeader != null ? "Oui" : "Non"));
                return authHeader;
            }
        } catch (Exception e) {
            System.out.println("Erreur Auth header: " + e.getMessage());
        }
        return null;
    }
    
    private String getCookieHeaderFromRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String cookieHeader = request.getHeader("Cookie");
                System.out.println("Cookie header brut: " + cookieHeader);
                
                if (cookieHeader != null && cookieHeader.contains("sid=")) {
                    return cookieHeader;
                }
                
                if (request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                        if ("sid".equals(cookie.getName())) {
                            String sidCookie = "sid=" + cookie.getValue();
                            System.out.println("Cookie SID trouvé: " + sidCookie);
                            return sidCookie;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur Cookie header: " + e.getMessage());
        }
        return null;
    }
    
    private String getSessionCookie() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    Object sidObj = session.getAttribute("frappe_sid");
                    if (sidObj != null) {
                        String sidValue = sidObj.toString();
                        return "sid=" + sidValue;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur Session: " + e.getMessage());
        }
        return null;
    }
        

    public List<Map<String, Object>> listDocuments(String doctype, String filtersJson) throws Exception {
        String url = baseUrl + "/api/resource/" + doctype;
        if (filtersJson != null && !filtersJson.isEmpty()) {
            url += "?filters=" + filtersJson;
        }
        
        HttpHeaders headers = buildSessionHeaders();
        System.out.println("=== Appel API ===");
        System.out.println("URL: " + url);
        System.out.println("Headers: " + headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            System.out.println(" Succès: " + data.size() + " éléments récupérés");
            return data;
        } catch (HttpClientErrorException e) {
            System.out.println("Erreur HTTP: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new Exception("Erreur lors de l'appel à ERPNext: " + e.getMessage() + " - " + e.getResponseBodyAsString());
        }
    }

    public List<String> getModules() throws Exception {
        String url = baseUrl + "/api/resource/Module Def";
        HttpHeaders headers = buildSessionHeaders();
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            return data.stream().map(d -> (String) d.get("name")).toList();
        } catch (HttpClientErrorException e) {
            throw new Exception("Erreur lors de la récupération des modules: " + e.getMessage());
        }
    }

    public List<String> getDoctypesByModule(String module) throws Exception {
        String url = baseUrl + "/api/resource/DocType?filters=[[\"module\",\"=\",\"" + module + "\"]]";
        HttpHeaders headers = buildSessionHeaders();
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            return data.stream().map(d -> (String) d.get("name")).toList();
        } catch (HttpClientErrorException e) {
            throw new Exception("Erreur lors de la récupération des doctypes: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getDocument(String doctype, String id) throws Exception {
        String url = baseUrl + "/api/resource/" + doctype + "/" + id;
        HttpHeaders headers = buildSessionHeaders();
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return (Map<String, Object>) response.getBody().get("data");
        } catch (HttpClientErrorException e) {
            throw new Exception("Erreur lors de la récupération du document: " + e.getMessage());
        }
    }
    
    public Map<String, Object> createDocument(String doctype, Map<String, Object> documentData) throws Exception {
        String url = baseUrl + "/api/resource/" + doctype;
        HttpHeaders headers = buildSessionHeaders();
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(documentData, headers), Map.class);
            return (Map<String, Object>) response.getBody().get("data");
        } catch (HttpClientErrorException e) {
            throw new Exception("Erreur lors de la création du document: " + e.getMessage());
        }
    }
    
    public Map<String, Object> updateDocument(String doctype, String id, Map<String, Object> documentData) throws Exception {
        String url = baseUrl + "/api/resource/" + doctype + "/" + id;
        HttpHeaders headers = buildSessionHeaders();
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(documentData, headers), Map.class);
            return (Map<String, Object>) response.getBody().get("data");
        } catch (HttpClientErrorException e) {
            throw new Exception("Erreur lors de la mise à jour du document: " + e.getMessage());
        }
    }
    
    public void deleteDocument(String doctype, String id) throws Exception {
        String url = baseUrl + "/api/resource/" + doctype + "/" + id;
        HttpHeaders headers = buildSessionHeaders();
        
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (HttpClientErrorException e) {
            throw new Exception("Erreur lors de la suppression du document: " + e.getMessage());
        }
    }
}