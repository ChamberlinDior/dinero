package com.nova.colis.service;

import com.nova.colis.dto.ColisDTO;
import com.nova.colis.dto.ColisRequestDTO;
import com.nova.colis.model.ColisPhoto;

import java.util.List;

public interface ColisService {

    ColisDTO createColis(ColisRequestDTO colisRequestDTO);
    ColisDTO getColisById(Long id);
    List<ColisDTO> getAllColis();

    ColisDTO updateColis(Long id, ColisRequestDTO colisRequestDTO);

    void deleteColis(Long id);

    ColisDTO updateStatutColis(Long id, String nouveauStatut);

    ColisDTO enregistrerPaiement(Long id, ColisRequestDTO dtoPaiement);

    // -----------------------------------------------------------------
    //      NOUVELLES METHODES pour gérer les Photos du Colis
    // -----------------------------------------------------------------
    /**
     * Retourne la liste des ColisPhoto (photos) associées à un colis.
     */
    List<ColisPhoto> getColisPhotos(Long colisId);

    /**
     * Ajoute une nouvelle photo (imageBytes) au colis indiqué.
     */
    void addPhotoToColis(Long colisId, byte[] imageBytes);

    /**
     * Supprime une photo particulière (par son ID) d’un colis donné.
     */
    void removePhotoFromColis(Long colisId, Long photoId);
}
