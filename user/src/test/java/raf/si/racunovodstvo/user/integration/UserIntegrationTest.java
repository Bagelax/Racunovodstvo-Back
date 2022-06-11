package raf.si.racunovodstvo.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import raf.si.racunovodstvo.user.config.EmbeddedMysqlServerConfig;
import raf.si.racunovodstvo.user.configuration.SpringSecurityConfig;
import raf.si.racunovodstvo.user.controllers.UserRestController;
import raf.si.racunovodstvo.user.filters.JwtFilter;
import raf.si.racunovodstvo.user.model.User;
import raf.si.racunovodstvo.user.repositories.PermissionRepository;
import raf.si.racunovodstvo.user.repositories.UserRepository;
import raf.si.racunovodstvo.user.utils.JwtUtil;

import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true", "eureka.client.enabled=false"},
                classes = {EmbeddedMysqlServerConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserIntegrationTest {

    private final static String URI = "/api/users";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private MockMvc mockMvc;

    private static final String MOCK_UID = "MOCK_UID";
    private static final String MOCK_EMAIL = "MOCK_EMAIL";
    private static final String MOCK_PASSWORD = "MOCK_PASSWORD";
    private static final String MOCK_UID_2 = "MOCK_UID_2";
    private static final String MOCK_EMAIL_2 = "MOCK_EMAIL_2";
    private static final Long MOCK_PREDUZECE_ID = 1L;
    private String jwtToken;

    @BeforeAll
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        mockServer = MockRestServiceServer.createServer(restTemplate);

        User user = new User();
        user.setUsername(MOCK_UID);
        user.setEmail(MOCK_EMAIL);
        user.setPassword(passwordEncoder.encode(MOCK_PASSWORD));
        user.setPermissions(permissionRepository.findAll());
        userRepository.save(user);
        jwtToken = jwtUtil.generateToken(user.getUsername());
    }
/*
    @Test
    @Order(1)
    void saveUserTest() throws Exception {
        User user = new User();
        user.setUsername(MOCK_UID_2);
        user.setEmail(MOCK_EMAIL_2);
        user.setPassword(MOCK_PASSWORD);
        user.setPreduzeceId(MOCK_PREDUZECE_ID);

        String body = "{\"preduzeceId\":1}";

        mockServer.expect(ExpectedCount.once(), requestTo(anyString()))
                  .andExpect(method(HttpMethod.GET))
                  .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(body));

        ObjectMapper mapper = new ObjectMapper();
        String requestJson = mapper.writeValueAsString(user);

        mockMvc.perform(post(URI).header("Authorization", "Bearer " + jwtToken).contentType(APPLICATION_JSON).content(requestJson))
               .andExpect(status().isOk());

        mockServer.verify();
    }

 */

    @Test
    @Order(1)
    void getAllTest() throws Exception {
        mockMvc.perform(get(URI + "/all").header("Authorization", "Bearer " + jwtToken)).andExpect(status().isOk());
    }

    @Test
    @Order(1)
    void getAllUnauthenticatedTest() throws Exception {
        mockMvc.perform(get(URI + "/all")).andExpect(status().isForbidden());
    }

    @Test
    @Order(1)
    void getByIdTest() throws Exception {
        Long userId = userRepository.findByUsername(MOCK_UID).get().getUserId();
        mockMvc.perform(get(URI).param("userId", userId.toString()).header("Authorization", "Bearer " + jwtToken))
               .andExpect(status().isOk());
    }

    @Test
    @Order(1)
    void getByIdNotFoundTest() throws Exception {
        mockMvc.perform(get(URI).param("userId", "-1").header("Authorization", "Bearer " + jwtToken)).andExpect(status().isNotFound());
    }

    @Test
    @Order(1)
    void getLoginUserTest() throws Exception {
        mockMvc.perform(get(URI + "/loginuser").header("Authorization", "Bearer " + jwtToken)).andExpect(status().isOk());
    }

    @Test
    @Order(1)
    void getLoginUserUnauthenticatedTest() throws Exception {
        mockMvc.perform(get(URI + "/loginuser")).andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    void deleteUserNotFoundTest() throws Exception {
        mockMvc.perform(delete(URI + "/" + -1).header("Authorization", "Bearer " + jwtToken)).andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    void deleteUserTest() throws Exception {
        Long userId = userRepository.findByUsername(MOCK_UID).get().getUserId();
        mockMvc.perform(delete(URI + "/" + userId).header("Authorization", "Bearer " + jwtToken)).andExpect(status().isNoContent());
        assertTrue(userRepository.findByUsername(MOCK_UID).isEmpty());
    }
}
