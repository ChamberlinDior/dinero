package com.nova.colis.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entité JPA représentant une photo associée à un Colis.
 */
@Entity
@Table(name = "colis_photos")
public class ColisPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Contenu binaire de la photo.
     * Annotation @Lob et columnDefinition="LONGBLOB"
     * pour accepter tout format/taille d'image.
     */
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] image;

    /**
     * Relation ManyToOne : plusieurs ColisPhoto peuvent être associées
     * à un même Colis. fetch=LAZY pour ne charger la photo que si nécessaire.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colis_id")
    private Colis colis;

    // --- Constructeurs ---
    public ColisPhoto() {
    }

    public ColisPhoto(byte[] image, Colis colis) {
        this.image = image;
        this.colis = colis;
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public Colis getColis() {
        return colis;
    }

    public void setColis(Colis colis) {
        this.colis = colis;
    }

    // --- equals & hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColisPhoto)) return false;
        ColisPhoto that = (ColisPhoto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
