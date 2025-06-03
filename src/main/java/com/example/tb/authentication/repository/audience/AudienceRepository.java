package com.example.tb.authentication.repository.audience;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.tb.model.entity.User;

@Repository
public interface AudienceRepository extends JpaRepository<User, UUID> {

    @Query("SELECT DISTINCT u FROM User u JOIN u.registeredEvents e WHERE e.id = :eventId")
    List<User> findAllByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.registeredEvents e WHERE e.id = :eventId")
    Long countByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u JOIN u.registeredEvents e WHERE e.id = :eventId AND u.id = :userId")
    boolean existsByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
}
