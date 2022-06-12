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
import raf.si.racunovodstvo.nabavka.model.Artikal;
import raf.si.racunovodstvo.nabavka.model.Lokacija;
import raf.si.racunovodstvo.nabavka.repositories.ArtikalRepository;
import raf.si.racunovodstvo.nabavka.repositories.LokacijaRepository;
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
class LokacijaIntegrationTest {

    private final static String URI = "/api/lokacije";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private LokacijaRepository lokacijaRepository;

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
        List<Map<String, Object>> responseList = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
        List<Long> lokacijaList = responseList.stream().map(x -> Long.valueOf(x.get("lokacijaId").toString())).collect(Collectors.toList());
        List<Long> expectedOutput = artikalRepository.findAllById(lokacijaList).stream().map(Artikal::getArtikalId).collect(Collectors.toList());
        assertTrue(lokacijaList.containsAll(expectedOutput));
    }

    @Test
    @Order(2)
    void create() throws Exception {
        Lokacija request = new Lokacija();
        request.setNaziv("Lokacija 2");
        request.setAdresa("Mite Ruzica 3");
        String requestBody = gson.toJson(request);
        MvcResult result = mockMvc.perform(post(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = gson.fromJson(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
        assertTrue(responseMap.entrySet().containsAll(requestMap.entrySet()));
    }

    @Test
    @Order(3)
    void update() throws Exception {
        Lokacija lokacija = lokacijaRepository.findAll().get(0);
        lokacija.setNaziv("Novi naziv");
        lokacija.setBaznaKonverzijaKalkulacijaList(null);
        String requestBody = gson.toJson(lokacija);
        MvcResult result = mockMvc.perform(put(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        Map<String, Object> responseMap = gson.fromJson(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
        assertTrue(responseMap.entrySet().containsAll(requestMap.entrySet()));
    }

    @Test
    @Order(4)
    void updateNotFound() throws Exception {
        Lokacija request = new Lokacija();
        request.setLokacijaId(3443L);
        request.setNaziv("Lokacija 2");
        request.setAdresa("Mite Ruzica 3");
        String requestBody = gson.toJson(request);
        mockMvc.perform(put(URI).content(requestBody).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void deleteLokacija() throws Exception {
        Lokacija request = new Lokacija();
        request.setLokacijaId(3113L);
        request.setNaziv("Lokacija 2");
        request.setAdresa("Mite Ruzica 3");
        request = lokacijaRepository.save(request);
        Long id = request.getLokacijaId();
        mockMvc.perform(delete(URI + "/" + id.toString())).andExpect(status().is(204));
        Optional<Lokacija> deleted = lokacijaRepository.findById(id);
        assertTrue(deleted.isEmpty());
    }

    @Test
    @Order(4)
    void deleteNotFound() throws Exception {
        mockMvc.perform(delete(URI + "/194242")).andExpect(status().isNotFound());
    }
}
