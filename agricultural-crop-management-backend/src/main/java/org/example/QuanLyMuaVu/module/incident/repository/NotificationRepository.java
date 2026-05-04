package org.example.QuanLyMuaVu.module.incident.repository;



import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.incident.entity.Notification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findByUserIdOrderByNewest(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.userId = :userId")
    Optional<Notification> findByIdAndUserId(@Param("id") Integer id, @Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Query("""
            SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
            FROM Notification n
            WHERE n.userId = :userId
              AND n.title = :title
              AND n.message = :message
              AND ((:link IS NULL AND n.link IS NULL) OR n.link = :link)
              AND n.createdAt >= :fromTime
            """)
    boolean existsRecentDuplicate(
            @Param("userId") Long userId,
            @Param("title") String title,
            @Param("message") String message,
            @Param("link") String link,
            @Param("fromTime") LocalDateTime fromTime);
}
