package raf.si.racunovodstvo.user.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import raf.si.racunovodstvo.user.model.Preduzece;
import raf.si.racunovodstvo.user.model.User;
import raf.si.racunovodstvo.user.requests.UpdateUserRequest;
import raf.si.racunovodstvo.user.services.impl.UserService;

import javax.persistence.EntityNotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserRestControllerTest {

    @InjectMocks
    private UserRestController userRestController;
    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RestTemplate restTemplate;

    private static final Long MOCK_ID = 1L;
    private static final String MOCK_USERNAME = "DUMMY_USERNAME";
    private static final String TOKEN = "TOKEN";



    @Test
    void getLoginUserSuccessTest() {
        User user = new User();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(MOCK_USERNAME, ""));
        given(userService.findByUsername(MOCK_USERNAME)).willReturn(Optional.of(user));

        assertEquals(user, userRestController.getLoginUser().getBody());
    }

    @Test
    void getLoginUserFailTest() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(MOCK_USERNAME, ""));
        given(userService.findByUsername(MOCK_USERNAME)).willReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userRestController.getLoginUser());
    }

    @Test
    void getAllUsers() {
        given(userService.findAll()).willReturn(new ArrayList<>());
        ResponseEntity<?> responseEntity = userRestController.getAllUsers();

        assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Test
    void getUserById() {
        User user = new User();
        given(userService.findById(MOCK_ID)).willReturn(Optional.of(user));
        ResponseEntity<?> responseEntity = userRestController.getUserById(MOCK_ID);

        assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Test
    void getUserByIdException() {
        given(userService.findById(MOCK_ID)).willReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userRestController.getUserById(MOCK_ID));
    }

    @Test
    void createUser() throws IOException {
        User user = new User();
        user.setPreduzeceId(MOCK_ID);
        List<User> userList = new ArrayList<>();
        String body = "{\"preduzeceId\":1}";

        given(userService.findAll()).willReturn(userList);
        given(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).willReturn(ResponseEntity.ok(body));

        ResponseEntity<?> responseEntity = userRestController.createUser(user, TOKEN);

        assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Test
    void createUserExceptionWithExistingUsername() throws IOException {
        User user = new User();
        user.setUsername("username");
        List<User> userList = new ArrayList<>();
        userList.add(user);
        given(userService.findAll()).willReturn(userList);

        ResponseEntity<?> responseEntity = userRestController.createUser(user, TOKEN);

        assertEquals(403, responseEntity.getStatusCodeValue());
    }

    @Test
    void createUserExceptionWithNullPreduzeceId() throws IOException {
        User user = new User();
        List<User> userList = new ArrayList<>();
        given(userService.findAll()).willReturn(userList);

        ResponseEntity<?> responseEntity = userRestController.createUser(user, TOKEN);

        assertEquals(404, responseEntity.getStatusCodeValue());
    }

    @Test
    void updateUser() throws IOException {
        User user = new User();
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setUserId(MOCK_ID);
        updateUserRequest.setPreduzeceId(MOCK_ID);
        String body = "{\"preduzeceId\":1}";

        given(userService.findById(MOCK_ID)).willReturn(Optional.of(user));
        given(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).willReturn(ResponseEntity.ok(body));

        ResponseEntity<?> responseEntity = userRestController.updateUser(updateUserRequest, TOKEN);

        assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Test
    void updateUserException() {
        User user = new User();
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setUserId(MOCK_ID);
        given(userService.findById(MOCK_ID)).willReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userRestController.updateUser(updateUserRequest, TOKEN));
    }

    @Test
    void deleteUser() {
        User user = new User();
        given(userService.findById(MOCK_ID)).willReturn(Optional.of(user));

        ResponseEntity<?> responseEntity = userRestController.deleteUser(MOCK_ID);

        assertEquals(204, responseEntity.getStatusCodeValue());
    }

    @Test
    void deleteUserException() {
        given(userService.findById(MOCK_ID)).willReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userRestController.deleteUser(MOCK_ID));
    }
}
