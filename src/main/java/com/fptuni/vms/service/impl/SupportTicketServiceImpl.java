package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.SupportTicket;
import com.fptuni.vms.repository.SupportTicketRepository;
import com.fptuni.vms.service.SupportTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> findAll() {
        return supportTicketRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> findAllWithPagination(int page, int size) {
        return supportTicketRepository.findAllWithPagination(page, size);
    }

    @Override
    public SupportTicket update(SupportTicket supportTicket) {
        return supportTicketRepository.update(supportTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportTicket> findById(Integer id) {
        return supportTicketRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> filterTickets(String status, String priority, Integer num, String keyword, int page, int size) {
        return supportTicketRepository.filterTickets(status, priority, num, keyword, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFilteredTickets(String status, String priority, String keyword) {
        return supportTicketRepository.countFilteredTickets(status, priority, keyword);
    }
}
