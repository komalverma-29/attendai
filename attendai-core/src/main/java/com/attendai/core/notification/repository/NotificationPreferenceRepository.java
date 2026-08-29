package com.attendai.core.notification.repository;

import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndTypeCodeAndChannel(
            Long userId, String typeCode, Channel channel);
}
