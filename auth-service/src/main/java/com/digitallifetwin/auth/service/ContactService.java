package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.ContactRequest;
import com.digitallifetwin.auth.dto.response.ContactMessageResponse;
import com.digitallifetwin.auth.dto.response.MessageResponse;
import com.digitallifetwin.auth.entity.ContactMessage;
import com.digitallifetwin.auth.repository.ContactMessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;

    @Transactional
    public MessageResponse submit(ContactRequest request) {
        ContactMessage message = new ContactMessage();
        message.setName(request.name().trim());
        message.setEmail(request.email().trim());
        message.setSubject(request.subject().trim());
        message.setMessage(request.message().trim());
        contactMessageRepository.save(message);
        return new MessageResponse("Message received");
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> list() {
        return contactMessageRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(item -> new ContactMessageResponse(
                        item.getId(),
                        item.getName(),
                        item.getEmail(),
                        item.getSubject(),
                        item.getMessage(),
                        item.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return contactMessageRepository.count();
    }
}
