package raf.si.racunovodstvo.nabavka.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import raf.si.racunovodstvo.nabavka.feign.PreduzeceFeignClient;
import raf.si.racunovodstvo.nabavka.requests.KonverzijaRequest;
import raf.si.racunovodstvo.nabavka.responses.PreduzeceResponse;
import raf.si.racunovodstvo.nabavka.utils.SearchUtil;
import raf.si.racunovodstvo.nabavka.utils.ApiUtil;

import raf.si.racunovodstvo.nabavka.model.Konverzija;
import raf.si.racunovodstvo.nabavka.responses.KonverzijaResponse;
import raf.si.racunovodstvo.nabavka.services.impl.KonverzijaService;
import raf.si.racunovodstvo.nabavka.services.IKonverzijaService;
import raf.si.racunovodstvo.nabavka.validation.groups.OnCreate;

import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import java.util.Optional;

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearerAuth")
@Validated
@RequestMapping("/api/konverzije")
public class KonverzijaRestController {

    private final IKonverzijaService iKonverzijaService;

    private final PreduzeceFeignClient preduzeceFeignClient;

    private final SearchUtil<Konverzija> searchUtil;

    public KonverzijaRestController(KonverzijaService iKonverzijaService, PreduzeceFeignClient preduzeceFeignClient) {
        this.iKonverzijaService = iKonverzijaService;
        this.preduzeceFeignClient = preduzeceFeignClient;
        this.searchUtil = new SearchUtil<>();
    }

    private PreduzeceResponse getPreduzeceById(Long id, String token) {
        if (id == null) {
            return null;
        }
        return preduzeceFeignClient.getPreduzeceById(id, token).getBody();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<KonverzijaResponse>> search(@RequestParam(name = "search", required = false, defaultValue = "") String search,
                                    @RequestParam(defaultValue = ApiUtil.DEFAULT_PAGE) @Min(ApiUtil.MIN_PAGE) Integer page,
                                    @RequestParam(defaultValue = ApiUtil.DEFAULT_SIZE) @Min(ApiUtil.MIN_SIZE) @Max(ApiUtil.MAX_SIZE) Integer size,
                                    @RequestParam(defaultValue = "-id") String[] sort) {
        Pageable pageSort = ApiUtil.resolveSortingAndPagination(page, size, sort);
        /*
        if (!search.isBlank()) {
            Specification<Konverzija> spec = searchUtil.getSpec(search);
            return ResponseEntity.ok(iKonverzijaService.findAll(spec, pageSort));
        }

         */
        return ResponseEntity.ok(iKonverzijaService.findAll(pageSort));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KonverzijaResponse> createKonverzija(@Validated(OnCreate.class) @RequestBody KonverzijaRequest konverzija,
                                              @RequestHeader(name = "Authorization") String token) {
        if (getPreduzeceById(konverzija.getDobavljacId(), token) == null) {
            throw new PersistenceException(String.format("Ne postoji dobavljac sa id-jem %s", konverzija.getDobavljacId()));
        }
        return ResponseEntity.ok(iKonverzijaService.saveKonverzija(konverzija));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> deleteKonverzija(@PathVariable("id") Long id) {
        Optional<Konverzija> optionalKonverzija = iKonverzijaService.findById(id);
        if (optionalKonverzija.isPresent()) {
            iKonverzijaService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        throw new EntityNotFoundException();
    }
}
