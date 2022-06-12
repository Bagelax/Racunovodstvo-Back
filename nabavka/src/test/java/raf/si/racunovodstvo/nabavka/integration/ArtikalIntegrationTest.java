package raf.si.racunovodstvo.nabavka.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
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
import raf.si.racunovodstvo.nabavka.model.Artikal;
import raf.si.racunovodstvo.nabavka.model.BaznaKonverzijaKalkulacija;
import raf.si.racunovodstvo.nabavka.model.Kalkulacija;
import raf.si.racunovodstvo.nabavka.model.Konverzija;
import raf.si.racunovodstvo.nabavka.repositories.ArtikalRepository;
import raf.si.racunovodstvo.nabavka.requests.ArtikalRequest;
import raf.si.racunovodstvo.nabavka.services.impl.KalkulacijaService;
import raf.si.racunovodstvo.nabavka.services.impl.KonverzijaService;

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
class ArtikalIntegrationTest {

    private final static String URI = "/api/artikli";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private KonverzijaService konverzijaService;

    @Autowired
    private KalkulacijaService kalkulacijaService;

    @Autowired
    private ArtikalRepository artikalRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Gson gson;

    @BeforeAll
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        objectMapper = new ObjectMapper();
        gson = new Gson();
    }

    @Test
    @Order(1)
    void getAllTest() throws Exception {
        MvcResult result = mockMvc.perform(get(URI)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
        List<Long> artikalList = ((List<Map<String, Object>>) responseMap.get("content")).stream().map(x -> Long.valueOf(x.get("artikalId").toString())).collect(Collectors.toList());
        List<Long> expectedOutput = artikalRepository.findAllById(artikalList).stream().map(Artikal::getArtikalId).collect(Collectors.toList());
        assertTrue(artikalList.containsAll(expectedOutput));
    }

    @Test
    @Order(1)
    void getById() throws Exception {
        Kalkulacija k1 = kalkulacijaService.findAll().get(0);
        MvcResult result = mockMvc.perform(get(URI + "/" + k1.getId())).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
        List<Long> artikalList = ((List<Map<String, Object>>) responseMap.get("content")).stream().map(x -> Long.valueOf(x.get("artikalId").toString())).collect(Collectors.toList());
        List<Long> expectedOutput = artikalRepository.findAllById(artikalList).stream().map(Artikal::getBaznaKonverzijaKalkulacija).map(BaznaKonverzijaKalkulacija::getId).collect(Collectors.toList());
        assertTrue(expectedOutput.stream().map(x -> x == 1).reduce((x, y) -> x & y).get());
    }

    @Test
    @Order(1)
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get(URI + "/SomethingRandom")).andExpect(status().is(400));
    }

    @Test
    @Order(2)
    void create() throws Exception {
        ArtikalRequest request = new ArtikalRequest();
        request.setSifraArtikla("14412AB");
        request.setNazivArtikla("Carape");
        request.setJedinicaMere("Komad");
        request.setKolicina(1);
        request.setNabavnaCena(200.0);
        request.setRabatProcenat(2.0);
        Konverzija k = konverzijaService.findAll().get(0);
        request.setKonverzijaKalkulacijaId(k.getId());
        String requestBody = gson.toJson(request);
        MvcResult result = mockMvc.perform(post(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = gson.fromJson(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
        requestMap.remove("aktivanZaProdaju"); // Not returned from BE
        assertTrue(responseMap.entrySet().containsAll(requestMap.entrySet()));
    }

    @Test
    @Order(3)
    void update() throws Exception {
        Artikal artikal = artikalRepository.findAll().get(0);
        ArtikalRequest request = new ArtikalRequest();
        request.setArtikalId(artikal.getArtikalId());
        request.setSifraArtikla("14412AB13");
        request.setNazivArtikla("Carape");
        request.setJedinicaMere("Komad");
        request.setKolicina(1);
        request.setNabavnaCena(200.0);
        request.setRabatProcenat(2.0);
        Konverzija k = konverzijaService.findAll().get(0);
        request.setKonverzijaKalkulacijaId(k.getId());
        String requestBody = gson.toJson(request);
        MvcResult result = mockMvc.perform(put(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = gson.fromJson(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
        requestMap.remove("aktivanZaProdaju"); // Not returned from BE
        assertTrue(responseMap.entrySet().containsAll(requestMap.entrySet()));
    }

    @Test
    @Order(4)
    void updateNotFound() throws Exception {
        ArtikalRequest request = new ArtikalRequest();
        request.setArtikalId(25356L);
        request.setSifraArtikla("14412AB");
        request.setNazivArtikla("Carape");
        request.setJedinicaMere("Komad");
        request.setKolicina(1);
        request.setNabavnaCena(200.0);
        request.setRabatProcenat(2.0);
        Konverzija k = konverzijaService.findAll().get(0);
        request.setKonverzijaKalkulacijaId(k.getId());
        String requestBody = gson.toJson(request);
        mockMvc.perform(put(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void deleteArtikal() throws Exception {
        Artikal artikal = artikalRepository.findAll().get(0);
        Long id = artikal.getArtikalId();
        mockMvc.perform(delete(URI + "/" + id.toString())).andExpect(status().is(204));
        Optional<Artikal> deleted = artikalRepository.findById(id);
        assertTrue(deleted.isEmpty());
    }

    @Test
    @Order(4)
    void deleteNotFound() throws Exception {
        mockMvc.perform(delete(URI + "/194242")).andExpect(status().isNotFound());
    }
}
