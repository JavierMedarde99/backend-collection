package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class SecurityMatrixTest {

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private com.wikicollection.domain.port.in.UserPreferencesUseCase preferencesUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ExternalBookCatalogClient bookCatalog;

    private Book ownedBook() {
        Book book = new Book();
        book.setId("b1");
        book.setOwnerId("owner");
        book.setTitle("Dune");
        book.setAuthor("Herbert");
        book.setState(BookState.TO_READ);
        return book;
    }

    private void asNonAdmin(String userId, String username) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).username(username).build()));
    }

    @BeforeEach
    void stubOwnerResolver() {
        lenient().when(ownerResolver.resolveOwner(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> com.wikicollection.domain.model.UserOwned.builder()
                        .ownerId(invocation.getArgument(0)).ownerName("Javi").build());
    }

    @Test
    void getPublic_withoutAuth_returns200() throws Exception {
        when(bookRepository.search(any(), any())).thenReturn(Page.empty());
        when(preferencesUseCase.getUserIdsWithPrivateCollection(any()))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/books").param("owner", "other"))
                .andExpect(status().isOk());
    }

    @Test
    void postAnonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dune"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postAuthenticated_reachesController() throws Exception {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId("b-new");
            return saved;
        });

        mockMvc.perform(post("/api/v1/books")
                        .with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dune","author":"Herbert","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("b-new"));
    }

    @Test
    void putOtherOwnersBook_returns403() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(ownedBook()));
        asNonAdmin("u1", "intruso");

        mockMvc.perform(put("/api/v1/books/b1")
                        .with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dune Messiah","author":"Herbert","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOwnBook_returns204() throws Exception {
        Book own = ownedBook();
        own.setOwnerId("u1");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(own));

        mockMvc.perform(delete("/api/v1/books/b1").with(user("u1")))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanEditOthersBook() throws Exception {
        when(bookRepository.findById("b1")).thenReturn(Optional.of(ownedBook()));
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(
                User.builder().id("admin-id").username("admin").build()));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/books/b1")
                        .with(user("admin-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dune Messiah","author":"Herbert","state":"TO_READ","type":"NOVEL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dune Messiah"));
    }

    @Test
    void searchPublic_withoutAuth_returns200() throws Exception {
        when(bookCatalog.search(any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/books/search").param("name", "dune"))
                .andExpect(status().isOk());
    }
}
