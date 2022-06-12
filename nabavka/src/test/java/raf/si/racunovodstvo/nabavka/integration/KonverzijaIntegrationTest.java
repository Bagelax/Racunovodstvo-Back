package raf.si.racunovodstvo.nabavka.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import raf.si.racunovodstvo.nabavka.config.EmbeddedMysqlServerConfig;
import raf.si.racunovodstvo.nabavka.model.*;
import raf.si.racunovodstvo.nabavka.repositories.KonverzijaRepository;
import raf.si.racunovodstvo.nabavka.requests.KonverzijaRequest;
import raf.si.racunovodstvo.nabavka.requests.LokacijaRequest;
import raf.si.racunovodstvo.nabavka.requests.TroskoviNabavkeRequest;
import raf.si.racunovodstvo.nabavka.services.impl.LokacijaService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true", "eureka.client.enabled=false"},
                classes = {EmbeddedMysqlServerConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KonverzijaIntegrationTest {

    private final static String URI = "/api/konverzije";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private LokacijaService lokacijaService;

    @Autowired
    private KonverzijaRepository konverzijaRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Gson gson;

    @BeforeAll
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        objectMapper = new ObjectMapper();
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    }

    @Test
    @Order(1)
    void getAllTest() throws Exception {
        MvcResult result = mockMvc.perform(get(URI)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
        List<Long> konverzijaList = ((List<Map<String, Object>>) responseMap.get("content")).stream().map(x -> Long.valueOf(x.get("konverzijaId").toString())).collect(Collectors.toList());
        List<Long> expectedOutput = konverzijaRepository.findAllById(konverzijaList).stream().map(Konverzija::getId).collect(Collectors.toList());
        assertTrue(konverzijaList.containsAll(expectedOutput));
    }

    @Order(2)
    void create() throws Exception {
        KonverzijaRequest request = new KonverzijaRequest();
        Konverzija k = konverzijaRepository.findAll().get(0);
        request.setBrojKonverzije(k.getBrojKonverzije());
        request.setDatum(new Date());
        request.setValuta("RSD");
        Lokacija lokacija = lokacijaService.findAll().get(0);
        request.setLokacija(new LokacijaRequest(lokacija.getLokacijaId(), lokacija.getNaziv(), lokacija.getAdresa()));
        TroskoviNabavkeRequest troskoviNabavke = new TroskoviNabavkeRequest();
        troskoviNabavke.setNaziv("Trosak1");
        troskoviNabavke.setCena(24.0);
        request.setTroskoviNabavke(List.of(troskoviNabavke));
        request.setDobavljacId(1L);
        String requestBody = gson.toJson(request);
        MvcResult result = mockMvc.perform(post(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON).header("Authorization", "")).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = gson.fromJson(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
        requestMap.remove("datum");
        requestMap.remove("troskoviNabavke"); // One of the maps lacks id
        assertTrue(responseMap.entrySet().containsAll(requestMap.entrySet()));
    }

    @Test
    @Order(3)
    void updateNotFound() throws Exception {
        KonverzijaRequest request = new KonverzijaRequest();
        Konverzija k = konverzijaRepository.findAll().get(0);
        request.setId(145536L);
        request.setBrojKonverzije(k.getBrojKonverzije());
        request.setDatum(new Date());
        request.setValuta("RSD");
        Lokacija lokacija = lokacijaService.findAll().get(0);
        request.setLokacija(new LokacijaRequest(lokacija.getLokacijaId(), lokacija.getNaziv(), lokacija.getAdresa()));
        TroskoviNabavkeRequest troskoviNabavke = new TroskoviNabavkeRequest();
        troskoviNabavke.setNaziv("Trosak1");
        troskoviNabavke.setCena(24.0);
        request.setTroskoviNabavke(List.of(troskoviNabavke));
        request.setDobavljacId(1L);
        String requestBody = gson.toJson(request);
        mockMvc.perform(post(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON).header("Authorization", "")).andExpect(status().is(400));
    }

    @Test
    @Order(4)
    void deleteKonverzija() throws Exception {
        Konverzija konverzija = new Konverzija();
        konverzija.setBrojKonverzije("T13452");
        konverzija.setDatum(new Date());
        konverzija.setValuta("RSD");
        konverzija.setDobavljacId(1L);
        konverzija.setFakturnaCena(24.4);
        konverzija.setNabavnaVrednost(24.1);
        konverzija = konverzijaRepository.save(konverzija);
        Long id = konverzija.getId();
        mockMvc.perform(delete(URI + "/" + id.toString()).header("Authorization", "")).andExpect(status().is(204));
        Optional<Konverzija> deleted = konverzijaRepository.findById(id);
        assertTrue(deleted.isEmpty());
    }

    @Test
    @Order(3)
    void deleteNotFound() throws Exception {
        mockMvc.perform(delete(URI + "/194242").header("Authorization", "")).andExpect(status().isNotFound());
    }
}
