package com.application.services;

import com.application.data.Servicio;
import com.vaadin.hilla.Endpoint;
import com.vaadin.hilla.exception.EndpointException;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Endpoint
@RolesAllowed("ADMIN")
public class ServicioEndpoint {

    private final ServicioService service;

    public ServicioEndpoint(ServicioService service) {
        this.service = service;
    }

    public Page<Servicio> list(Pageable page) {
        return service.list(page);
    }

    public Optional<Servicio> get(Long id) {
        return service.get(id);
    }

    public Servicio update(Servicio entity) {
        try {
            return service.update(entity);
        } catch (OptimisticLockingFailureException e) {
            throw new EndpointException("Somebody else has updated the data while you were making changes.");
        }
    }

    public void delete(Long id) {
        service.delete(id);
    }

    public int count() {
        return service.count();
    }

}
