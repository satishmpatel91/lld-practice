package com.parkinglot.repo;

import com.parkinglot.Ticket;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TicketRepository {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public void save(Ticket ticket) {
        tickets.put(ticket.ticketId(), ticket);
    }

    public Optional<Ticket> findById(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    public void remove(String ticketId) {
        tickets.remove(ticketId);
    }
}
