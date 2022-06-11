package raf.si.racunovodstvo.user.integration;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import raf.si.racunovodstvo.user.config.EmbeddedMysqlServerConfig;
import raf.si.racunovodstvo.user.model.User;
import raf.si.racunovodstvo.user.repositories.UserRepository;
import raf.si.racunovodstvo.user.requests.LoginRequest;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true",
                classes = EmbeddedMysqlServerConfig.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    private final static String URI = "/auth";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private static final String MOCK_UID = "MOCK_UID";
    private static final String MOCK_EMAIL = "MOCK_EMAIL";
    private static final String MOCK_PASSWORD = "MOCK_PASSWORD";
    private String jwtToken = "";

    @BeforeAll
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    }

    @Test
    @Order(1)
    void loginTest() throws Exception {
        User user = new User();
        user.setUsername(MOCK_UID);
        user.setEmail(MOCK_EMAIL);
        user.setPassword(passwordEncoder.encode(MOCK_PASSWORD));
        userRepository.save(user);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword(MOCK_PASSWORD);
        loginRequest.setUsername(MOCK_UID);
        ObjectMapper mapper = new ObjectMapper();
        String requestJson = mapper.writeValueAsString(loginRequest);

        String result = mockMvc.perform(post(URI + "/login").contentType(APPLICATION_JSON).content(requestJson))
                               .andExpect(status().isOk())
                               .andReturn()
                               .getResponse()
                               .getContentAsString();
        Map<String, String> resultMap = mapper.readValue(result, new TypeReference<>() {
        });
        jwtToken = resultMap.get("jwt");
        System.out.println(jwtToken);
    }

    @Test
    @Order(2)
    void accessTest() throws Exception {
        mockMvc.perform(get(URI + "/access").header("Authorization", "Bearer " + jwtToken)).andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void accessUnauthenticatedTest() throws Exception {
        mockMvc.perform(get(URI + "/access")).andExpect(status().isForbidden());
    }
}
