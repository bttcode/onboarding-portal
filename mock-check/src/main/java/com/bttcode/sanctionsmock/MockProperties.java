package com.bttcode.sanctionsmock;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Last names that produce a HIT, so a demo can trigger one on purpose. */
@ConfigurationProperties(prefix = "sanctions-mock")
public record MockProperties(@DefaultValue({"Blocked", "Sanctioned"}) List<String> listedLastNames) {
}
