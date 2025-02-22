package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.CreateClientRequest;
import com.double2and9.auth_service.dto.request.UpdateClientRequest;
import com.double2and9.auth_service.dto.response.ClientResponse;
import com.double2and9.auth_service.service.ClientService;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClientResponse createClient(@Valid @RequestBody CreateClientRequest request) {
        return clientService.createClient(request);
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientResponse getClient(@PathVariable String clientId) {
        return clientService.getClient(clientId);
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientResponse updateClient(
            @PathVariable String clientId,
            @Valid @RequestBody UpdateClientRequest request) {
        return clientService.updateClient(clientId, request);
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteClient(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResult<ClientResponse> listClients(
            @RequestParam(defaultValue = "1") Long pageNo,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return clientService.listClients(new PageParams(pageNo, pageSize));
    }
} 