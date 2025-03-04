package com.nova.colis.dto;

import com.nova.colis.model.ModePaiement;
import com.nova.colis.model.StatutColis;
import com.nova.colis.model.StatutPaiement;
import com.nova.colis.model.TypeColis;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class ColisRequestDTO {

    // Informations générales
    @NotNull
    private TypeColis typeColis;
    private String description;
    private Double poids;
    private String dimensions;
    private Double valeurDeclaree;
    private Boolean assurance;

    // Expéditeur
    @NotNull
    private Long clientId;
    private String nomExpediteur;
    private String telephoneExpediteur;
    private String emailExpediteur;
    private String adresseEnlevement;
    private String villeDepart;

    // Destinataire
    private String nomDestinataire;
    private String telephoneDestinataire;
    private String emailDestinataire;
    private String adresseLivraison;
    /**
     * Pour ce champ, le frontend envoie désormais :
     * - le type d'expédition (ex : "Urbain", "Interurbain", "International")
     * via ce champ (bien que le nom reste "villeDestination")
     */
    private String villeDestination;

    // Livreur (optionnel)
    private Long livreurId;
    private String nomLivreur;
    private String telephoneLivreur;

    // État du colis
    private StatutColis statutColis;
    private LocalDateTime datePriseEnCharge;
    private LocalDateTime dateLivraisonEstimee;
    private LocalDateTime dateLivraisonEffective;

    // Paiement
    private ModePaiement modePaiement;
    private StatutPaiement statutPaiement;
    /**
     * Nouveau champ pour enregistrer les informations spécifiques au paiement.
     */
    private String paiementInfo;

    // Suivi
    private String historiqueSuivi;
    private String coordonneesGPS;
    private String preuveLivraison;

    /**
     * Nouveau champ : si l'on souhaite que le frontend puisse envoyer
     * des photos du colis au format base64.
     */
    private List<String> photosBase64;  // Ajout

    public ColisRequestDTO() {
    }

    // --- GETTERS / SETTERS ---

    public TypeColis getTypeColis() {
        return typeColis;
    }

    public void setTypeColis(TypeColis typeColis) {
        this.typeColis = typeColis;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPoids() {
        return poids;
    }

    public void setPoids(Double poids) {
        this.poids = poids;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public Double getValeurDeclaree() {
        return valeurDeclaree;
    }

    public void setValeurDeclaree(Double valeurDeclaree) {
        this.valeurDeclaree = valeurDeclaree;
    }

    public Boolean getAssurance() {
        return assurance;
    }

    public void setAssurance(Boolean assurance) {
        this.assurance = assurance;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getNomExpediteur() {
        return nomExpediteur;
    }

    public void setNomExpediteur(String nomExpediteur) {
        this.nomExpediteur = nomExpediteur;
    }

    public String getTelephoneExpediteur() {
        return telephoneExpediteur;
    }

    public void setTelephoneExpediteur(String telephoneExpediteur) {
        this.telephoneExpediteur = telephoneExpediteur;
    }

    public String getEmailExpediteur() {
        return emailExpediteur;
    }

    public void setEmailExpediteur(String emailExpediteur) {
        this.emailExpediteur = emailExpediteur;
    }

    public String getAdresseEnlevement() {
        return adresseEnlevement;
    }

    public void setAdresseEnlevement(String adresseEnlevement) {
        this.adresseEnlevement = adresseEnlevement;
    }

    public String getVilleDepart() {
        return villeDepart;
    }

    public void setVilleDepart(String villeDepart) {
        this.villeDepart = villeDepart;
    }

    public String getNomDestinataire() {
        return nomDestinataire;
    }

    public void setNomDestinataire(String nomDestinataire) {
        this.nomDestinataire = nomDestinataire;
    }

    public String getTelephoneDestinataire() {
        return telephoneDestinataire;
    }

    public void setTelephoneDestinataire(String telephoneDestinataire) {
        this.telephoneDestinataire = telephoneDestinataire;
    }

    public String getEmailDestinataire() {
        return emailDestinataire;
    }

    public void setEmailDestinataire(String emailDestinataire) {
        this.emailDestinataire = emailDestinataire;
    }

    public String getAdresseLivraison() {
        return adresseLivraison;
    }

    public void setAdresseLivraison(String adresseLivraison) {
        this.adresseLivraison = adresseLivraison;
    }

    public String getVilleDestination() {
        return villeDestination;
    }

    public void setVilleDestination(String villeDestination) {
        this.villeDestination = villeDestination;
    }

    public Long getLivreurId() {
        return livreurId;
    }

    public void setLivreurId(Long livreurId) {
        this.livreurId = livreurId;
    }

    public String getNomLivreur() {
        return nomLivreur;
    }

    public void setNomLivreur(String nomLivreur) {
        this.nomLivreur = nomLivreur;
    }

    public String getTelephoneLivreur() {
        return telephoneLivreur;
    }

    public void setTelephoneLivreur(String telephoneLivreur) {
        this.telephoneLivreur = telephoneLivreur;
    }

    public StatutColis getStatutColis() {
        return statutColis;
    }

    public void setStatutColis(StatutColis statutColis) {
        this.statutColis = statutColis;
    }

    public LocalDateTime getDatePriseEnCharge() {
        return datePriseEnCharge;
    }

    public void setDatePriseEnCharge(LocalDateTime datePriseEnCharge) {
        this.datePriseEnCharge = datePriseEnCharge;
    }

    public LocalDateTime getDateLivraisonEstimee() {
        return dateLivraisonEstimee;
    }

    public void setDateLivraisonEstimee(LocalDateTime dateLivraisonEstimee) {
        this.dateLivraisonEstimee = dateLivraisonEstimee;
    }

    public LocalDateTime getDateLivraisonEffective() {
        return dateLivraisonEffective;
    }

    public void setDateLivraisonEffective(LocalDateTime dateLivraisonEffective) {
        this.dateLivraisonEffective = dateLivraisonEffective;
    }

    public ModePaiement getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(ModePaiement modePaiement) {
        this.modePaiement = modePaiement;
    }

    public StatutPaiement getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(StatutPaiement statutPaiement) {
        this.statutPaiement = statutPaiement;
    }

    public String getPaiementInfo() {
        return paiementInfo;
    }

    public void setPaiementInfo(String paiementInfo) {
        this.paiementInfo = paiementInfo;
    }

    public String getHistoriqueSuivi() {
        return historiqueSuivi;
    }

    public void setHistoriqueSuivi(String historiqueSuivi) {
        this.historiqueSuivi = historiqueSuivi;
    }

    public String getCoordonneesGPS() {
        return coordonneesGPS;
    }

    public void setCoordonneesGPS(String coordonneesGPS) {
        this.coordonneesGPS = coordonneesGPS;
    }

    public String getPreuveLivraison() {
        return preuveLivraison;
    }

    public void setPreuveLivraison(String preuveLivraison) {
        this.preuveLivraison = preuveLivraison;
    }

    // --- Getter/Setter pour le nouveau champ ---

    public List<String> getPhotosBase64() {
        return photosBase64;
    }

    public void setPhotosBase64(List<String> photosBase64) {
        this.photosBase64 = photosBase64;
    }
}
