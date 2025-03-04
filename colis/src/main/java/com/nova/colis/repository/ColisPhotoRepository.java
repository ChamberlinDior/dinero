package com.nova.colis.repository;

import com.nova.colis.model.ColisPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColisPhotoRepository extends JpaRepository<ColisPhoto, Long> {
    // Par exemple, pour récupérer toutes les photos d’un colis précis :
    // List<ColisPhoto> findByColisId(Long colisId);
}
