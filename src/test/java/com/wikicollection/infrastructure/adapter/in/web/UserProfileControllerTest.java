package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.in.UserProfileUseCase;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileUseCase profileUseCase;

    private User sampleUser() {
        return User.builder().id("u1").username("javi").displayName("Javi").build();
    }

    private void stubEmptyCollections() {
        when(profileUseCase.getPublicBooks(eq("javi"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 3));
        when(profileUseCase.getPublicGames(eq("javi"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(profileUseCase.getPublicBoardGames(eq("javi"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(profileUseCase.getPublicMagicCards(eq("javi"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(profileUseCase.getPublicDecks(eq("javi"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(profileUseCase.getPublicMovieShows(eq("javi"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
    }

    @Test
    void getProfile_returnsPublicDataWithCounts() throws Exception {
        when(profileUseCase.getPublicProfile("javi")).thenReturn(sampleUser());
        stubEmptyCollections();

        mockMvc.perform(get("/api/v1/users/javi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("javi"))
                .andExpect(jsonPath("$.displayName").value("Javi"))
                .andExpect(jsonPath("$.publicCollectionCounts.books").value(3));
    }

    @Test
    void getProfile_returns404_whenUnknown() throws Exception {
        when(profileUseCase.getPublicProfile("ghost"))
                .thenThrow(new UserNotFoundException("Usuario no encontrado: ghost"));

        mockMvc.perform(get("/api/v1/users/ghost"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPublicBooks_hidesPrivateFields_whenAnonymous() throws Exception {
        Book book = Book.builder().id("b1").ownerId("u1").title("Dune").author("Herbert")
                .state(BookState.TO_READ).type(BookType.NOVEL).comment("privado").build();
        when(profileUseCase.getPublicBooks(eq("javi"), any()))
                .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/javi/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Dune"))
                .andExpect(jsonPath("$.content[0].comment").value(Matchers.nullValue()));
    }
}
