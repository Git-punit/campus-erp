package com.campuserp.training;

import com.campuserp.training.TrainingDtos.ClientRequest;
import com.campuserp.training.TrainingDtos.ClientView;
import com.campuserp.training.TrainingDtos.ProgramRequest;
import com.campuserp.training.TrainingDtos.ProgramView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TrainingController {

    private final TrainingService service;

    public TrainingController(TrainingService service) {
        this.service = service;
    }

    @GetMapping("/training-programs")
    public List<ProgramView> programs() {
        return service.programs();
    }

    @GetMapping("/training-programs/{id}")
    public ProgramView program(@PathVariable Long id) {
        return service.program(id);
    }

    @PostMapping("/training-programs")
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramView create(@Valid @RequestBody ProgramRequest req) {
        return service.saveProgram(null, req);
    }

    @PutMapping("/training-programs/{id}")
    public ProgramView update(@PathVariable Long id, @Valid @RequestBody ProgramRequest req) {
        return service.saveProgram(id, req);
    }

    @DeleteMapping("/training-programs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteProgram(id);
    }

    @GetMapping("/training-programs/{id}/clients")
    public List<ClientView> clients(@PathVariable Long id) {
        return service.clients(id);
    }

    @PostMapping("/training-programs/{id}/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientView addClient(@PathVariable Long id, @Valid @RequestBody ClientRequest req, Authentication auth) {
        return service.addClient(id, req, auth.getName());
    }

    @GetMapping("/training-clients/{id}")
    public ClientView client(@PathVariable Long id) {
        return service.client(id);
    }

    @PutMapping("/training-clients/{id}")
    public ClientView updateClient(@PathVariable Long id, @Valid @RequestBody ClientRequest req, Authentication auth) {
        return service.updateClient(id, req, auth.getName());
    }

    @DeleteMapping("/training-clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable Long id) {
        service.deleteClient(id);
    }
}
