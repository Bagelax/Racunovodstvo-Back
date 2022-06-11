package raf.si.racunovodstvo.user.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import raf.si.racunovodstvo.user.config.EmbeddedMysqlServerConfig;
import raf.si.racunovodstvo.user.model.Permission;
import raf.si.racunovodstvo.user.model.User;
import raf.si.racunovodstvo.user.repositories.PermissionRepository;
import raf.si.racunovodstvo.user.repositories.UserRepository;
import raf.si.racunovodstvo.user.utils.JwtUtil;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true", "eureka.client.enabled=false"},
                classes = {EmbeddedMysqlServerConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PermissionIntegrationTest {

    private final static String URI = "/api/permissions";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private JwtUtil jwtUtil;

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

        User user = new User();
        user.setUsername(MOCK_UID);
        user.setEmail(MOCK_EMAIL);
        user.setPassword(MOCK_PASSWORD);
        user.setPermissions(permissionRepository.findAll());
        userRepository.save(user);
        jwtToken = jwtUtil.generateToken(user.getUsername());
    }

    @Test
    void findAllTest() throws Exception {
        MvcResult mvcResult =
            mockMvc.perform(get(URI + "/all").header("Authorization", "Bearer " + jwtToken)).andExpect(status().isOk()).andReturn();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Object> objectList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
        });
        List<Long> permissions =
            objectList.stream().map(o -> objectMapper.convertValue(o, Permission.class).getId()).collect(Collectors.toList());
        List<Long> expectedList =
            permissionRepository.findAll().stream().map(Permission::getId).collect(Collectors.toList());
        assertEquals(permissions.size(), expectedList.size());
        assertTrue(permissions.containsAll(expectedList));
    }

    @Test
    void findAllUnauthorizedTest() throws Exception {
        mockMvc.perform(get(URI + "/all")).andExpect(status().isForbidden());
    }
}
