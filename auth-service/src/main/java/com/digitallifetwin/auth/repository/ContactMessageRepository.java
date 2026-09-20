package com.digitallifetwin.auth.repository;

import com.digitallifetwin.auth.entity.ContactMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
}
