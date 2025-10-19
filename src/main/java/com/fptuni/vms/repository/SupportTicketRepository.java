package com.fptuni.vms.repository;

import com.fptuni.vms.model.SupportTicket;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository {
    List<SupportTicket> findAll();
    List<SupportTicket> findAllWithPagination(int page, int size);
    Optional<SupportTicket> findById(Integer id);
    List<SupportTicket> filterTickets(String status, String priority, Integer num, String keyword, int page, int size);
    long countFilteredTickets(String status, String priority, String keyword);
    SupportTicket update(SupportTicket ticket);
}
