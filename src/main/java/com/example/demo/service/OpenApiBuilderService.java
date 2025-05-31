package com.example.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenApiBuilderService {

    private final ErpNextClientService client;

    @Value("${server.port:8080}")
    private String serverPort;

    public OpenApiBuilderService(ErpNextClientService client) {
        this.client = client;
    }

    public Map<String, Object> buildOpenApi() throws Exception {
        Map<String, Object> openapi = new LinkedHashMap<>();
        openapi.put("openapi", "3.0.0");
        openapi.put("info", createApiInfo("ERPNext Dynamic API", "Documentation complète de l'API ERPNext"));
        
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
            "description", description + "\n\n" +
                "⚠️ **Gestion des erreurs ERPNext** :\n" +
                "- **ProgrammingError** : DocType inexistant → Vérifiez le nom dans ERPNext\n" +
                "- **PermissionError** : Droits insuffisants → Vérifiez les permissions utilisateur\n" +
                "- **AuthenticationError** : Session expirée → Reconnectez-vous\n" +
                "- **ValidationError** : Données invalides → Vérifiez les champs obligatoires\n\n" +
                "Les erreurs affichent le stack trace Python complet pour faciliter le debug.",
            "version", "1.0.0",
            "contact", Map.of("name", "Support API", "email", "antsamadagascar@gmail.com")
        );
    }

    private void addModulePaths(Map<String, Object> paths, String module, List<String> doctypes) {
        for (String doctype : doctypes) {
            String collectionPath = "/resource/" + doctype;
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
            "description", "Retourne la liste de tous les éléments de type " + doctype,
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
            "responses", createStandardResponses()
        );
    }

    private Map<String, Object> createPostOperation(String doctype) {
        return Map.of(
            "summary", "Créer un nouveau " + doctype,
            "description", "Crée un nouvel élément de type " + doctype,
            "tags", List.of(doctype),
            "security", List.of(Map.of("sessionAuth", List.of()), Map.of("bearerAuth", List.of())),
            "requestBody", Map.of(
                "required", true,
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of("$ref", "#/components/schemas/GenericDocument")
                    )
                )
            ),
            "responses", createStandardResponses()
        );
    }

    private Map<String, Object> createGetByIdOperation(String doctype) {
        return Map.of(
            "summary", "Récupérer un " + doctype + " par ID",
            "description", "Retourne un élément spécifique de type " + doctype,
            "tags", List.of(doctype),
            "security", List.of(Map.of("sessionAuth", List.of()), Map.of("bearerAuth", List.of())),
            "parameters", List.of(createPathParameter("id", "Identifiant de l'élément")),
            "responses", createStandardResponses()
        );
    }

    private Map<String, Object> createPutOperation(String doctype) {
        return Map.of(
            "summary", "Mettre à jour un " + doctype,
            "description", "Met à jour un élément existant de type " + doctype,
            "tags", List.of(doctype),
            "security", List.of(Map.of("sessionAuth", List.of()), Map.of("bearerAuth", List.of())),
            "parameters", List.of(createPathParameter("id", "Identifiant de l'élément à modifier")),
            "requestBody", Map.of(
                "required", true,
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of("$ref", "#/components/schemas/GenericDocument")
                    )
                )
            ),
            "responses", createStandardResponses()
        );
    }

    private Map<String, Object> createDeleteOperation(String doctype) {
        return Map.of(
            "summary", "Supprimer un " + doctype,
            "description", "Supprime un élément de type " + doctype,
            "tags", List.of(doctype),
            "security", List.of(Map.of("sessionAuth", List.of()), Map.of("bearerAuth", List.of())),
            "parameters", List.of(createPathParameter("id", "Identifiant de l'élément à supprimer")),
            "responses", createStandardResponses()
        );
    }

    private Map<String, Object> createStandardResponses() {
        return Map.of(
            "default", Map.of(
                "description", "Réponse du serveur ERPNext",
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of(
                            "oneOf", List.of(
                                // Réponse de succès
                                Map.of(
                                    "type", "object",
                                    "description", "Réponse de succès d'ERPNext"
                                ),
                                // Réponse d'erreur structurée
                                Map.of("$ref", "#/components/schemas/ErpNextError")
                            )
                        ),
                        "examples", Map.of(
                            "success", Map.of(
                                "summary", "Succès",
                                "value", Map.of(
                                    "data", List.of(
                                        Map.of("name", "DOC-001", "creation", "2025-05-30")
                                    )
                                )
                            ),
                            "doctype_not_found", Map.of(
                                "summary", "DocType inexistant",
                                "value", Map.of(
                                    "exception", "pymysql.err.ProgrammingError: ('DocType', 'Authorization Control')",
                                    "exc_type", "ProgrammingError",
                                    "message", "Le DocType 'Authorization Control' n'existe pas dans la base de données ERPNext",
                                    "cause", "Table manquante ou DocType non installé"
                                )
                            ),
                            "permission_error", Map.of(
                                "summary", "Erreur de permission",
                                "value", Map.of(
                                    "exception", "frappe.exceptions.PermissionError",
                                    "message", "Insufficient Permission for User"
                                )
                            ),
                            "authentication_error", Map.of(
                                "summary", "Erreur d'authentification",
                                "value", Map.of(
                                    "exception", "frappe.exceptions.AuthenticationError",
                                    "message", "Not permitted"
                                )
                            )
                        )
                    )
                )
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
                    "description", "Document ERPNext générique",
                    "properties", Map.of(
                        "name", Map.of("type", "string", "description", "Nom/ID du document"),
                        "creation", Map.of("type", "string", "format", "date-time", "description", "Date de création"),
                        "modified", Map.of("type", "string", "format", "date-time", "description", "Date de modification"),
                        "owner", Map.of("type", "string", "description", "Propriétaire du document"),
                        "docstatus", Map.of("type", "integer", "description", "Statut du document (0=Draft, 1=Submitted, 2=Cancelled)")
                    ),
                    "additionalProperties", true
                ),
                "ErpNextError", Map.of(
                    "type", "object",
                    "description", "Structure d'erreur ERPNext avec explications détaillées",
                    "properties", Map.of(
                        "exception", Map.of(
                            "type", "string", 
                            "description", "Type d'exception Python",
                            "examples", List.of(
                                "pymysql.err.ProgrammingError: ('DocType', 'Authorization Control')",
                                "frappe.exceptions.PermissionError",
                                "frappe.exceptions.ValidationError"
                            )
                        ),
                        "exc_type", Map.of(
                            "type", "string", 
                            "description", "Type d'exception court",
                            "enum", List.of("ProgrammingError", "PermissionError", "ValidationError", "AuthenticationError")
                        ),
                        "exc", Map.of(
                            "type", "array",
                            "description", "Stack trace complet de l'erreur Python",
                            "items", Map.of("type", "string")
                        ),
                        "message", Map.of(
                            "type", "string",
                            "description", "Message d'erreur explicite"
                        )
                    ),
                    "examples", List.of(
                        Map.of(
                            "name", "DocType inexistant",
                            "value", Map.of(
                                "exception", "pymysql.err.ProgrammingError: ('DocType', 'Authorization Control')",
                                "exc_type", "ProgrammingError",
                                "message", "Le DocType 'Authorization Control' n'existe pas dans ERPNext. Causes possibles :\n- DocType supprimé ou renommé\n- Module non installé\n- Erreur de frappe dans le nom"
                            )
                        )
                    )
                ),
                "CommonErrors", Map.of(
                    "type", "object",
                    "description", "Guide des erreurs ERPNext les plus courantes",
                    "properties", Map.of(
                        "ProgrammingError", Map.of(
                            "type", "string",
                            "description", "DocType inexistant ou table manquante",
                            "example", "Vérifiez que le DocType existe dans ERPNext > Developer > DocType"
                        ),
                        "PermissionError", Map.of(
                            "type", "string", 
                            "description", "Utilisateur sans droits suffisants",
                            "example", "Vérifiez les permissions dans ERPNext > Users and Permissions > Role Permissions Manager"
                        ),
                        "AuthenticationError", Map.of(
                            "type", "string",
                            "description", "Session expirée ou token invalide", 
                            "example", "Reconnectez-vous ou générez un nouveau token API"
                        ),
                        "ValidationError", Map.of(
                            "type", "string",
                            "description", "Données invalides selon les règles ERPNext",
                            "example", "Vérifiez les champs obligatoires et les formats de données"
                        )
                    )
                )
            ),
            "securitySchemes", Map.of(
                "sessionAuth", Map.of(
                    "type", "apiKey",
                    "in", "cookie",
                    "name", "sid",
                    "description", "Cookie de session ERPNext"
                ),
                "bearerAuth", Map.of(
                    "type", "http",
                    "scheme", "bearer",
                    "description", "Token d'authentification ERPNext"
                )
            )
        );
    }
}