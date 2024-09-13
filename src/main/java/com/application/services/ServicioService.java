package com.application.services;

import com.application.data.Servicio;
import com.application.data.ServicioRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ServicioService {

    private final ServicioRepository repository;

    public ServicioService(ServicioRepository repository) {
        this.repository = repository;
    }

    public Optional<Servicio> get(Long id) {
        return repository.findById(id);
    }

    public Servicio update(Servicio entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Servicio> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Servicio> list(Pageable pageable, Specification<Servicio> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
