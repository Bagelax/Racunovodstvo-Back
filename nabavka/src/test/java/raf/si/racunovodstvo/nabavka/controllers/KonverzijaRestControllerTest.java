package raf.si.racunovodstvo.nabavka.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import raf.si.racunovodstvo.nabavka.feign.PreduzeceFeignClient;
import raf.si.racunovodstvo.nabavka.model.Konverzija;
import raf.si.racunovodstvo.nabavka.requests.KonverzijaRequest;
import raf.si.racunovodstvo.nabavka.responses.PreduzeceResponse;
import raf.si.racunovodstvo.nabavka.services.impl.KonverzijaService;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class KonverzijaRestControllerTest {

    @InjectMocks
    private KonverzijaRestController konverzijaRestController;
    @Mock
    private KonverzijaService konverzijaService;
    @Mock
    private PreduzeceFeignClient preduzeceFeignClient;

    private static final Long MOCK_ID = 1L;

    @Test
    void search() throws IOException {
        String search = "knjizenjeId:1";
        Integer page = 1;
        Integer size = 50;
        String[] sort = new String[1];
        sort[0] = "1";

        ResponseEntity<?> responseEntity = konverzijaRestController.search(search, page, size, sort);
        assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Test
    void createKonverzija() {
        KonverzijaRequest konverzija = new KonverzijaRequest();
        konverzija.setDobavljacId(MOCK_ID);
        given(preduzeceFeignClient.getPreduzeceById(MOCK_ID, "")).willReturn(ResponseEntity.ok(new PreduzeceResponse()));
        ResponseEntity<?> responseEntity = konverzijaRestController.createKonverzija(konverzija, "");
        assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Test
    void deleteKonverzija() {
        Konverzija konverzija = new Konverzija();
        konverzija.setId(MOCK_ID);
        given(konverzijaService.findById(MOCK_ID)).willReturn(Optional.of(konverzija));
        ResponseEntity<?> responseEntity = konverzijaRestController.deleteKonverzija(MOCK_ID);

        assertEquals(204, responseEntity.getStatusCodeValue());
    }
}
