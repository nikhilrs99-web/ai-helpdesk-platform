package com.helpdesk.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "agents")
public class Agent extends BaseEntity {

    @Column(name = "keycloak_subject_id", nullable = false, unique = true)
    private String keycloakSubjectId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String team;

    public String getKeycloakSubjectId() {
        return keycloakSubjectId;
    }

    public void setKeycloakSubjectId(String keycloakSubjectId) {
        this.keycloakSubjectId = keycloakSubjectId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}
