package com.lumenml.rabbit;

import java.util.UUID;

public record NotificationEvent(String type, UUID taskId, String message) {}
