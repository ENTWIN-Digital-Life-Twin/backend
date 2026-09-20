package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.ContactRequest;
import com.digitallifetwin.auth.entity.ContactMessage;
import com.digitallifetwin.auth.repository.ContactMessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    private ContactService contactService;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactMessageRepository);
    }

    @Test
    void submit_persistsTrimmedFields() {
        when(contactMessageRepository.save(any(ContactMessage.class))).thenAnswer(invocation -> {
            ContactMessage message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });

        contactService.submit(new ContactRequest("  Ada  ", "ada@example.com", "Hello", "This is a real message."));

        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ada");
        assertThat(captor.getValue().getSubject()).isEqualTo("Hello");
    }

    @Test
    void list_mapsNewestFirst() {
        ContactMessage message = new ContactMessage();
        message.setId(UUID.randomUUID());
        message.setName("Ada");
        message.setEmail("ada@example.com");
        message.setSubject("Hello");
        message.setMessage("This is a real message.");
        message.setCreatedAt(Instant.parse("2026-09-25T10:00:00Z"));
        when(contactMessageRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(message));

        assertThat(contactService.list()).hasSize(1);
        assertThat(contactService.list().getFirst().email()).isEqualTo("ada@example.com");
    }
}
