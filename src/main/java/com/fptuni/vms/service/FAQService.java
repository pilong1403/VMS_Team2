package com.fptuni.vms.service;
import com.fptuni.vms.model.FAQ;
import java.util.List;

public interface FAQService {
    List<FAQ> getAllFAQs(int page, int size);
    List<FAQ> getAllFAQsWithoutPagination();
    long getTotalFAQs();
    FAQ getFAQById(Integer id);
    FAQ createFAQ(FAQ faq);
    FAQ updateFAQ(FAQ faq);
    boolean existsById(Integer id);
    List<FAQ> filterFAQs(String status, String category,Integer num, String keyword, int page, int size);
    long countFilteredFAQs(String status, String category, String keyword);

}

