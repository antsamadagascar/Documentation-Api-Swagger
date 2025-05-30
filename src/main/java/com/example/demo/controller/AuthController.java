/**  
 * creer par Ny Antsa  
 
**/
package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "pages/auth/index";
    }

    @PostMapping("/api/auth/login")
    public String login(@RequestBody Map<String, String> credentials, HttpSession session, RedirectAttributes redirectAttributes) {
        String usr = credentials.get("usr");
        String pwd = credentials.get("pwd");

        if (usr == null || pwd == null) {
            redirectAttributes.addFlashAttribute("error", "Nom d'utilisateur et mot de passe requis");
            return "redirect:/login";
        }

        try {
            Map<String, Object> response = authService.login(usr, pwd, session);
            if (response.containsKey("sid")) {
                session.setAttribute("frappe_sid", response.get("sid"));
                session.setAttribute("user_full_name", response.get("full_name"));
                return "redirect:/api-docs";
            } else {
                redirectAttributes.addFlashAttribute("error", "Cookie d'authentification non trouvé");
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur serveur : " + e.getMessage());
            return "redirect:/login";
        }
    }


    @PostMapping("/api/auth/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!authService.isAuthenticated(session)) {
            return "redirect:/login";
        }

        try {
            String sid = (String) session.getAttribute("frappe_sid");
            authService.logout(sid);
            session.removeAttribute("frappe_sid");
            session.removeAttribute("user_full_name");
            redirectAttributes.addFlashAttribute("success", "Déconnexion réussie");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la déconnexion : " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/api/auth/user")
    @ResponseBody
    public ResponseEntity<?> getLoggedUser(HttpSession session) {
        if (!authService.isAuthenticated(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String sid = (String) session.getAttribute("frappe_sid");
            Map<String, Object> user = authService.getLoggedUser(sid);
            return ResponseEntity.ok(user);
        } catch (Exception e) {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
            session.removeAttribute("frappe_sid");
            session.removeAttribute("user_full_name");
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
        }
    }

}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   