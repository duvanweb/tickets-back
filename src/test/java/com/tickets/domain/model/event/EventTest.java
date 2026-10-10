package com.tickets.domain.model.event;

import com.tickets.domain.exception.InvalidEventException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final String NAME = "Concierto Rock";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 12, 15, 19, 30);
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
    void create_initializesCounters() {
        Event event = Event.create(NAME, DATE, PLACE, TOTAL_CAPACITY);

        assertThat(event.available()).isEqualTo(TOTAL_CAPACITY);
        assertThat(event.reserved()).isZero();
        assertThat(event.sold()).isZero();
        assertThat(event.complimentary()).isZero();
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
    void reconstitute_rebuildsEventWithGivenIdAndCounters() {
        EventId id = EventId.generate();

        Event event = Event.reconstitute(id, NAME, DATE, PLACE, TOTAL_CAPACITY,
                3000, 1000, 800, 200);

        assertThat(event.id()).isEqualTo(id);
        assertThat(event.name()).isEqualTo(NAME);
        assertThat(event.date()).isEqualTo(DATE);
        assertThat(event.place()).isEqualTo(PLACE);
        assertThat(event.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
        assertThat(event.available()).isEqualTo(3000);
        assertThat(event.reserved()).isEqualTo(1000);
        assertThat(event.sold()).isEqualTo(800);
        assertThat(event.complimentary()).isEqualTo(200);
    }

    @Test
    void reserve_withValidQuantity_adjustsCounters() {
        Event event = Event.create(NAME, DATE, PLACE, 100);

        Event reserved = event.reserve(30);

        assertThat(reserved.available()).isEqualTo(70);
        assertThat(reserved.reserved()).isEqualTo(30);
        assertThat(reserved.sold()).isZero();
        assertThat(reserved.complimentary()).isZero();
    }

    @Test
    void reserve_withInsufficientAvailable_throwsInvalidEventException() {
        Event event = Event.create(NAME, DATE, PLACE, 10);

        assertThatThrownBy(() -> event.reserve(11))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("Not enough available");
    }

    @Test
    void reserve_withZeroQuantity_throwsInvalidEventException() {
        Event event = Event.create(NAME, DATE, PLACE, 10);

        assertThatThrownBy(() -> event.reserve(0))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void confirmSale_withValidQuantity_adjustsCounters() {
        Event event = Event.create(NAME, DATE, PLACE, 100).reserve(30);

        Event sold = event.confirmSale(20);

        assertThat(sold.available()).isEqualTo(70);
        assertThat(sold.reserved()).isEqualTo(10);
        assertThat(sold.sold()).isEqualTo(20);
    }

    @Test
    void confirmSale_withInsufficientReserved_throwsInvalidEventException() {
        Event event = Event.create(NAME, DATE, PLACE, 100).reserve(10);

        assertThatThrownBy(() -> event.confirmSale(11))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("Not enough reserved");
    }

    @Test
    void release_withValidQuantity_adjustsCounters() {
        Event event = Event.create(NAME, DATE, PLACE, 100).reserve(30);

        Event released = event.release(10);

        assertThat(released.available()).isEqualTo(80);
        assertThat(released.reserved()).isEqualTo(20);
    }

    @Test
    void release_withInsufficientReserved_throwsInvalidEventException() {
        Event event = Event.create(NAME, DATE, PLACE, 100).reserve(10);

        assertThatThrownBy(() -> event.release(11))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("Not enough reserved");
    }

    @Test
    void grantComplimentary_withValidQuantity_adjustsCounters() {
        Event event = Event.create(NAME, DATE, PLACE, 100);

        Event complimentary = event.grantComplimentary(5);

        assertThat(complimentary.available()).isEqualTo(95);
        assertThat(complimentary.complimentary()).isEqualTo(5);
    }

    @Test
    void grantComplimentary_withInsufficientAvailable_throwsInvalidEventException() {
        Event event = Event.create(NAME, DATE, PLACE, 10);

        assertThatThrownBy(() -> event.grantComplimentary(11))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("Not enough available");
    }

    @Test
    void adjustmentMethods_returnNewInstance() {
        Event event = Event.create(NAME, DATE, PLACE, 100);

        Event reserved = event.reserve(10);

        assertThat(reserved).isNotSameAs(event);
        assertThat(event.available()).isEqualTo(100);
        assertThat(event.reserved()).isZero();
    }

    @Test
    void eventsWithSameId_areEqual() {
        EventId id = EventId.generate();
        Event event1 = Event.reconstitute(id, NAME, DATE, PLACE, TOTAL_CAPACITY,
                100, 50, 25, 10);
        Event event2 = Event.reconstitute(id, "Other", LocalDateTime.of(2027, 1, 1, 20, 0),
                "Other", 100, 0, 0, 0, 0);

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
