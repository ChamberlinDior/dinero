package com.nova.colis.service;

import com.nova.colis.dto.ClientDTO;
import com.nova.colis.dto.ColisDTO;
import com.nova.colis.dto.ColisRequestDTO;
import com.nova.colis.dto.LivreurDTO;
import com.nova.colis.exception.ResourceNotFoundException;
import com.nova.colis.model.Colis;
import com.nova.colis.model.ColisPhoto;
import com.nova.colis.model.StatutColis;
import com.nova.colis.repository.ColisPhotoRepository;
import com.nova.colis.repository.ColisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * Implémentation de ColisService avec gestion des photos.
 */
@Service
public class ColisServiceImpl implements ColisService {

    @Autowired
    private ColisRepository colisRepository;

    @Autowired
    private ColisPhotoRepository colisPhotoRepository;  // Pour gérer la table des photos

    // Service pour récupérer les informations du client
    @Autowired
    private ClientService clientService;

    // Service pour récupérer les informations du livreur
    @Autowired
    private LivreurService livreurService;

    // Service pour envoyer les notifications push via Firebase
    @Autowired
    private FirebaseMessagingService firebaseMessagingService;

    // ==========================================================
    // 1) Méthodes CRUD / Statut / Paiement
    // ==========================================================

    @Override
    public ColisDTO createColis(ColisRequestDTO dto) {
        // 1) On mappe le DTO vers l'entité
        Colis colis = mapToEntity(dto);
        colis.setReferenceColis("COL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // 2) Initialisation de la géolocalisation depuis le client (si dispo)
        ClientDTO clientDTO = clientService.getClientById(dto.getClientId());
        if (clientDTO.getLatitude() != null && clientDTO.getLongitude() != null) {
            String coords = String.format(Locale.US, "%.6f,%.6f",
                    clientDTO.getLatitude(),
                    clientDTO.getLongitude());
            colis.setCoordonneesGPS(coords);
        }

        // 3) Gérer les photos en base64 (si le front en envoie dans dto.getPhotosBase64())
        if (dto.getPhotosBase64() != null && !dto.getPhotosBase64().isEmpty()) {
            for (String base64Str : dto.getPhotosBase64()) {
                byte[] imageBytes = Base64.getDecoder().decode(base64Str);
                ColisPhoto photo = new ColisPhoto(imageBytes, colis);
                colis.getPhotos().add(photo);
            }
        }

        // 4) Calcul des tarifs
        calculTarif(colis);

        // 5) On sauvegarde en base
        Colis saved = colisRepository.save(colis);

        // 6) On retourne le DTO vers le front
        return mapToDTO(saved);
    }

    @Override
    public ColisDTO getColisById(Long id) {
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", id));
        return mapToDTO(colis);
    }

    @Override
    public List<ColisDTO> getAllColis() {
        return colisRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ColisDTO updateColis(Long id, ColisRequestDTO dto) {
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", id));

        // Mettre à jour l'entité depuis le DTO (champs existants)
        updateEntityFromDTO(colis, dto);

        // Calculer/Mettre à jour les tarifs
        calculTarif(colis);

        // Sauvegarde
        Colis updated = colisRepository.save(colis);
        return mapToDTO(updated);
    }

    @Override
    public void deleteColis(Long id) {
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", id));
        colisRepository.delete(colis);
    }

    /**
     * Mise à jour du statut du colis et de sa géolocalisation en fonction du nouveau statut.
     * Lorsqu'il passe à RECUPERE, on envoie une notification push au client, etc.
     */
    @Override
    public ColisDTO updateStatutColis(Long id, String nouveauStatut) {
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", id));

        StatutColis statutEnum = StatutColis.valueOf(nouveauStatut);

        // Logique de changement de statut et géolocalisation
        if (statutEnum == StatutColis.RECUPERE) {
            if (colis.getLivreurId() == null) {
                throw new IllegalArgumentException("Le colis ne peut être récupéré sans livreur assigné.");
            }
            List<StatutColis> statutsActifs = Arrays.asList(
                    StatutColis.RECUPERE,
                    StatutColis.EN_COURS_DE_LIVRAISON
            );
            // Vérif : si ce livreur a déjà un colis en cours
            List<Colis> colisActifs = colisRepository
                    .findByLivreurIdAndStatutColisIn(colis.getLivreurId(), statutsActifs);
            if (!colisActifs.isEmpty()) {
                throw new IllegalStateException("Ce livreur a déjà un colis en cours de livraison.");
            }
            // Récup coordonnées livreur
            LivreurDTO livreurDTO = livreurService.getLivreurById(colis.getLivreurId());
            if (livreurDTO.getLatitudeActuelle() != null && livreurDTO.getLongitudeActuelle() != null) {
                String coords = String.format(Locale.US, "%.6f,%.6f",
                        livreurDTO.getLatitudeActuelle(), livreurDTO.getLongitudeActuelle());
                colis.setCoordonneesGPS(coords);
            }
            colis.setDatePriseEnCharge(LocalDateTime.now());

        } else if (statutEnum == StatutColis.EN_COURS_DE_LIVRAISON) {
            if (colis.getLivreurId() != null) {
                LivreurDTO livreurDTO = livreurService.getLivreurById(colis.getLivreurId());
                if (livreurDTO.getLatitudeActuelle() != null && livreurDTO.getLongitudeActuelle() != null) {
                    String coords = String.format(Locale.US, "%.6f,%.6f",
                            livreurDTO.getLatitudeActuelle(), livreurDTO.getLongitudeActuelle());
                    colis.setCoordonneesGPS(coords);
                }
            }
            colis.setDatePriseEnCharge(LocalDateTime.now());

        } else if (statutEnum == StatutColis.LIVRE) {
            colis.setDateLivraisonEffective(LocalDateTime.now());

        } else if (statutEnum == StatutColis.EN_ATTENTE) {
            // S'il n'a pas encore de coord GPS, on peut récupérer celles du client
            if (colis.getCoordonneesGPS() == null) {
                ClientDTO clientDTO = clientService.getClientById(colis.getClientId());
                if (clientDTO.getLatitude() != null && clientDTO.getLongitude() != null) {
                    String coords = String.format(Locale.US, "%.6f,%.6f",
                            clientDTO.getLatitude(), clientDTO.getLongitude());
                    colis.setCoordonneesGPS(coords);
                }
            }
        }

        // Mise à jour du statut
        colis.setStatutColis(statutEnum);
        Colis saved = colisRepository.save(colis);
        ColisDTO dto = mapToDTO(saved);

        // Notification push au client
        ClientDTO clientDTO = clientService.getClientById(saved.getClientId());
        if (clientDTO != null && clientDTO.getFcmToken() != null) {
            String title = "Mise à jour de votre commande";
            String message;
            switch (saved.getStatutColis()) {
                case RECUPERE:
                    message = "Le livreur est en cours de route pour récupérer votre colis " + saved.getReferenceColis();
                    break;
                case EN_COURS_DE_LIVRAISON:
                    message = "Votre colis " + saved.getReferenceColis() + " est en cours de livraison.";
                    break;
                case LIVRE:
                    message = "Votre colis " + saved.getReferenceColis() + " a été livré.";
                    break;
                case EN_ATTENTE:
                    message = "Votre colis " + saved.getReferenceColis() + " est en attente.";
                    break;
                default:
                    message = "Le statut de votre colis " + saved.getReferenceColis() + " a changé.";
            }
            firebaseMessagingService.sendNotification(title, message, clientDTO.getFcmToken());
        }

        return dto;
    }

    @Override
    public ColisDTO enregistrerPaiement(Long id, ColisRequestDTO dtoPaiement) {
        Colis colis = colisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", id));

        if (dtoPaiement.getModePaiement() != null) {
            colis.setModePaiement(dtoPaiement.getModePaiement());
        }
        if (dtoPaiement.getStatutPaiement() != null) {
            colis.setStatutPaiement(dtoPaiement.getStatutPaiement());
        }
        if (dtoPaiement.getPaiementInfo() != null) {
            colis.setPaiementInfo(dtoPaiement.getPaiementInfo());
        }

        Colis saved = colisRepository.save(colis);
        return mapToDTO(saved);
    }

    // ==========================================================
    // 2) Méthodes de gestion des PHOTOS
    // ==========================================================

    @Override
    public List<ColisPhoto> getColisPhotos(Long colisId) {
        Colis colis = colisRepository.findById(colisId)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", colisId));
        // Retourne la liste des photos associées
        return colis.getPhotos();
    }

    @Override
    public void addPhotoToColis(Long colisId, byte[] imageBytes) {
        Colis colis = colisRepository.findById(colisId)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", colisId));

        ColisPhoto newPhoto = new ColisPhoto(imageBytes, colis);
        colis.getPhotos().add(newPhoto);

        // Sauvegarde (cascade = ALL => enregistre aussi la photo)
        colisRepository.save(colis);
    }

    @Override
    public void removePhotoFromColis(Long colisId, Long photoId) {
        Colis colis = colisRepository.findById(colisId)
                .orElseThrow(() -> new ResourceNotFoundException("Colis", "id", colisId));

        // On trouve la photo voulue
        ColisPhoto target = colis.getPhotos().stream()
                .filter(p -> p.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Photo", "id", photoId));

        // On la retire => orphanRemoval = true => la photo sera supprimée
        colis.getPhotos().remove(target);

        // On sauvegarde
        colisRepository.save(colis);
    }

    // ==========================================================
    // 3) Méthodes internes : calculTarif, mapToDTO, etc.
    // ==========================================================

    /**
     * Calcule le prix total, la répartition livreur/plateforme, etc.
     */
    private void calculTarif(Colis colis) {
        if (colis.getPoids() == null) {
            colis.setPoids(0.0);
        }
        double poids = colis.getPoids();
        double basePrice = 0.0;
        String expeditionType = colis.getVilleDestination();

        switch (colis.getTypeColis()) {
            case STANDARD:
                if ("Urbain".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 3000;
                    else if (poids <= 10) basePrice = 4500;
                    else if (poids <= 20) basePrice = 7500;
                    else if (poids <= 30) basePrice = 11000;
                } else if ("Interurbain".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 7500;
                    else if (poids <= 10) basePrice = 10000;
                    else if (poids <= 20) basePrice = 15000;
                    else if (poids <= 30) basePrice = 20000;
                } else if ("International".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 34650;
                    else if (poids <= 10) basePrice = 66300;
                    else if (poids <= 20) basePrice = 130600;
                    else if (poids <= 30) basePrice = 196000;
                }
                break;

            case OBJET_DE_VALEUR:
                if ("Urbain".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 4000;
                    else if (poids <= 10) basePrice = 6000;
                    else if (poids <= 20) basePrice = 9500;
                    else if (poids <= 30) basePrice = 14000;
                } else if ("Interurbain".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 8000;
                    else if (poids <= 10) basePrice = 12000;
                    else if (poids <= 20) basePrice = 18000;
                    else if (poids <= 30) basePrice = 25000;
                } else if ("International".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 36382;
                    else if (poids <= 10) basePrice = 69615;
                    else if (poids <= 20) basePrice = 137130;
                    else if (poids <= 30) basePrice = 205800;
                }
                break;

            case VOLUMINEUX:
                if ("Urbain".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 8000;
                    else if (poids <= 10) basePrice = 12000;
                    else if (poids <= 20) basePrice = 18000;
                    else if (poids <= 30) basePrice = 26000;
                } else if ("Interurbain".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 15000;
                    else if (poids <= 10) basePrice = 20000;
                    else if (poids <= 20) basePrice = 30000;
                    else if (poids <= 30) basePrice = 40000;
                } else if ("International".equalsIgnoreCase(expeditionType)) {
                    if (poids <= 5) basePrice = 65000;
                    else if (poids <= 10) basePrice = 100000;
                    else if (poids <= 20) basePrice = 150000;
                    else if (poids <= 30) basePrice = 250000;
                }
                break;
        }

        // Si assurance cochée => +5%
        if (Boolean.TRUE.equals(colis.getAssurance())) {
            basePrice *= 1.05;
        }

        // 75% livreur / 25% plateforme
        double livreurShare = basePrice * 0.75;
        double plateformeShare = basePrice * 0.25;

        colis.setPrixTotal(basePrice);
        colis.setFraisLivraison(livreurShare);
        colis.setCommissionPlateforme(plateformeShare);
    }

    /**
     * Mappe un ColisRequestDTO vers un objet Colis (nouveau ou existant).
     */
    private Colis mapToEntity(ColisRequestDTO dto) {
        Colis c = new Colis();
        updateEntityFromDTO(c, dto);
        return c;
    }

    /**
     * Met à jour l'entité Colis existante avec les champs du DTO.
     */
    private void updateEntityFromDTO(Colis c, ColisRequestDTO dto) {
        if (dto.getTypeColis() != null) {
            c.setTypeColis(dto.getTypeColis());
        }
        c.setDescription(dto.getDescription());
        c.setPoids(dto.getPoids());
        c.setDimensions(dto.getDimensions());
        c.setValeurDeclaree(dto.getValeurDeclaree());
        c.setAssurance(dto.getAssurance());

        if (dto.getClientId() != null) {
            c.setClientId(dto.getClientId());
        }

        c.setNomExpediteur(dto.getNomExpediteur());
        c.setTelephoneExpediteur(dto.getTelephoneExpediteur());
        c.setEmailExpediteur(dto.getEmailExpediteur());

        // N'autoriser la mise à jour de certains champs (adresse, etc.) que si le colis est EN_ATTENTE
        if (c.getStatutColis() == null || c.getStatutColis() == StatutColis.EN_ATTENTE) {
            if (dto.getAdresseEnlevement() != null) {
                c.setAdresseEnlevement(dto.getAdresseEnlevement());
            }
            if (dto.getAdresseLivraison() != null) {
                c.setAdresseLivraison(dto.getAdresseLivraison());
            }
            if (dto.getVilleDestination() != null) {
                c.setVilleDestination(dto.getVilleDestination());
            }
        }

        c.setNomDestinataire(dto.getNomDestinataire());
        c.setTelephoneDestinataire(dto.getTelephoneDestinataire());
        c.setEmailDestinataire(dto.getEmailDestinataire());
        c.setLivreurId(dto.getLivreurId());
        c.setNomLivreur(dto.getNomLivreur());
        c.setTelephoneLivreur(dto.getTelephoneLivreur());

        if (dto.getStatutColis() != null) {
            c.setStatutColis(dto.getStatutColis());
        }
        if (dto.getDatePriseEnCharge() != null) {
            c.setDatePriseEnCharge(dto.getDatePriseEnCharge());
        }
        if (dto.getDateLivraisonEstimee() != null) {
            c.setDateLivraisonEstimee(dto.getDateLivraisonEstimee());
        }
        if (dto.getDateLivraisonEffective() != null) {
            c.setDateLivraisonEffective(dto.getDateLivraisonEffective());
        }
        if (dto.getModePaiement() != null) {
            c.setModePaiement(dto.getModePaiement());
        }
        if (dto.getStatutPaiement() != null) {
            c.setStatutPaiement(dto.getStatutPaiement());
        }
        if (dto.getPaiementInfo() != null) {
            c.setPaiementInfo(dto.getPaiementInfo());
        }

        c.setHistoriqueSuivi(dto.getHistoriqueSuivi());

        // S'il n'y a pas déjà de coordonneesGPS et qu'on en reçoit
        if (c.getCoordonneesGPS() == null && dto.getCoordonneesGPS() != null) {
            c.setCoordonneesGPS(dto.getCoordonneesGPS());
        }

        c.setPreuveLivraison(dto.getPreuveLivraison());
    }

    /**
     * Mappe un objet Colis (entité) vers le DTO ColisDTO.
     */
    private ColisDTO mapToDTO(Colis c) {
        ColisDTO dto = new ColisDTO();
        dto.setId(c.getId());
        dto.setReferenceColis(c.getReferenceColis());
        dto.setTypeColis(c.getTypeColis());
        dto.setDescription(c.getDescription());
        dto.setPoids(c.getPoids());
        dto.setDimensions(c.getDimensions());
        dto.setValeurDeclaree(c.getValeurDeclaree());
        dto.setAssurance(c.getAssurance());
        dto.setClientId(c.getClientId());
        dto.setNomExpediteur(c.getNomExpediteur());
        dto.setTelephoneExpediteur(c.getTelephoneExpediteur());
        dto.setEmailExpediteur(c.getEmailExpediteur());
        dto.setAdresseEnlevement(c.getAdresseEnlevement());
        dto.setVilleDepart(c.getVilleDepart());
        dto.setNomDestinataire(c.getNomDestinataire());
        dto.setTelephoneDestinataire(c.getTelephoneDestinataire());
        dto.setEmailDestinataire(c.getEmailDestinataire());
        dto.setAdresseLivraison(c.getAdresseLivraison());
        dto.setVilleDestination(c.getVilleDestination());
        dto.setLivreurId(c.getLivreurId());
        dto.setNomLivreur(c.getNomLivreur());
        dto.setTelephoneLivreur(c.getTelephoneLivreur());
        dto.setStatutColis(c.getStatutColis());
        dto.setDateCreation(c.getDateCreation());
        dto.setDatePriseEnCharge(c.getDatePriseEnCharge());
        dto.setDateLivraisonEstimee(c.getDateLivraisonEstimee());
        dto.setDateLivraisonEffective(c.getDateLivraisonEffective());
        dto.setPrixTotal(c.getPrixTotal());
        dto.setFraisLivraison(c.getFraisLivraison());
        dto.setCommissionPlateforme(c.getCommissionPlateforme());
        dto.setModePaiement(c.getModePaiement());
        dto.setStatutPaiement(c.getStatutPaiement());
        dto.setHistoriqueSuivi(c.getHistoriqueSuivi());
        dto.setCoordonneesGPS(c.getCoordonneesGPS());
        dto.setPreuveLivraison(c.getPreuveLivraison());
        dto.setPaiementInfo(c.getPaiementInfo());

        // Si vous souhaitez renvoyer les photos en base64 dans ColisDTO, vous pouvez faire :
        // List<String> listBase64 = c.getPhotos().stream()
        //     .map(photo -> Base64.getEncoder().encodeToString(photo.getImage()))
        //     .collect(Collectors.toList());
        // dto.setPhotosBase64(listBase64);

        return dto;
    }
}
