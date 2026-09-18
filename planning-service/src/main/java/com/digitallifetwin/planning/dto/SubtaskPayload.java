package com.digitallifetwin.planning.dto;

import java.util.UUID;

public record SubtaskPayload(UUID id, String title, boolean done) {
}
