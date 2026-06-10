package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.enums.UserRole;
import dev.thilanka.resolvr.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetToken(String token);

    List<User> findByRole(UserRole role);

    List<User> findByRoleAndActiveTrue(UserRole role);

    @Query("SELECT u FROM User u WHERE u.role = 'MANAGER' AND u.active = true AND u.region.id = :regionId")
    List<User> findActiveManagersByRegionId(@Param("regionId") Long regionId);

    @Query("SELECT u FROM User u JOIN u.districts d WHERE d.id = :districtId AND u.role = :role AND u.active = true")
    List<User> findActiveByDistrictAndRole(@Param("districtId") Long districtId, @Param("role") UserRole role);

    @Query("SELECT u FROM User u WHERE u.region.id = :regionId AND u.active = true")
    List<User> findActiveByRegion(@Param("regionId") Long regionId);

    List<User> findByActiveAndRoleIsNull(boolean active);

    @Query(value = """
            SELECT * FROM public.users
            WHERE role = 'ENGINEER' OR role = 'TECHNICAL_OFFICER'
            ORDER BY full_name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<User> findAllAssigners(@Param("limit") int limit);

    @Query(value = """
            SELECT * FROM public.users u
            JOIN user_districts ud
            	ON u.id = ud.user_id
            WHERE ud.district_id = :districtId
            	AND (u.role = 'ENGINEER' OR u.role = 'TECHNICAL_OFFICER')
            """, nativeQuery = true)
    List<User> findAllAssignersByDistrict(@Param("districtId") Long districtId);
}
