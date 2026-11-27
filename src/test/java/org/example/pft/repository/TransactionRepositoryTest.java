package org.example.pft.repository;

import org.example.pft.dto.transaction.HistoryData;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.Transaction;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TransactionRepositoryTest
    extends TransactionRepositoryContractTest{

}
