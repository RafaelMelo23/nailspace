package com.rafael.nailspro.webapp.shared.tenant;

import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.domain.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IgnoreFilterTestService {
    @Autowired
    private ClientRepository clientRepository;

    @IgnoreTenantFilter
    public List<Client> findAllIgnored() {
        return clientRepository.findAll();
    }
}
