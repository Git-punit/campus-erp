package com.campuserp.course;

import com.campuserp.course.OfferingDtos.ExceptionRequest;
import com.campuserp.course.OfferingDtos.ExceptionView;
import com.campuserp.course.OfferingDtos.OfferingRequest;
import com.campuserp.course.OfferingDtos.OfferingView;
import com.campuserp.course.OfferingDtos.SaveResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OfferingController {

    private final OfferingService service;

    public OfferingController(OfferingService service) {
        this.service = service;
    }

    @GetMapping("/offerings")
    public List<OfferingView> search(@RequestParam(required = false) String year,
                                     @RequestParam(required = false) Integer semester,
                                     @RequestParam(required = false) Long sectionId) {
        return service.search(year, semester, sectionId);
    }

    @GetMapping("/offerings/{id}")
    public OfferingView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/offerings")
    @ResponseStatus(HttpStatus.CREATED)
    public SaveResult create(@Valid @RequestBody OfferingRequest req) {
        return service.create(req);
    }

    @PutMapping("/offerings/{id}")
    public SaveResult update(@PathVariable Long id, @Valid @RequestBody OfferingRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/offerings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** The courses a teacher teaches. Leave out year for every year. */
    @GetMapping("/faculty/{id}/courses")
    public List<OfferingView> taughtBy(@PathVariable Long id, @RequestParam(required = false) String year) {
        return service.taughtBy(id, year);
    }

    @GetMapping("/offerings/{id}/exceptions")
    public List<ExceptionView> exceptions(@PathVariable Long id) {
        return service.exceptions(id);
    }

    @PostMapping("/offerings/{id}/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExceptionView addException(@PathVariable Long id, @Valid @RequestBody ExceptionRequest req) {
        return service.addException(id, req);
    }

    @DeleteMapping("/class-exceptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteException(@PathVariable Long id) {
        service.deleteException(id);
    }
}
