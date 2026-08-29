package com.wikicollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wikicollection.dto.GoogleBookResult;
import com.wikicollection.model.Book;
import com.wikicollection.model.BookStatus;
import com.wikicollection.repository.BookRepository;
import com.wikicollection.service.GoogleBooksService;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private GoogleBooksService googleBooksService;

    private Book sampleBook() {
        Book book = new Book();
        book.setTitle("Cien años de soledad");
        book.setAuthors(List.of("Gabriel García Márquez"));
        book.setStatus(BookStatus.WISHLIST);
        return book;
    }

    @Test
    void listBooks_returnsEmptyPage_whenNoBooks() throws Exception {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listBooks_filtersByStatus() throws Exception {
        when(bookRepository.findByStatus(eq(BookStatus.READING), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books").param("status", "reading"))
                .andExpect(status().isOk());

        verify(bookRepository).findByStatus(eq(BookStatus.READING), any(Pageable.class));
    }

    @Test
    void listBooks_filtersByTag() throws Exception {
        when(bookRepository.findByTagsContaining(eq("ficcion"), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/books").param("tag", "ficcion"))
                .andExpect(status().isOk());

        verify(bookRepository).findByTagsContaining(eq("ficcion"), any(Pageable.class));
    }

    @Test
    void getBook_returnsBook_whenExists() throws Exception {
        Book book = sampleBook();
        book.setId("b1");
        book.setDateAdded(LocalDateTime.now());
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        mockMvc.perform(get("/api/books/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.title").value("Cien años de soledad"))
                .andExpect(jsonPath("$.status").value("WISHLIST"));
    }

    @Test
    void getBook_returns404_whenMissing() throws Exception {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBook_returns201_withLocation() throws Exception {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId("b-new");
            return saved;
        });

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cien años de soledad","authors":["Gabriel García Márquez"],"status":"WISHLIST"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/books/b-new")))
                .andExpect(jsonPath("$.id").value("b-new"))
                .andExpect(jsonPath("$.title").value("Cien años de soledad"));
    }

    @Test
    void createBook_returns400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","authors":[],"status":"WISHLIST"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBook_returnsUpdatedBook() throws Exception {
        Book existing = sampleBook();
        existing.setId("b1");
        existing.setDateAdded(LocalDateTime.now().minusDays(2));
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/books/b1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","authors":["Autor Actualizado"],"status":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo título"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteBook_returns204_whenExists() throws Exception {
        Book book = sampleBook();
        book.setId("b1");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        mockMvc.perform(delete("/api/books/b1"))
                .andExpect(status().isNoContent());

        verify(bookRepository).delete(book);
    }

    @Test
    void deleteBook_returns404_whenMissing() throws Exception {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/books/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsGoogleBooksResults() throws Exception {
        GoogleBookResult result = new GoogleBookResult(
                "abc123", "Cien años de soledad", List.of("Gabriel García Márquez"),
                "9780307474728", "http://thumb", "Sinopsis", 417,
                "Vintage Español", "2011-05-03", "es", List.of("Literatura"));
        when(googleBooksService.search("cien")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/books/search").param("q", "cien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Cien años de soledad"))
                .andExpect(jsonPath("$[0].isbn").value("9780307474728"));
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/books")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}