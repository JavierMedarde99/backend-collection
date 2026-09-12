package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieStatus;

import org.junit.jupiter.api.Test;

class StringToEnumConvertersNullSafeTest {

    @Test
    void converters_returnNull_forNullAndBlank() {
        assertThat(new StringToGameStatusConverter().convert(null)).isNull();
        assertThat(new StringToGameStatusConverter().convert("  ")).isNull();
        assertThat(new StringToMovieStatusConverter().convert(null)).isNull();
        assertThat(new StringToMovieStatusConverter().convert("")).isNull();
        assertThat(new StringToMovieMediaTypeConverter().convert(null)).isNull();
        assertThat(new StringToMovieMediaTypeConverter().convert("  ")).isNull();
        assertThat(new StringToBookStateConverter().convert(null)).isNull();
        assertThat(new StringToBookStateConverter().convert("")).isNull();
        assertThat(new StringToBoardGameStatusConverter().convert(null)).isNull();
        assertThat(new StringToBoardGameStatusConverter().convert("  ")).isNull();
    }

    @Test
    void converters_stillMapValidValues() {
        assertThat(new StringToMovieStatusConverter().convert("watched")).isEqualTo(MovieStatus.WATCHED);
        assertThat(new StringToMovieMediaTypeConverter().convert("tv")).isEqualTo(MovieMediaType.TV);
        assertThat(new StringToBookStateConverter().convert("reading")).isEqualTo(BookState.READING);
        assertThat(new StringToBoardGameStatusConverter().convert("owned")).isEqualTo(BoardGameStatus.OWNED);
    }

    @Test
    void converters_rejectInvalidValues() {
        assertThatThrownBy(() -> new StringToMovieStatusConverter().convert("NO_EXISTE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
