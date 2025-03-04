package com.nova.colis.controller;

import com.nova.colis.dto.ColisDTO;
import com.nova.colis.dto.ColisRequestDTO;
import com.nova.colis.model.ColisPhoto;
import com.nova.colis.service.ColisService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour gérer les Colis et leurs photos.
 */
@RestController
@RequestMapping("/api/colis")
public class ColisController {

    @Autowired
    private ColisService colisService;

    // -------------------------------------------------------------------------
    //         1) CRUD de base sur Colis (existant dans votre code)
    // -------------------------------------------------------------------------

    // Créer un nouveau colis
    @PostMapping
    public ResponseEntity<ColisDTO> createColis(@Valid @RequestBody ColisRequestDTO colisRequestDTO) {
        ColisDTO created = colisService.createColis(colisRequestDTO);
        return ResponseEntity.ok(created);
    }

    // Récupérer un colis par son ID
    @GetMapping("/{id}")
    public ResponseEntity<ColisDTO> getColisById(@PathVariable Long id) {
        ColisDTO colisDTO = colisService.getColisById(id);
        return ResponseEntity.ok(colisDTO);
    }

    // Récupérer tous les colis
    @GetMapping
    public ResponseEntity<List<ColisDTO>> getAllColis() {
        List<ColisDTO> colisList = colisService.getAllColis();
        return ResponseEntity.ok(colisList);
    }

    // Mettre à jour un colis
    @PutMapping("/{id}")
    public ResponseEntity<ColisDTO> updateColis(@PathVariable Long id, @Valid @RequestBody ColisRequestDTO dto) {
        ColisDTO updated = colisService.updateColis(id, dto);
        return ResponseEntity.ok(updated);
    }

    // Supprimer un colis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColis(@PathVariable Long id) {
        colisService.deleteColis(id);
        return ResponseEntity.noContent().build();
    }

    // Mettre à jour le statut du colis
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ColisDTO> updateStatut(@PathVariable Long id, @RequestParam("statut") String statut) {
        ColisDTO updated = colisService.updateStatutColis(id, statut);
        return ResponseEntity.ok(updated);
    }

    // Enregistrer un paiement pour un colis
    @PostMapping("/{id}/paiement")
    public ResponseEntity<ColisDTO> enregistrerPaiement(@PathVariable Long id, @RequestBody ColisRequestDTO dtoPaiement) {
        ColisDTO updated = colisService.enregistrerPaiement(id, dtoPaiement);
        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------------------------
    //         2) Gestion des PHOTOS du Colis
    // -------------------------------------------------------------------------

    /**
     * Récupérer la liste des photos (encodées en base64) pour un colis donné.
     */
    @GetMapping("/{id}/photos")
    public ResponseEntity<List<String>> getColisPhotos(@PathVariable Long id) {
        // Récupérer le ColisDTO complet
        ColisDTO colisDTO = colisService.getColisById(id);

        // Dans votre ColisDTO, vous pouvez avoir un champ photosBase64
        // OU vous pouvez récupérer la liste des ColisPhoto via la méthode de service
        // Pour l'exemple, imaginons que colisService expose une méthode getColisPhotos(id)
        List<ColisPhoto> photos = colisService.getColisPhotos(id);

        // Conversion en base64
        List<String> base64List = photos.stream()
                .map(photo -> Base64.getEncoder().encodeToString(photo.getImage()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(base64List);
    }

    /**
     * Ajouter une photo en base64 dans le JSON du body.
     * (Exemple: le front envoie {"base64":"..."}).
     */
    @PostMapping("/{id}/photos/base64")
    public ResponseEntity<String> addPhotoBase64(
            @PathVariable Long id,
            @RequestBody PhotoRequest photoRequest
    ) {
        // Décoder la chaîne base64 en tableau de bytes
        byte[] imageBytes = Base64.getDecoder().decode(photoRequest.getBase64());

        // Appel au service
        colisService.addPhotoToColis(id, imageBytes);

        return ResponseEntity.ok("Photo (base64) ajoutée avec succès");
    }

    /**
     * Ajouter une photo via un fichier uploadé (Multipart).
     * Envoie du fichier (clé "file") dans un <form> ou via Postman, etc.
     */
    @PostMapping(path = "/{id}/photos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        // Conversion du fichier en tableau d'octets
        byte[] imageBytes = file.getBytes();

        // Appel au service
        colisService.addPhotoToColis(id, imageBytes);

        return ResponseEntity.ok("Photo (multipart) ajoutée avec succès");
    }

    /**
     * Supprimer une photo particulière d'un colis, en se basant sur l'ID de la photo.
     * (Il faut l'ID de la ColisPhoto)
     */
    @DeleteMapping("/{colisId}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long colisId,
            @PathVariable Long photoId
    ) {
        colisService.removePhotoFromColis(colisId, photoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Classe interne pour recevoir un JSON de type {"base64":"..."}
     * lorsqu'on POST /api/colis/{id}/photos/base64
     */
    public static class PhotoRequest {
        private String base64;

        public String getBase64() {
            return base64;
        }
        public void setBase64(String base64) {
            this.base64 = base64;
        }
    }
}
