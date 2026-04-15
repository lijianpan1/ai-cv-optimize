package com.luren.aicvoptimize.repository;

import com.luren.aicvoptimize.entity.CvUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<CvUser, Long> {
    Optional<CvUser> findByUsername(String username);
    Optional<CvUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Transactional
    @Modifying
    @Query("update CvUser c set c.quota = ?1 where c.username = ?2")
    int updateQuotaByUsername(Integer quota, String username);
}
