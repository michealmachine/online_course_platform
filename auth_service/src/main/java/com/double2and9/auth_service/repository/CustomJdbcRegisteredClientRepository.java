package com.double2and9.auth_service.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Primary
@Repository
public class CustomJdbcRegisteredClientRepository extends JdbcRegisteredClientRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomJdbcRegisteredClientRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
    }

    public List<RegisteredClient> findAll() {
        List<RegisteredClient> clients = new ArrayList<>();
        List<String> clientIds = jdbcTemplate.queryForList(
            "SELECT client_id FROM oauth2_registered_client", String.class);
        
        for (String clientId : clientIds) {
            RegisteredClient client = findByClientId(clientId);
            if (client != null) {
                clients.add(client);
            }
        }
        
        return clients;
    }
} 