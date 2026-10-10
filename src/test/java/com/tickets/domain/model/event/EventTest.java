package com.tickets.domain.model.event;

import com.tickets.domain.exception.InvalidEventException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final String NAME = "Concierto Rock";
    private static final LocalDate DATE = LocalDate.of(2026, 12, 15);
    private static final String PLACE = "Estadio Nacional";
    private static final int TOTAL_CAPACITY = 5000;

    @Test
    void create_withValidInputs_returnsEventWithGeneratedId() {
        Event event = Event.create(NAME, DATE, PLACE, TOTAL_CAPACITY);

        assertThat(event.id()).isNotNull();
        assertThat(event.name()).isEqualTo(NAME);
        assertThat(event.date()).isEqualTo(DATE);
        assertThat(event.place()).isEqualTo(PLACE);
        assertThat(event.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
    }

    @Test
    void create_withBlankName_throwsInvalidEventException() {
        assertThatThrownBy(() -> Event.create("", DATE, PLACE, TOTAL_CAPACITY))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_withNullName_throwsInvalidEventException() {
        assertThatThrownBy(() -> Event.create(null, DATE, PLACE, TOTAL_CAPACITY))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_withNullDate_throwsInvalidEventException() {
        assertThatThrownBy(() -> Event.create(NAME, null, PLACE, TOTAL_CAPACITY))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("date");
    }

    @Test
    void create_withBlankPlace_throwsInvalidEventException() {
        assertThatThrownBy(() -> Event.create(NAME, DATE, "", TOTAL_CAPACITY))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("place");
    }

    @Test
    void create_withZeroCapacity_throwsInvalidEventException() {
        assertThatThrownBy(() -> Event.create(NAME, DATE, PLACE, 0))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void create_withNegativeCapacity_throwsInvalidEventException() {
        assertThatThrownBy(() -> Event.create(NAME, DATE, PLACE, -1))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void reconstitute_rebuildsEventWithGivenId() {
        EventId id = EventId.generate();

        Event event = Event.reconstitute(id, NAME, DATE, PLACE, TOTAL_CAPACITY);

        assertThat(event.id()).isEqualTo(id);
        assertThat(event.name()).isEqualTo(NAME);
        assertThat(event.date()).isEqualTo(DATE);
        assertThat(event.place()).isEqualTo(PLACE);
        assertThat(event.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
    }

    @Test
    void eventsWithSameId_areEqual() {
        EventId id = EventId.generate();
        Event event1 = Event.reconstitute(id, NAME, DATE, PLACE, TOTAL_CAPACITY);
        Event event2 = Event.reconstitute(id, "Other", LocalDate.of(2027, 1, 1), "Other", 100);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void eventsWithDifferentId_areNotEqual() {
        Event event1 = Event.create(NAME, DATE, PLACE, TOTAL_CAPACITY);
        Event event2 = Event.create(NAME, DATE, PLACE, TOTAL_CAPACITY);

        assertThat(event1).isNotEqualTo(event2);
    }
}
