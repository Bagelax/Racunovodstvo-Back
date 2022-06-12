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
import raf.si.racunovodstvo.nabavka.model.enums.TipKalkulacije;
import raf.si.racunovodstvo.nabavka.repositories.ArtikalRepository;
import raf.si.racunovodstvo.nabavka.repositories.KalkulacijaRepository;
import raf.si.racunovodstvo.nabavka.repositories.TroskoviNabavkeRepository;
import raf.si.racunovodstvo.nabavka.requests.KalkulacijaRequest;
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
class KalkulacijaIntegrationTest {

    private final static String URI = "/api/kalkulacije";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private LokacijaService lokacijaService;

    @Autowired
    private KalkulacijaRepository kalkulacijaRepository;

    @Autowired
    private TroskoviNabavkeRepository troskoviNabavkeRepository;

    @Autowired
    private ArtikalRepository artikalRepository;

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
        List<Long> artikalList = ((List<Map<String, Object>>) responseMap.get("content")).stream().map(x -> Long.valueOf(x.get("id").toString())).collect(Collectors.toList());
        List<Long> expectedOutput = artikalRepository.findAllById(artikalList).stream().map(Artikal::getArtikalId).collect(Collectors.toList());
        assertTrue(artikalList.containsAll(expectedOutput));
    }

    @Test
    @Order(1)
    void getAll2Test() throws Exception {
        MvcResult result = mockMvc.perform(get(URI + "/all")).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
        List<Long> artikalList = ((List<Map<String, Object>>) responseMap.get("content")).stream().map(x -> Long.valueOf(x.get("id").toString())).collect(Collectors.toList());
        List<Long> expectedOutput = artikalRepository.findAllById(artikalList).stream().map(Artikal::getArtikalId).collect(Collectors.toList());
        assertTrue(artikalList.containsAll(expectedOutput));
    }

    @Test
    @Order(1)
    void total() throws Exception {
        MvcResult result = mockMvc.perform(get(URI + "/total")).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
        assertTrue(responseMap.keySet().containsAll(List.of(
                "totalKolicina",
                "totalRabat",
                "totalNabavnaCena",
                "totalNabavnaCenaPosleRabata",
                "totalNabavnaVrednost",
                "totalMarza",
                "totalOsnovicaZaProdaju",
                "totalPorez",
                "totalProdajnaCena",
                "totalPoreskaOsnovica",
                "totalProdajnaVrednost"
        )));
    }

    @Test
    @Order(2)
    void create() throws Exception {
        KalkulacijaRequest request = new KalkulacijaRequest();
        request.setDatum(new Date());
        request.setValuta("RSD");
        request.setBrojKalkulacije("AA21");
        request.setTipKalkulacije(TipKalkulacije.MALOPRODAJA);
        Lokacija lokacija = lokacijaService.findAll().get(0);
        request.setLokacija(new LokacijaRequest(lokacija.getLokacijaId(), lokacija.getNaziv(), lokacija.getAdresa()));
        TroskoviNabavkeRequest troskoviNabavke = new TroskoviNabavkeRequest();
        troskoviNabavke.setNaziv("Trosak1");
        troskoviNabavke.setCena(24.0);
        request.setTroskoviNabavke(List.of(troskoviNabavke));
        request.setDobavljacId(1L);
        String requestBody = gson.toJson(request);
        MvcResult result = mockMvc.perform(post(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = gson.fromJson(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
        requestMap.remove("datum");
        requestMap.remove("troskoviNabavke"); // One of the maps lacks id
        assertTrue(responseMap.entrySet().containsAll(requestMap.entrySet()));
    }

    @Test
    @Order(3)
    void update() throws Exception {
        Kalkulacija kalkulacija = kalkulacijaRepository.findAll().get(0);
        KalkulacijaRequest request = new KalkulacijaRequest();
        request.setId(kalkulacija.getId());
        request.setDatum(new Date());
        request.setValuta("USD");
        request.setBrojKalkulacije("AA21MB");
        request.setTipKalkulacije(TipKalkulacije.MALOPRODAJA);
        Lokacija lokacija = kalkulacija.getLokacija();
        request.setLokacija(new LokacijaRequest(lokacija.getLokacijaId(), lokacija.getNaziv(), lokacija.getAdresa()));
        TroskoviNabavkeRequest troskoviNabavke = new TroskoviNabavkeRequest();
        TroskoviNabavke trn = troskoviNabavkeRepository.findAll().get(0);
        troskoviNabavke.setTroskoviNabavkeId(trn.getTroskoviNabavkeId());
        troskoviNabavke.setNaziv(trn.getNaziv());
        troskoviNabavke.setCena(trn.getCena());
        request.setTroskoviNabavke(List.of(troskoviNabavke));
        request.setDobavljacId(1L);
        String requestBody = gson.toJson(request);
        MvcResult result = mockMvc.perform(put(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = gson.fromJson(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
        requestMap.remove("datum");
        requestMap.remove("troskoviNabavke"); // One of the maps lacks id
        assertTrue(responseMap.entrySet().containsAll(requestMap.entrySet()));
    }

    @Test
    @Order(4)
    void updateNotFound() throws Exception {
        Kalkulacija kalkulacija = kalkulacijaRepository.findAll().get(0);
        KalkulacijaRequest request = new KalkulacijaRequest();
        request.setId(31345L);
        request.setDatum(new Date());
        request.setValuta("USD");
        request.setBrojKalkulacije("AA21");
        request.setTipKalkulacije(TipKalkulacije.MALOPRODAJA);
        Lokacija lokacija = kalkulacija.getLokacija();
        request.setLokacija(new LokacijaRequest(lokacija.getLokacijaId(), lokacija.getNaziv(), lokacija.getAdresa()));
        TroskoviNabavkeRequest troskoviNabavke = new TroskoviNabavkeRequest();
        TroskoviNabavke trn = troskoviNabavkeRepository.findAll().get(0);
        troskoviNabavke.setTroskoviNabavkeId(trn.getTroskoviNabavkeId());
        troskoviNabavke.setNaziv(trn.getNaziv());
        troskoviNabavke.setCena(trn.getCena());
        request.setTroskoviNabavke(List.of(troskoviNabavke));
        request.setDobavljacId(1L);
        String requestBody = gson.toJson(request);
        mockMvc.perform(put(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void deleteKalkulacija() throws Exception {

        Kalkulacija kalkulacija = new Kalkulacija();
        kalkulacija.setBrojKalkulacije("BLAH");
        kalkulacija.setProdajnaCena(42.3);
        kalkulacija.setTipKalkulacije(TipKalkulacije.MALOPRODAJA);
        kalkulacija.setBrojKalkulacije("3142");
        kalkulacija.setDatum(new Date());
        kalkulacija.setDobavljacId(1L);
        kalkulacija.setNabavnaVrednost(3.4);
        kalkulacija.setFakturnaCena(42.5);
        kalkulacija.setValuta("RSD");
        kalkulacija = kalkulacijaRepository.save(kalkulacija);
        Long id = kalkulacija.getId();
        mockMvc.perform(delete(URI + "/" + id.toString())).andExpect(status().is(204));
        Optional<Kalkulacija> deleted = kalkulacijaRepository.findById(id);
        assertTrue(deleted.isEmpty());
    }

    @Test
    @Order(4)
    void deleteNotFound() throws Exception {
        mockMvc.perform(delete(URI + "/194242")).andExpect(status().isNotFound());
    }
}
