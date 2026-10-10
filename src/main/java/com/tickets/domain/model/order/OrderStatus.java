package com.tickets.domain.model.order;

/**
 * Lifecycle status of an {@link Order}.
 *
 * <pre>
 * PENDING ─(consumer reserves N)─► RESERVED ─(confirm)─► PENDING_CONFIRMATION ─(payment OK)─► SOLD
 *    │                                 │
 *    │ no availability                 │ TTL expires → scheduler
 *    ▼                                 ▼
 * REJECTED                          EXPIRED
 *
 * (admin flow) ──────────────────────────────────────────────────────► COMPLIMENTARY
 * </pre>
 */
public enum OrderStatus {

    PENDING,
    RESERVED,
    PENDING_CONFIRMATION,
    SOLD,
    REJECTED,
    EXPIRED,
    COMPLIMENTARY
}
