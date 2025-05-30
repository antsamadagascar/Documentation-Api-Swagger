package com.example.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenApiBuilderService {

    private final ErpNextClientService client;

    @Value("${server.port:8080}")  // Port de votre application Spring Boot
    private String serverPort;

    public OpenApiBuilderService(ErpNextClientService client) {
        this.client = client;
    }

    public Map<String, Object> buildOpenApi() throws Exception {
        Map<String, Object> openapi = new LinkedHashMap<>();
        openapi.put("openapi", "3.0.0");
        openapi.put("info", createApiInfo("ERPNext Dynamic API", "Documentation complète de l'API ERPNext"));
        
        // CHANGEMENT IMPORTANT : Pointer vers votre serveur local au lieu d'ERPNext directement
        openapi.put("servers", List.of(
            Map.of("url", "http://localhost:" + serverPort + "/api", "description", "Serveur Spring Boot (Proxy)")
        ));
        
        openapi.put("security", List.of(Map.of("sessionAuth", List.of()), Map.of("bearerAuth", List.of())));
        Map<String, Object> paths = new LinkedHashMap<>();
        List<String> modules = client.getModules();
        for (String module : modules) {
            List<String> doctypes = client.getDoctypesByModule(module);
            addModulePaths(paths, module, doctypes);
        }
        openapi.put("paths", paths);
        openapi.put("components", createComponents());
        return openapi;
    }

    public Map<String, Object> buildOpenApiForModule(String moduleName) throws Exception {
        Map<String, Object> openapi = new LinkedHashMap<>();
        openapi.put("openapi", "3.0.0");
        openapi.put("info", createApiInfo("ERPNext API - Module " + moduleName, "Documentation API pour le module " + moduleName));
        
        // CHANGEMENT IMPORTANT : Pointer vers votre serveur local
        openapi.put("servers", List.of(
            Map.of("url", "http://localhost:" + serverPort + "/api", "description", "Serveur Spring Boot (Proxy)")
        ));
        
        Map<String, Object> paths = new LinkedHashMap<>();
        List<String> doctypes = client.getDoctypesByModule(moduleName);
        addModulePaths(paths, moduleName, doctypes);
        openapi.put("paths", paths);
        openapi.put("components", createComponents());
        return openapi;
    }

    private Map<String, Object> createApiInfo(String title, String description) {
        return Map.of(
            "title", title,
            "description", description + " (via Proxy Spring Boot)",
            "version", "1.0.0",
            "contact", Map.of("name", "Support API", "email", "antsamadagascar@gmail.com")
        );
    }

    private void addModulePaths(Map<String, Object> paths, String module, List<String> doctypes) {
        for (String doctype : doctypes) {
            String collectionPath = "/resource/" + doctype;  // Les chemins restent les mêmes
            String itemPath = collectionPath + "/{id}";
            paths.put(collectionPath, Map.of(
                "get", createGetAllOperation(doctype),
                "post", createPostOperation(doctype)
            ));
            paths.put(itemPath, Map.of(
                "get", createGetByIdOperation(doctype),
                "put", createPutOperation(doctype),
                "delete", createDeleteOperation(doctype)
            ));
        }
    }

    private Map<String, Object> createGetAllOperation(String doctype) {
        return Map.of(
            "summary", "Récupérer tous les " + doctype,
            "description", "Retourne la liste de tous les éléments de type " + doctype + " (via proxy Spring Boot)",
            "tags", List.of(doctype),
            "security", List.of(Map.of("sessionAuth", List.of()), Map.of("bearerAuth", List.of())),
            "parameters", List.of(
                Map.of(
                    "name", "filters",
                    "in", "query",
                    "required", false,
                    "description", "Filtres JSON optionnels",
                    "schema", Map.of("type", "string")
                )
            ),
            "responses", Map.of(
                "200", Map.of(
                    "description", "Liste récupérée avec succès",
                    "content", Map.of(
                        "application/json", Map.of(
                            "schema", Map.of(
                                "type", "array",
                                "items", Map.of("$ref", "#/components/schemas/GenericDocument")
                            )
                        )
                    )
                ),
                "403", Map.of("description", "Accès refusé"),
                "401", Map.of("description", "Non authentifié")
            )
        );
    }

    private Map<String, Object> createPostOperation(String doctype) {
        return Map.of(
            "summary", "Créer un nouveau " + doctype,
            "description", "Crée un nouvel élément de type " + doctype + " (via proxy Spring Boot)",
            "tags", List.of(doctype),
            "requestBody", Map.of(
                "required", true,
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of("$ref", "#/components/schemas/GenericDocument")
                    )
                )
            ),
            "responses", Map.of(
                "201", Map.of("description", "Document créé avec succès"),
                "400", Map.of("description", "Données invalides"),
                "403", Map.of("description", "Accès refusé")
            )
        );
    }

    private Map<String, Object> createGetByIdOperation(String doctype) {
        return Map.of(
            "summary", "Récupérer un " + doctype + " par ID",
            "description", "Retourne un élément spécifique de type " + doctype + " (via proxy Spring Boot)",
            "tags", List.of(doctype),
            "parameters", List.of(createPathParameter("id", "Identifiant de l'élément")),
            "responses", Map.of(
                "200", Map.of("description", "Document récupéré avec succès"),
                "404", Map.of("description", "Document non trouvé"),
                "403", Map.of("description", "Accès refusé")
            )
        );
    }

    private Map<String, Object> createPutOperation(String doctype) {
        return Map.of(
            "summary", "Mettre à jour un " + doctype,
            "description", "Met à jour un élément existant de type " + doctype + " (via proxy Spring Boot)",
            "tags", List.of(doctype),
            "parameters", List.of(createPathParameter("id", "Identifiant de l'élément à modifier")),
            "requestBody", Map.of(
                "required", true,
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of("$ref", "#/components/schemas/GenericDocument")
                    )
                )
            ),
            "responses", Map.of(
                "200", Map.of("description", "Document mis à jour avec succès"),
                "404", Map.of("description", "Document non trouvé"),
                "403", Map.of("description", "Accès refusé")
            )
        );
    }

    private Map<String, Object> createDeleteOperation(String doctype) {
        return Map.of(
            "summary", "Supprimer un " + doctype,
            "description", "Supprime un élément de type " + doctype + " (via proxy Spring Boot)",
            "tags", List.of(doctype),
            "parameters", List.of(createPathParameter("id", "Identifiant de l'élément à supprimer")),
            "responses", Map.of(
                "204", Map.of("description", "Document supprimé avec succès"),
                "404", Map.of("description", "Document non trouvé"),
                "403", Map.of("description", "Accès refusé")
            )
        );
    }

    private Map<String, Object> createPathParameter(String name, String description) {
        return Map.of(
            "name", name,
            "in", "path",
            "required", true,
            "description", description,
            "schema", Map.of("type", "string")
        );
    }

    private Map<String, Object> createComponents() {
        return Map.of(
            "schemas", Map.of(
                "GenericDocument", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name", Map.of("type", "string", "description", "Nom/ID du document"),
                        "creation", Map.of("type", "string", "format", "date-time", "description", "Date de création"),
                        "modified", Map.of("type", "string", "format", "date-time", "description", "Date de modification"),
                        "owner", Map.of("type", "string", "description", "Propriétaire du document"),
                        "docstatus", Map.of("type", "integer", "description", "Statut du document")
                    )
                ),
                "Error", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "message", Map.of("type", "string", "description", "Message d'erreur"),
                        "error_code", Map.of("type", "string", "description", "Code d'erreur"),
                        "timestamp", Map.of("type", "string", "format", "date-time", "description", "Horodatage de l'erreur")
                    )
                )
            ),
            "securitySchemes", Map.of(
                "sessionAuth", Map.of(
                    "type", "apiKey",
                    "in", "cookie",
                    "name", "sid",
                    "description", "Cookie de session ERPNext (format: sid=votre_session_id) - Géré automatiquement via le proxy"
                ),
                "bearerAuth", Map.of(
                    "type", "http",
                    "scheme", "bearer",
                    "description", "Token d'authentification ERPNext - Géré automatiquement via le proxy"
                )
            )
        );
    }
}