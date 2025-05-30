package com.example.demo.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ErpNextClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/resource")
@Tag(name = "ERPNext Resources", description = "API pour accéder aux ressources ERPNext")
@SecurityRequirement(name = "sessionAuth")
public class ErpNextProxyController {

    private final ErpNextClientService erpNextClient;

    public ErpNextProxyController(ErpNextClientService erpNextClient) {
        this.erpNextClient = erpNextClient;
    }

    @GetMapping("/{doctype}")
    @Operation(
        summary = "Lister tous les documents d'un type",
        description = "Récupère la liste de tous les documents du type spécifié"
    )
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    @ApiResponse(responseCode = "401", description = "Non authentifié")
    public ResponseEntity<List<Map<String, Object>>> listDocuments(
            @PathVariable String doctype,
            @Parameter(description = "Filtres JSON optionnels")
            @RequestParam(required = false) String filters) {
        
        try {
            String decodedDoctype = URLDecoder.decode(doctype, StandardCharsets.UTF_8);
            
            List<Map<String, Object>> documents = erpNextClient.listDocuments(decodedDoctype, filters);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des documents: " + e.getMessage());
            return ResponseEntity.status(403).body(null);
        }
    }

    @GetMapping("/{doctype}/{id}")
    @Operation(
        summary = "Récupérer un document spécifique",
        description = "Récupère un document par son ID"
    )
    @ApiResponse(responseCode = "200", description = "Document récupéré avec succès")
    @ApiResponse(responseCode = "404", description = "Document non trouvé")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    public ResponseEntity<Map<String, Object>> getDocument(
            @PathVariable String doctype,
            @PathVariable String id) {
        
        try {
            String decodedDoctype = URLDecoder.decode(doctype, StandardCharsets.UTF_8);
            String decodedId = URLDecoder.decode(id, StandardCharsets.UTF_8);
            
            Map<String, Object> document = erpNextClient.getDocument(decodedDoctype, decodedId);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du document: " + e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    @PostMapping("/{doctype}")
    @Operation(
        summary = "Créer un nouveau document",
        description = "Crée un nouveau document du type spécifié"
    )
    @ApiResponse(responseCode = "201", description = "Document créé avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    public ResponseEntity<Map<String, Object>> createDocument(
            @PathVariable String doctype,
            @RequestBody Map<String, Object> documentData) {
        
        try {
            String decodedDoctype = URLDecoder.decode(doctype, StandardCharsets.UTF_8);
            
            Map<String, Object> createdDocument = erpNextClient.createDocument(decodedDoctype, documentData);
            return ResponseEntity.status(201).body(createdDocument);
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du document: " + e.getMessage());
            return ResponseEntity.status(400).body(null);
        }
    }

    @PutMapping("/{doctype}/{id}")
    @Operation(
        summary = "Mettre à jour un document",
        description = "Met à jour un document existant"
    )
    @ApiResponse(responseCode = "200", description = "Document mis à jour avec succès")
    @ApiResponse(responseCode = "404", description = "Document non trouvé")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    public ResponseEntity<Map<String, Object>> updateDocument(
            @PathVariable String doctype,
            @PathVariable String id,
            @RequestBody Map<String, Object> documentData) {
        
        try {
            String decodedDoctype = URLDecoder.decode(doctype, StandardCharsets.UTF_8);
            String decodedId = URLDecoder.decode(id, StandardCharsets.UTF_8);
            
            Map<String, Object> updatedDocument = erpNextClient.updateDocument(decodedDoctype, decodedId, documentData);
            return ResponseEntity.ok(updatedDocument);
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du document: " + e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    @DeleteMapping("/{doctype}/{id}")
    @Operation(
        summary = "Supprimer un document",
        description = "Supprime un document existant"
    )
    @ApiResponse(responseCode = "204", description = "Document supprimé avec succès")
    @ApiResponse(responseCode = "404", description = "Document non trouvé")
    @ApiResponse(responseCode = "403", description = "Accès refusé")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String doctype,
            @PathVariable String id) {
        
        try {
            String decodedDoctype = URLDecoder.decode(doctype, StandardCharsets.UTF_8);
            String decodedId = URLDecoder.decode(id, StandardCharsets.UTF_8);
            
            erpNextClient.deleteDocument(decodedDoctype, decodedId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du document: " + e.getMessage());
            return ResponseEntity.status(404).build();
        }
    }
}