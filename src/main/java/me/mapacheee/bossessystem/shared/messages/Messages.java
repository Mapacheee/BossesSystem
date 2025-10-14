package me.mapacheee.bossessystem.shared.messages;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Map;

@ConfigSerializable
@Configurate("messages")
public record Messages(
    String prefix,
    Map<String, String> errors,
    Map<String, String> flow,
    Map<String, String> end,
    Map<String, String> admin,
    Map<String, String> ui
) {}

